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
assert "appsrc/app/src/main/" in workflow
assert "rm -rf appsrc" in workflow
assert "appsrc/app/build.gradle.kts" in workflow
assert workflow.index("gradle assembleDebug --stacktrace") < workflow.index("jarsigner -verify")
assert "Snapshot verified native Android sources" not in workflow
assert "'tools/native_build/**'" in workflow
assert "'appsrc/**'" in workflow
assert "id: source_mode" in workflow
assert "legacy=false' >> \"$GITHUB_OUTPUT\"" in workflow
assert "legacy=true' >> \"$GITHUB_OUTPUT\"" in workflow
assert workflow.count("if: steps.source_mode.outputs.legacy == 'true'") >= 15
assert "cp native/v0.5.4/PromptSafety.java" in workflow
assert "cp native/v0.5.4/AppReleaseUpdater.java" in workflow
assert workflow.index("Install required shared native Java classes") < workflow.index("Build APK")
assert "Persist successfully compiled native source files on main" not in workflow
assert "unzip -q source.zip -d appsrc" in workflow
assert "base64 --decode appsrc/signing-key.b64" in workflow
print("Novaris native build migration static contracts PASS")
