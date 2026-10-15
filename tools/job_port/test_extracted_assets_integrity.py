#!/usr/bin/env python3
"""Validate extracted V27.42 assets without inventing HTML job rules.

Usage:
  python tools/job_port/test_extracted_assets_integrity.py PATH_TO_HTML ASSETS_DIR

Requires the canonical extractor's dependencies (lxml).
"""
import json
import sys
import tempfile
from pathlib import Path

from extract_html_job_contract import extract


def check(html_path, assets_dir):
    catalog, species = extract(html_path)
    assets = Path(assets_dir)
    files = {
        "novaris_job_catalog_v2742.json": catalog,
        "novaris_job_baseline_v2742.json": species,
    }
    for filename, expected in files.items():
        actual = json.loads((assets / filename).read_text(encoding="utf-8"))
        assert actual == expected, f"{filename}: stale or altered relative to original HTML"
        encoded = json.dumps(actual, ensure_ascii=False, separators=(",", ":"))
        assert json.loads(encoded) == actual, f"{filename}: JSON round-trip failed"

    ids = [item["dex"] for item in species]
    assert ids == sorted(ids), "Baseline order differs from ascending Pokédex order"
    assert len(ids) == len(set(ids)) == 1025, "Missing or duplicated standard species"
    assert all(1 <= number <= 1025 for number in ids), "Unexpected Pokédex identifier"

    # The override set is authoritative; it must reference known species.
    overrides = catalog["NOVARIS_ASSIGNMENT_OVERRIDES_V2645"]
    assert all(int(key) in ids for key in overrides), "Override refers to unknown species"

    # Verify the extractor itself is deterministic across repeated reads.
    second_catalog, second_species = extract(html_path)
    assert (catalog, species) == (second_catalog, second_species), "Nondeterministic extraction"
    print("Canonical V27.42 asset integrity and JSON round-trip PASS")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: test_extracted_assets_integrity.py HTML_PATH ASSETS_DIR")
    check(sys.argv[1], sys.argv[2])
