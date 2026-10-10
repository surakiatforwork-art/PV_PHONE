"""Compile production policy with host Android shims; verify rollback and user isolation."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCES = {
    "android/content/Context.java": """package android.content;
public class Context { public java.io.File getFilesDir() {
return new java.io.File(System.getProperty("policy.root")); } }""",
    "android/system/Os.java": """package android.system;
public class Os {
 public static boolean fail;
 public static void rename(String from, String to) throws Exception {
  if (fail) throw new java.io.IOException("injected rename failure");
  java.nio.file.Files.move(java.nio.file.Paths.get(from), java.nio.file.Paths.get(to),
   java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
 }
}""",
    "com/lody/virtual/client/core/VirtualCore.java": """package com.lody.virtual.client.core;
public class VirtualCore {
 public static VirtualCore get() { return new VirtualCore(); }
 public android.content.Context getContext() { return new android.content.Context(); }
}""",
    "PolicyTest.java": """import com.lody.virtual.helper.utils.GuestPermissionPolicy;
public class PolicyTest {
 static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
 public static void main(String[] args) {
  String pkg = "test.guest", perm = "android.permission.CAMERA";
  require(GuestPermissionPolicy.setMode(pkg, 0, perm, 2), "persist deny");
  require(GuestPermissionPolicy.isDenied(pkg, 0, perm), "read deny");
  require(!GuestPermissionPolicy.isDenied(pkg, 1, perm), "user isolation");
  android.system.Os.fail = true;
  require(!GuestPermissionPolicy.setMode(pkg, 0, perm, 1), "failed save must return false");
  require(GuestPermissionPolicy.isDenied(pkg, 0, perm), "failed save must preserve deny");
  android.system.Os.fail = false;
  require(GuestPermissionPolicy.setMode(pkg, 0, perm, 1), "replace deny with allow");
  require(GuestPermissionPolicy.isExplicitlyAllowed(pkg, 0, perm), "read allow");
  require(GuestPermissionPolicy.setMode(pkg, 0, perm, 0), "reset permission");
  require(GuestPermissionPolicy.getMode(pkg, 0, perm) == 0, "read default");
  require(!GuestPermissionPolicy.setMode(pkg, 0, perm, 99), "reject invalid mode");
  System.out.println("PASS: rollback, virtual user isolation, allow/deny/default, invalid mode");
 }
}""",
}
with tempfile.TemporaryDirectory() as directory:
    temp = Path(directory)
    files = []
    for name, text in SOURCES.items():
        path = temp / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        files.append(str(path))
    files.append(str(ROOT / "runtime_overlay/GuestPermissionPolicy.java"))
    subprocess.run(["javac", "-d", str(temp / "classes"), *files], check=True)
    subprocess.run(["java", "-Dpolicy.root=" + str(temp / "data"),
                    "-cp", str(temp / "classes"), "PolicyTest"], check=True)
