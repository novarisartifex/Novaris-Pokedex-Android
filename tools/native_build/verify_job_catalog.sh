#!/usr/bin/env bash
set -euo pipefail
python3 -m pip install lxml
python3 - <<'PY'
import gzip
from pathlib import Path
source=Path("Pokedex_V27_42.html.gz")
target=Path("/tmp/novaris_jobs_v2742.html")
with gzip.open(source,"rb") as f:
    target.write_bytes(f.read())
print("Original HTML:",target.stat().st_size,"bytes")
PY
python3 tools/job_port/test_html_job_contract.py /tmp/novaris_jobs_v2742.html
python3 tools/job_port/extract_html_job_contract.py /tmp/novaris_jobs_v2742.html appsrc/app/src/main/assets
test -s appsrc/app/src/main/assets/novaris_job_catalog_v2742.json
test -s appsrc/app/src/main/assets/novaris_job_baseline_v2742.json
python3 tools/job_port/test_extracted_assets_integrity.py /tmp/novaris_jobs_v2742.html appsrc/app/src/main/assets
python3 - <<'PY'
import json
from pathlib import Path
c=json.loads(Path("appsrc/app/src/main/assets/novaris_job_catalog_v2742.json").read_text())
for key in ("V2610_RAG","V2610_CIV","V2610_GATHER","V2610_CRAFT","V2622_FFXIV"):
    print("CATALOG STRUCTURE",key)
    for family,entry in list(c[key].items())[:3]:
        print("FAMILY",family,"TYPE",type(entry).__name__,"VALUE",json.dumps(entry,ensure_ascii=False)[:1500])
PY
python3 - <<'PY'
import json
from pathlib import Path
c=json.loads(Path("appsrc/app/src/main/assets/novaris_job_catalog_v2742.json").read_text())
for family, entry in c["V2610_RAG"].items():
    assert isinstance(entry.get("tier_map"),dict),family
    assert any(entry["tier_map"].values()),family
    assert any(entry["tier_map"].get(t) for t in ("T2","Transcendent","T3","T4")),family
for family,entry in c["V2610_CIV"].items():
    assert isinstance(entry.get("specializations"),list) and entry["specializations"],family
print("Native job picker catalog contracts PASS")
PY
