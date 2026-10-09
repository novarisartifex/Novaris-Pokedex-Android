#!/usr/bin/env python3
"""Update V27.42 source content to Novaris V27.43 without changing app code."""
import argparse
from pathlib import Path
import re

COMPLEXION = "lightly sun-kissed warm beige complexion, consistent across the entire evolution family."
OLD_RULE = "preserve the intended complexion identity; do not force tanning or change ethnicity, but prevent exposure / desaturation from washing the skin toward paper-white"
NEW_RULE = "preserve the prescribed lightly sun-kissed warm beige complexion; keep the exact same skin tone in every stage and alternate form of the same evolution family; prevent exposure / desaturation from washing the skin toward paper-white"
OLD_SUMMARY = "This rule is a color-depth correction only. It must not overwrite source palette identity or the Pokédex-prescribed complexion."
NEW_SUMMARY = "This rule must preserve source palette identity while enforcing a lightly sun-kissed warm beige human complexion. Every member and form of the same evolution family has an identical complexion; evolution never changes skin color."

def transform(source: str) -> str:
    if source.count(OLD_RULE) != 1 or source.count(OLD_SUMMARY) != 1:
        raise ValueError("Unexpected source Core; refusing to change incomplete or unknown data")
    source = source.replace(OLD_RULE, NEW_RULE).replace(OLD_SUMMARY, NEW_SUMMARY)
    source, count = re.subn(r"(SKIN / COMPLEXION: )[^\\n<\\\"]+", lambda m: m.group(1) + COMPLEXION, source)
    if count != 5131:
        raise ValueError(f"Expected 5131 complexion descriptions, found {count}")
    if source.count('data-dex="') != 1025:
        raise ValueError("Expected 1025 Pokémon records")
    return source

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("source")
    parser.add_argument("destination")
    args = parser.parse_args()
    updated = transform(Path(args.source).read_text(encoding="utf-8"))
    Path(args.destination).write_text(updated, encoding="utf-8")
    print("Novaris V27.43: 1025 Pokémon; 5131 complexion descriptions updated")

if __name__ == "__main__":
    main()
