# Versioned source migration from the existing native APK workflow.
from pathlib import Path
import re

source = Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/DetailActivity.java")
replacement_file = Path("native/v0.5.4/DetailActivity.java")
original = source.read_text()
replacement = replacement_file.read_text()

def method_bounds(text):
    match = re.search(r'private\s+String\s+buildShinyPrompt\s*\(', text)
    if not match:
        raise SystemExit("Shiny prompt method not found; stopping build rather than silently omitting fix")
    start = match.start()
    brace = text.find("{", match.end())
    if brace < 0:
        raise SystemExit("Shiny prompt method body not found")
    depth = 0
    for i in range(brace, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return start, i + 1
    raise SystemExit("Unclosed Shiny prompt method")

c, d = method_bounds(replacement)
method = replacement[c:d]
try:
    a, b = method_bounds(original)
    updated = original[:a] + method + original[b:]
except SystemExit:
    anchor = "private void copy(String s){"
    if anchor not in original:
        raise SystemExit("Cannot inject Shiny method: copy anchor absent")
    updated = original.replace(anchor, method + "\n    " + anchor, 1)
source.write_text(updated)
assert "BLUE, NOT YELLOW" in updated
assert "SHINY REFERENCE IMAGE" in updated
print("Verified Shiny color authority in compiled DetailActivity")
