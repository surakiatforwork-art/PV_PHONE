# Read-only Android diagnostics integration

Settings → Engine Diagnostics observes the host build version and source commit,
Android SDK/release, device model/ABI, process bitness, actual Linux UID, page size,
host all-files grant and configured camera package. Refresh repeats these local
observations; it does not launch guests, reinject hooks or change preferences.

All 23 guest capability entries remain UNKNOWN / NOT_TESTED. Details explicitly
state that guest hook presence, guest identity/version/user scope, last runtime
trial and evidence artifacts have not been collected. This is a host facts UI,
not an Android port of the Python evidence reducer or a compatibility matrix.
Previously documented v0.4.8 trials are not silently attributed to this build.

The activity is private (`exported=false`). It requests no permissions, exports
no files, enumerates no user documents and contacts no external service. Existing
VirtualApp patches and guest enforcement are retained. The signed CI artifact is
labelled **0.4.9-dev**, code 68; the published stable release remains v0.4.8.
No GitHub release is created. An update install preserves existing host data.

Boundary checks verify catalog parity, private manifest/build wiring and the
absence of runtime/policy mutation paths. Physical UI checks validate navigation,
refresh and host values; they cannot establish guest subsystem compatibility.

Next integration: a separately scoped diagnostic guest fixture and evidence
import with exact build/device/package/version/user identity. Only then can the
existing evidence contract determine ACTIVE, DEGRADED or FAILED.
