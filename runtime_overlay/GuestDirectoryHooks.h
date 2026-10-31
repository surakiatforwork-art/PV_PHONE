#pragma once
#include <dirent.h>
#include <errno.h>

// Directory listing must use the same virtual path rules as file opening.
static DIR* (*phantom_original_opendir)(const char*);
static DIR* phantom_opendir(const char* pathname) {
    int policy;
    const char* mapped = relocate_path(pathname, &policy);
    if (policy == FORBID) {
        FREE(mapped, pathname); errno = EACCES; return nullptr;
    }
    DIR* result = phantom_original_opendir(mapped);
    int saved = errno;
    FREE(mapped, pathname); errno = saved;
    return result;
}
static void hook_guest_directory_symbols(void* libc) {
    hook_function(libc, "opendir", (void*)phantom_opendir, (void**)&phantom_original_opendir);
    ALOGI("PHANToM IO opendir redirect installed=%d", phantom_original_opendir != nullptr);
}
