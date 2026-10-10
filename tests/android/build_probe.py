"""Build a disposable camera/permission guest; outputs stay outside the repository."""
from pathlib import Path
import os
import subprocess
import sys
import tempfile
import re

sdk = Path(os.environ["ANDROID_HOME"])
tools = sdk / "build-tools" / "36.1.0"
jar = sdk / "platforms" / "android-34" / "android.jar"
root = Path(__file__).resolve().parent
out = Path(sys.argv[1]).resolve()
out.mkdir(parents=True, exist_ok=True)
package = sys.argv[2] if len(sys.argv) > 2 else "com.phantom.releaseprobe"
if not re.fullmatch(r"com\.phantom\.[a-z][a-z0-9]*", package):
    raise ValueError("Probe package must be in com.phantom")

def run(*args):
    subprocess.run([str(arg) for arg in args], check=True)

with tempfile.TemporaryDirectory() as directory:
    temp = Path(directory)
    classes = temp / "classes"
    dex = temp / "dex"
    classes.mkdir()
    dex.mkdir()
    sources = temp / "sources"
    sources.mkdir()
    for name in ("MainActivity.java", "CaptureProvider.java", "TransitionActivity.java", "AndroidManifest.xml"):
        (sources / name).write_text((root / name).read_text().replace("com.phantom.releaseprobe", package))
    run("javac", "-source", "8", "-target", "8", "-classpath", jar,
        "-d", classes, *sources.glob("*.java"))
    run("java", "-cp", tools / "lib/d8.jar", "com.android.tools.r8.D8",
        "--lib", jar, "--min-api", "21", "--output", dex,
        *classes.rglob("*.class"))
    exe = ".exe" if os.name == "nt" else ""
    run(tools / ("aapt2" + exe), "link", "-o", temp / "base.apk",
        "--manifest", sources / "AndroidManifest.xml", "-I", jar,
        "--min-sdk-version", "21", "--target-sdk-version", "33")
    run("jar", "uf", temp / "base.apk", "-C", dex, "classes.dex")
    run(tools / ("zipalign" + exe), "-f", "4", temp / "base.apk", temp / "aligned.apk")
    run("java", "-jar", tools / "lib/apksigner.jar", "sign", "--ks",
        Path.home() / ".android/debug.keystore", "--ks-key-alias", "androiddebugkey",
        "--ks-pass", "pass:android", "--key-pass", "pass:android",
        "--out", out / "Probe.apk", temp / "aligned.apk")
print(out / "Probe.apk")
