# Versioned source migration from the existing native APK workflow.
from pathlib import Path
s=Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/DetailActivity.java").read_text()
start=s.index("private void addJobEditor(")
end=s.index("private String applyEditedJob(",start)
picker=s[start:end]
assert 'new EditText(this)' not in picker, "Free-text editor is still present"
assert 'optJSONObject("tier_map")' in picker, "Ragnarok stage mapping missing"
assert 'optJSONArray("specializations")' in picker, "Civilian stage mapping missing"
assert 'system==2?"C"+(index+1):""' in picker, "Civilian stage tier not persisted"
assert 'boolean structuredSaved=' in picker, "Structured job persistence must be checked"
assert 'if(system==2 && entry!=null)' in picker, "Civilian specializations must use system index 2"
assert 'if(system==1 && entry!=null)' not in picker, "FFXIV index must not be used for Civilian stages"
for key in ("V2610_RAG","V2610_CIV","V2622_FFXIV","V2610_GATHER","V2610_CRAFT"):
    assert key in picker, f"Missing catalog: {key}"
print("Native catalog picker source contract PASS")
