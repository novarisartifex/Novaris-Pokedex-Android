#!/usr/bin/env python3
"""Extract exact original V27.42 optional variant source payload inputs."""
import html as htmlmod
import json
import re
from pathlib import Path

source = Path("Pokedex_V27_42.html").read_text(encoding="utf-8")
decoder = json.JSONDecoder()
names = ("V2713_LIFESTYLE_BLUEPRINTS","V2713_ASSIGNMENTS","V2713_CLASS_BLUEPRINTS","V2713_ASSIGNMENT_META","NOVARIS_OUTPUT_SINGLE","NOVARIS_OUTPUT_TRIPLE","NOVARIS_CORE_V117")
data = {}
for name in names:
    m = re.search(r"const\s+" + name + r"\s*=\s*",source)
    if not m: raise SystemExit("Missing original V27.42 constant: "+name)
    data[name],_ = decoder.raw_decode(source,m.end())

species = {}
row_pattern = re.compile(r'<tr\b[^>]*\bdata-dex="(\d+)"[^>]*>',re.I)
rows = list(row_pattern.finditer(source))
for idx,match in enumerate(rows):
    row = source[match.start():rows[idx+1].start() if idx+1<len(rows) else len(source)]
    name_match = re.search(r'\bdata-en-name="([^"]*)"',match.group(0))
    text_match = re.search(r'\bdata-transform-context="(.*?)"',row,re.S)
    if not name_match or not text_match:raise SystemExit("Missing original species text/name for "+match.group(1))
    t=htmlmod.unescape(text_match.group(1))
    # speciesOnlyDexV2713: preserve exact order and flags of original JS substitutions.
    t=re.sub(r'\n?\[TRANSFORM PROMPT\]\s*$', '', t, flags=re.I).strip()
    t=re.sub(r'\n?NOVARIS LIFE ROLE:[\s\S]*?(?=\nCHARACTER BLUEPRINT:)', '\n', t, count=1,flags=re.I)
    t=re.sub(r'^IDENTITY:\s*[^;\n]*? interpretation of ([^;\n]+);',r'IDENTITY: \1;',t,flags=re.I|re.M)
    for pattern in (r'^PRESENCE / BODY LANGUAGE:.*$',r'^PROFESSION MATERIAL LAYER:.*$',r'^ROLE OUTFIT INTEGRATION:.*$'):
        t=re.sub(pattern,'',t,flags=re.I|re.M)
    cut=re.search(r'\nWORK OUTFIT BLUEPRINT:',t,re.I)
    if cut:t=t[:cut.start()]
    t=re.sub(r'\n{3,}','\n\n',t).strip()
    dex=match.group(1)
    species[dex]={"name":htmlmod.unescape(name_match.group(1)).strip(),"text":t}
data["SPECIES"]=species
assert len(species)==1025,len(species)
assert len(data["V2713_ASSIGNMENTS"])>=1025
assert len(data["V2713_LIFESTYLE_BLUEPRINTS"])==3
assets=Path("appsrc/app/src/main/assets")
assets.mkdir(parents=True,exist_ok=True)
dest=assets/"novaris_original_variants.json"
dest.write_text(json.dumps(data,ensure_ascii=False,separators=(',',':')),encoding="utf-8")
print("Extracted original variants",len(species),"species; asset bytes",dest.stat().st_size)
