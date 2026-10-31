// Read-only property overrides, confined to an explicitly opted-in guest process.
// Included after the existing guarded hook_function implementation.
#include <sys/system_properties.h>
#include <cstring>
#include <cstdint>

namespace GuestDeviceProperties {
static const char* value(const char* key) {
    if (!key) return nullptr;
    static const char* pairs[][2] = {
        {"ro.product.model", "V2408A"}, {"ro.product.brand", "iQOO"},
        {"ro.product.name", "PD2408"}, {"ro.product.system.name", "PD2408"},
        {"ro.product.odm.model", "V2408A"}, {"ro.product.system.model", "V2408A"},
        {"ro.product.system.brand", "iQOO"}, {"ro.product.system.device", "PD2408"},
        {"ro.product.vendor.model", "V2408A"}, {"ro.product.manufacturer", "iQOO"},
        {"ro.product.system.manufacturer", "iQOO"},
        {"ro.product.vendor.marketname", "iQOO 13"},
        {"ro.product.system_ext.model", "V2408A"}, {"ro.build.product", "PD2408"}
    };
    for (const auto& pair : pairs) if (!strcmp(key, pair[0])) return pair[1];
    return nullptr;
}
static int (*originalGet)(const char*, char*);
static int (*originalRead)(const prop_info*, char*, char*);
using Callback = void (*)(void*, const char*, const char*, uint32_t);
static void (*originalCallback)(const prop_info*, Callback, void*);

static int get(const char* key, char* out) {
    const char* replacement = value(key);
    if (replacement) { strcpy(out, replacement); return strlen(replacement); }
    return originalGet(key, out);
}
static int read(const prop_info* info, char* name, char* out) {
    int result = originalRead(info, name, out);
    const char* replacement = name ? value(name) : nullptr;
    if (!name && originalCallback) {
        originalCallback(info, [](void* cookie, const char* key, const char*, uint32_t) {
            const char* mapped = value(key);
            if (mapped) strcpy(static_cast<char*>(cookie), mapped);
        }, out);
        return strlen(out);
    }
    if (replacement) { strcpy(out, replacement); return strlen(replacement); }
    return result;
}
struct Forward { Callback callback; void* cookie; };
static void forward(void* opaque, const char* name, const char* actual, uint32_t serial) {
    Forward* state = static_cast<Forward*>(opaque);
    const char* replacement = value(name);
    state->callback(state->cookie, name, replacement ? replacement : actual, serial);
}
static void readCallback(const prop_info* info, Callback callback, void* cookie) {
    Forward state{callback, cookie};
    originalCallback(info, forward, &state);
}

// Install only on explicit opt-in; ordinary guests/host never hook property APIs.
// Bits report actual trampoline availability, so callers cannot claim full coverage
// if the existing short-function safety guard refuses a device's libc entry point.
static int install() {
    static int status = -1;
    if (status >= 0) return status;
    status = 0;
    void* libc = dlopen("libc.so", RTLD_NOW);
    if (!libc) return status;
    hook_function(libc, "__system_property_get", (void*)get, (void**)&originalGet);
    hook_function(libc, "__system_property_read", (void*)read, (void**)&originalRead);
    hook_function(libc, "__system_property_read_callback", (void*)readCallback, (void**)&originalCallback);
    if (originalGet) status |= 1;
    if (originalRead) status |= 2;
    if (originalCallback) status |= 4;
    dlclose(libc);
    return status;
}
}
