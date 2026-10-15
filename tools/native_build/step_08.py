# Versioned source migration from the existing native APK workflow.
from pathlib import Path
p=Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/DetailActivity.java")
s=p.read_text()
anchor='private void copy(String s){'
if anchor not in s:raise SystemExit("Copy method not found")
# Every x3 listener uses the dedicated triple-aware copy path.
import re
matches=0
for name in ('x3','b3'):
    pattern=r'(?m)(\b'+name+r'\.setOnClickListener\(v->)copy\('
    s,n=re.subn(pattern,r'\1copyTriple(',s)
    matches+=n
if matches<2:raise SystemExit("x3 copy wiring missing")
s=s.replace(anchor,'private void copyTriple(String s){ copy(PromptSafety.enforce(s,true)); }\n    '+anchor,1)
# The ordinary copy path must also enforce the species-to-fashion guard.
# Avoid double-appending to prompts that already include the safety header.
start=s.index(anchor)
end=s.find('\n    private ',start+len(anchor))
if end<0:end=len(s)
block=s[start:end]
if 'PromptSafety.enforce' not in block:
    block=block.replace(anchor,anchor+'\n        if(s!=null && !s.startsWith("NOVARIS OUTPUT CONTROL") && (s.contains("SPECIES IDENTITY:") || s.contains("NOVARIS OPTIONAL CLASS") || s.contains("NOVARIS LIFESTYLE VARIANT"))) s=PromptSafety.enforce(s,false);',1)
    s=s[:start]+block+s[end:]
p.write_text(s)
print("Triple copy listeners:",matches)
assert 'copyTriple(' in s
