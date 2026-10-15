# Versioned source migration from the existing native APK workflow.
from pathlib import Path
import re
s=Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/DetailActivity.java").read_text()
for word in ("shinyFile","buildShinyPrompt","Copy shiny image","shinyJson"):
    for m in list(re.finditer(re.escape(word),s,re.I))[:5]:
        print("SOURCE",word,repr(s[max(0,m.start()-220):m.end()+260]))
assert "BLUE, NOT YELLOW" in s
