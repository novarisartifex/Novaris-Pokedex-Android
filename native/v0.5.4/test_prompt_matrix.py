#!/usr/bin/env python3
"""Regression guard for NOVARIS 4-species / 4-class / 2-form / 2-count matrix.

This validates stored lifestyle concepts and the Java prompt-assembly wiring.
It does NOT execute Android Java or claim to validate 64 rendered prompt texts.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SPECIES = (190, 324, 332, 338)
CLASSES = ("Main", "Casual", "Nightlife", "Lingerie")
FORMS = ("Normal", "Shiny")
COUNTS = (1, 3)
data = {}
for name in ("0151_0200", "0301_0350"):
    path = ROOT / f"novaris_lifestyle_block_{name}_v2.json"
    data.update(json.loads(path.read_text(encoding="utf-8"))["species"])

engine = (ROOT / "OriginalVariantEngine.java").read_text(encoding="utf-8")
installer = (ROOT / "install_shiny_lifestyle.py").read_text(encoding="utf-8")
safety = (ROOT / "PromptSafety.java").read_text(encoding="utf-8")
assert 'SHINY LIFESTYLE PALETTE OVERRIDE' in engine
assert 'NOVARIS SHINY COLOR AUTHORITY:' in installer
assert 'shinyLifestylePrompt(dex,shinyName,variant,false,root,0)' in installer
assert 'prompt.contains("NOVARIS OUTPUT CONTROL")' in safety
assert 'String core=root.getString("NOVARIS_CORE_V117")' in engine
assert 'return build(context, root, dex, label, triple, 0);' in engine
# x3 reuses one stored concept for all three independent images; x1 uses the chosen concept.
assert engine.count('dex<=50?0:(triple?0:conceptIndex)') == 2
assert engine.count('int limit=1;') == 2
assert 'conceptIndex<0||conceptIndex>2' in engine
assert 'OriginalVariantEngine.build(this,shinyRoot,dex,variant,triple,conceptIndex)' in installer
assert 'for(int concept=0;concept<3;concept++)' not in installer
workflow = (ROOT.parent.parent / '.github/workflows/build-native-v054.yml').read_text(encoding='utf-8')
assert 'originalVariantPrompt(dex,variant,false,root,0)' in workflow
assert 'single.setOnClickListener(v->copy(originalVariantPrompt(dex,variant,false,root,0)))' in workflow


checked = 0
for dex in SPECIES:
    entry = data[str(dex)]
    for role in CLASSES:
        for form in FORMS:
            for count in COUNTS:
                checked += 1
                if role == "Main":
                    # Main-class prompt is assembled by DetailActivity, not by
                    # the lifestyle engine. Count these cases as UNEXECUTED.
                    continue
                variant = entry["variants"][role]
                for field in ("outfit", "materials", "identity_translation"):
                    assert variant[field].strip(), (dex, role, field)
                concepts = variant["concepts"]
                assert len(concepts) >= count, (dex, role, count)
                for concept in concepts[:count]:
                    for field in ("outfit", "pose", "scene", "lighting"):
                        assert concept[field].strip(), (dex, role, field)
                    scene = concept["scene"].lower()
                    if role == "Casual":
                        assert "moonlit" not in scene and "nighttime" not in scene, (dex, scene)
                    if role == "Nightlife":
                        assert any(word in scene for word in
                                   ("gala", "ball", "masquerade", "reception", "banquet")), (dex, scene)
print(f"Matrix enumerated: {checked}; lifestyle data cases checked: 48; "
      "main-class cases NOT executed: 16.")
print("Java prompt assembly and full Core occurrence counts require an Android runtime test.")
