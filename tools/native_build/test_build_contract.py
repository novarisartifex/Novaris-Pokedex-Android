#!/usr/bin/env python3
"""Static contracts for the transitional Novaris native build workflow."""
from pathlib import Path

workflow = Path(".github/workflows/build-native-v054.yml").read_text(encoding="utf-8")
scripts = [
    *(f"tools/native_build/step_{i:02d}.py" for i in range(1, 11)),
    "tools/native_build/wire_original_variants.py",
    "tools/native_build/wire_form_variants.py",
    "tools/native_build/verify_job_catalog.sh",
]
for script in scripts:
    assert Path(script).is_file(), f"Missing build script: {script}"
    assert script in workflow, f"Unreferenced build script: {script}"
assert "python3 - <<'PY'" not in workflow
assert "cat native/v0.4.0/part*.b64" in workflow
assert "gradle assembleDebug --stacktrace" in workflow
assert "jarsigner -verify" in workflow
assert "appsrc/app/src/main/" in workflow, "Snapshot must preserve all assets and resources"
assert "for path in appsrc/app/src/main " in workflow, "Snapshot must stage all main source files"
assert workflow.index("gradle assembleDebug --stacktrace") < workflow.index("jarsigner -verify")
assert workflow.index("jarsigner -verify") < workflow.index("Snapshot verified native Android sources")
assert "'tools/native_build/**'" in workflow
assert "'appsrc/**'" in workflow
print("Novaris native build migration static contracts PASS")
