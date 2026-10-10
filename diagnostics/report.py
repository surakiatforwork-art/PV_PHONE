"""Validate and render an offline report; no ADB, network or runtime mutation."""
import argparse
from dataclasses import asdict
from datetime import datetime, timezone
from html import escape
import json
from pathlib import Path
import os
import tempfile

from .model import (SCHEMA_VERSION, SUBSYSTEMS, Evidence, Result, Scope, capability,
                    validate_matrix_row)


def evaluate(document, *, now=None):
    if type(document.get("schema_version")) is not int or document.get("schema_version") != SCHEMA_VERSION:
        raise ValueError("Unsupported diagnostic report schema")
    scope = Scope(**document["scope"])
    registry = {}
    for item in document.get("evidence", []):
        trial = Evidence(**{**item, "scope": Scope(**item["scope"]),
                            "result": Result(item["result"]), "artifacts": tuple(item["artifacts"])})
        if trial.evidence_id in registry:
            raise ValueError("Duplicate evidence ID")
        registry[trial.evidence_id] = trial
    requirements = document["requirements"]
    if set(requirements) != set(SUBSYSTEMS):
        raise ValueError("Report must declare requirements for the complete subsystem catalog")
    caps = [capability(key, requirements[key], registry.values(), scope, now=now)
            for key in SUBSYSTEMS]
    matrix = []
    for item in document.get("matrix", []):
        row = {**item, "scope": Scope(**item["scope"])}
        if not row.get("feature") or not row.get("requirement_id"):
            raise ValueError("Incomplete matrix row")
        validate_matrix_row(row, registry, now=now)
        matrix.append(row)
    return caps, registry, matrix


def render(document, *, now=None):
    now = now or datetime.now(timezone.utc)
    caps, registry, matrix = evaluate(document, now=now)
    def text(value):
        return escape(str(value), quote=True)
    parts = ["<!doctype html><html lang='en'><meta charset='utf-8'>",
             "<meta name='viewport' content='width=device-width,initial-scale=1'>",
             "<title>PHANToM Engine Diagnostics</title>",
             "<style>body{font:16px system-ui;background:#111827;color:#e5e7eb;max-width:1000px;margin:32px auto;padding:16px}details{background:#1f2937;margin:8px 0;padding:14px;border-radius:8px}summary{cursor:pointer}pre{white-space:pre-wrap;overflow-wrap:anywhere}table{width:100%;border-collapse:collapse}td,th{padding:10px;text-align:left;border-bottom:1px solid #475569}.ACTIVE{color:#86efac}.FAILED{color:#fca5a5}.DEGRADED{color:#fde68a}small{color:#cbd5e1}</style>",
             "<h1>PHANToM Engine Diagnostics</h1>",
             "<p>Offline report. No Android hooks or settings are changed. Build success does not prove runtime success.</p>",
             "<p>Evaluated at " + text(now.isoformat()) + " · evidence expires after 30 days.</p>",
             "<pre>" + text(json.dumps(document["scope"], indent=2)) + "</pre>"]
    for cap in caps:
        parts.append("<details><summary>" + text(cap.subsystem) + " — <strong class='" +
                     cap.state.value + "'>" + cap.state.value + "</strong></summary>")
        parts.append("<p>" + text(cap.reason) + "</p><p>Last tested: " + text(cap.last_tested_at or "NOT_TESTED") +
                     "<br>Fallback: " + text(cap.fallback) + "</p>")
        for key in cap.evidence_ids:
            trial = registry[key]
            parts.append("<pre>" + text(json.dumps(asdict(trial), indent=2)) + "</pre>")
        parts.append("</details>")
    parts.append("<h2>Guest compatibility</h2><table><tr><th>Guest / version / user</th><th>Feature</th><th>Result</th></tr>")
    for row in matrix:
        s = row["scope"]
        parts.append("<tr><td>" + text(f"{s.package_name} / {s.app_version_code} / {s.virtual_user_id}") +
                     "</td><td>" + text(row["feature"]) + "</td><td>" + text(row["result"]) + "</td></tr>")
    parts.append("</table>")
    if not matrix:
        parts.append("<p>No scoped compatibility rows collected. This is not a PASS.</p>")
    parts.append("<h2>Historical context</h2><p>Historical documentation is not current runtime evidence.</p>")
    for note in document.get("historical_notes", []):
        parts.append("<p>" + text(note) + "</p>")
    parts.append("</html>")
    return "\n".join(parts)


def atomic_write(path, content):
    """Replace only the requested offline artifact; preserve it on write failure."""
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    handle, temporary = tempfile.mkstemp(prefix=path.name + ".", dir=path.parent)
    try:
        with os.fdopen(handle, "w", encoding="utf-8") as output:
            output.write(content)
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    parser.add_argument("--html", type=Path)
    args = parser.parse_args()
    if args.html and args.input.resolve() == args.html.resolve():
        parser.error("HTML output must not overwrite the evidence input")
    document = json.loads(args.input.read_text(encoding="utf-8"))
    caps, registry, matrix = evaluate(document)
    if args.html:
        atomic_write(args.html, render(document))
    print(json.dumps({"validated": True, "capabilities": len(caps),
                      "evidence": len(registry), "matrix_rows": len(matrix),
                      "states": {c.subsystem: c.state.value for c in caps}}, indent=2))


if __name__ == "__main__":
    main()
