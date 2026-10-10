"""Integration boundaries for the host-only Android diagnostics surface."""
from pathlib import Path
import re
import unittest

from diagnostics.model import SUBSYSTEMS

ROOT = Path(__file__).resolve().parents[1]


class AndroidDiagnosticsBoundary(unittest.TestCase):
    def test_catalog_matches_contract(self):
        source = (ROOT / "runtime_overlay/EngineDiagnosticsActivity.java").read_text(encoding="utf-8")
        catalog = source.split("SUBSYSTEMS = {", 1)[1].split("};", 1)[0]
        self.assertEqual(tuple(re.findall(r'"([a-z0-9_]+)"', catalog)), SUBSYSTEMS)
        # This surface has no evidence importer: it must never claim ACTIVE.
        self.assertNotIn("ACTIVE", source)
        for mutation in ("VirtualCore", "VActivityManager", "checkEnv(", "requestPermissions(",
                         ".edit()", "startActivity(", "FileOutputStream", "Runtime.getRuntime"):
            self.assertNotIn(mutation, source)

    def test_private_activity_and_build_wiring(self):
        patch = (ROOT / "runtime_patch/engine_diagnostics.patch").read_text(encoding="utf-8")
        self.assertIn('android:exported="false"', patch)
        workflow = (ROOT / ".github/workflows/phantom-vphone-rc.yml").read_text(encoding="utf-8")
        self.assertIn("../runtime_patch/engine_diagnostics.patch", workflow)
        self.assertIn("cp runtime_overlay/EngineDiagnosticsActivity.java", workflow)
        self.assertIn("DIAGNOSTICS_SOURCE", workflow)


if __name__ == "__main__":
    unittest.main()
