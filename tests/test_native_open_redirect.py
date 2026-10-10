"""Exercise the production open wrappers on Linux without installing inline hooks."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class NativeOpenContract(unittest.TestCase):
    @unittest.skipUnless(shutil.which("g++"), "Linux g++ is required (CI runs this test)")
    def test_flags_modes_relative_paths_and_failures(self):
        patch = (ROOT / "runtime_patch/native_open_redirect.patch").read_text(encoding="utf-8")
        additions = "\n".join(line[1:] for line in patch.splitlines()
                              if line.startswith("+") and not line.startswith("+++"))
        wrappers = additions.split("// ParcelFileDescriptor", 1)[1]
        wrappers = "// ParcelFileDescriptor" + wrappers.split("static void hook_open_symbols", 1)[0]
        harness = r'''
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <errno.h>
#include <cstdlib>
#include <cstring>
#include <string>
#include <cassert>
#define FORBID 1
#define FREE(ptr, original) if (ptr && ptr != original) free((void*)ptr)
#define HOOK_DEF(ret, name, ...) ret (*orig_##name)(__VA_ARGS__); ret new_##name(__VA_ARGS__)
static std::string directory;
static const char* relocate_path(const char* path, int* result) {
    *result = 0;
    if (strncmp(path, "/forbidden/", 11) == 0) { *result = FORBID; return nullptr; }
    if (strncmp(path, "/guest/", 7) == 0) return strdup((directory + "/" + (path + 7)).c_str());
    return path;
}
'''
        cases = r'''
int main(int argc, char** argv) {
    assert(argc == 2); directory = argv[1]; umask(0);
    orig_open = ::open; orig_open64 = ::open64;
    orig_openat = ::openat; orig_openat64 = ::openat64;
    int fd = new_open("/guest/photo.jpg", O_CREAT | O_RDWR | O_CLOEXEC, 0600);
    assert(fd >= 0 && (fcntl(fd, F_GETFD) & FD_CLOEXEC));
    assert(write(fd, "abc", 3) == 3); close(fd);
    struct stat st; assert(stat((directory + "/photo.jpg").c_str(), &st) == 0);
    assert((st.st_mode & 0777) == 0600 && st.st_size == 3);
    fd = new_open64("/guest/photo.jpg", O_RDONLY);
    char bytes[5] = {}; assert(fd >= 0 && read(fd, bytes, 3) == 3);
    assert(strcmp(bytes, "abc") == 0); close(fd);
    int dirfd = ::open(directory.c_str(), O_RDONLY | O_DIRECTORY);
    assert(dirfd >= 0);
    fd = new_openat(dirfd, "photo.jpg", O_WRONLY | O_APPEND);
    assert(fd >= 0 && write(fd, "d", 1) == 1); close(fd);
    fd = new_openat64(-1, "/guest/photo.jpg", O_RDONLY);
    assert(fd >= 0 && read(fd, bytes, 4) == 4); close(fd);
    assert(memcmp(bytes, "abcd", 4) == 0);
    fd = new_openat(dirfd, "second.jpg", O_CREAT | O_WRONLY, 0640);
    assert(fd >= 0); close(fd); close(dirfd);
    assert(stat((directory + "/second.jpg").c_str(), &st) == 0);
    assert((st.st_mode & 0777) == 0640);
    fd = new_open("/guest/photo.jpg", O_WRONLY | O_TRUNC);
    assert(fd >= 0); close(fd);
    assert(stat((directory + "/photo.jpg").c_str(), &st) == 0 && st.st_size == 0);
    errno = 0; assert(new_open("/guest/missing.jpg", O_RDONLY) == -1 && errno == ENOENT);
    errno = 0; assert(new_openat(-1, "/forbidden/photo.jpg", O_CREAT | O_WRONLY, 0600) == -1 && errno == EACCES);
    return 0;
}
'''
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "open-contract.cpp"
            source.write_text(harness + wrappers + cases, encoding="utf-8")
            binary = root / "open-contract"
            subprocess.run(["g++", "-std=c++11", "-Wall", "-Wextra", "-Werror",
                            str(source), "-o", str(binary)], check=True)
            subprocess.run([str(binary), str(root)], check=True)


if __name__ == "__main__":
    unittest.main()
