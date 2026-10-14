#!/usr/bin/env python3
"""Extract the effective V27.42 HTML job catalog and baseline for the native port.

The HTML contains an obsolete V26.28 editor before the real V26.38/V26.48
overrides. Extract V2610_* and the v16 Civilian-stage override, not the
first buildNovarisJobCatalog() definition.
"""
import argparse
import json
import re
from pathlib import Path
from lxml import etree

KEYS = ("V2610_RAG", "V2610_CIV", "V2610_GATHER", "V2610_CRAFT",
        "V2622_FFXIV", "V2615_JOB_DE", "NOVARIS_ASSIGNMENT_OVERRIDES_V2645")

def js_constant(html, name):
    m = re.search(r"\b(?:const|let|var)\s+" + re.escape(name) + r"\s*=\s*", html)
    if not m:
        raise ValueError("Missing canonical HTML constant: " + name)
    i = m.end()
    if html.startswith("Object.freeze(", i):
        i += len("Object.freeze(")
    while html[i].isspace():
        i += 1
    start, depth, quote, escape = i, 0, None, False
    if html[i] not in "{[":
        raise ValueError("Unexpected constant type: " + name)
    for j in range(i, len(html)):
        ch = html[j]
        if quote:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == quote:
                quote = None
        elif ch in ('"', "'", "`"):
            quote = ch
        elif ch in "{[":
            depth += 1
        elif ch in "}]":
            depth -= 1
            if depth == 0:
                return json.loads(html[start:j+1])
    raise ValueError("Unterminated constant: " + name)

def extract(html_path):
    html = Path(html_path).read_text(encoding="utf-8")
    catalog = {key: js_constant(html, key) for key in KEYS}
    baby = re.search(r"\bconst\s+V2610_BABY\s*=\s*new Set\((\[[^\]]+\])\)", html)
    if not baby:
        raise ValueError("Missing V2610_BABY evolution rules")
    catalog["V2610_BABY"] = json.loads(baby.group(1))
    catalog["V2610_BRANCH_HINT"] = js_constant(html, "V2610_BRANCH_HINT")
    catalog["schema"] = "NOVARIS_JOB_ASSIGNMENTS_v16"
    catalog["pokedex_version"] = "V27.0"
    species = []
    for _, row in etree.iterparse(str(html_path), html=True, events=("end",), tag="tr"):
        dex = row.get("data-dex")
        if dex:
            art = row.xpath('.//div[contains(concat(" ",normalize-space(@class)," ")," art-wrap ")]')
            if len(art) != 1:
                raise ValueError("Missing or duplicate art-wrap for dex " + dex)
            a = art[0]
            item = dict(
                dex=int(dex), en=row.get("data-en-name", ""),
                de=row.get("data-de-name", ""),
                generation=int(row.get("data-generation", "0")),
                system=a.get("data-career-system", ""),
                role=a.get("data-career-role", ""),
                family=a.get("data-job-family", ""),
                tier=a.get("data-career-tier", ""),
                specialization=a.get("data-civil-specialization", ""),
                evolution=a.get("data-evolution-context", ""),
                legendary=a.get("data-legendary-special", "") == "1")
            override = catalog["NOVARIS_ASSIGNMENT_OVERRIDES_V2645"].get(dex)
            if override:
                item["original_override"] = override
            species.append(item)
        row.clear()
        while row.getprevious() is not None:
            del row.getparent()[0]
    if len(species) != 1025 or len({p["dex"] for p in species}) != 1025:
        raise ValueError("Expected 1025 distinct standard forms")
    if len(catalog["V2610_RAG"]) != 19 or len(catalog["V2610_CIV"]) != 60:
        raise ValueError("Canonical job catalog changed; inspect HTML before porting")
    if sum(len(v["specializations"]) for v in catalog["V2610_CIV"].values()) != 241:
        raise ValueError("Civilian C1..Cn stages changed; inspect HTML")
    return catalog, species

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("html")
    ap.add_argument("assets_dir")
    args = ap.parse_args()
    catalog, species = extract(args.html)
    out = Path(args.assets_dir)
    out.mkdir(parents=True, exist_ok=True)
    for name, obj in (("novaris_job_catalog_v2742.json", catalog),
                      ("novaris_job_baseline_v2742.json", species)):
        (out / name).write_text(json.dumps(obj, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print("Extracted:", len(species), "Pokémon,", len(catalog["V2610_RAG"]),
          "Ragnarok families,", len(catalog["V2610_CIV"]), "Civilian families,",
          sum(len(v["specializations"]) for v in catalog["V2610_CIV"].values()),
          "Civilian stages,", len(catalog["NOVARIS_ASSIGNMENT_OVERRIDES_V2645"]),
          "approved assignment overrides")
