# Versioned source migration from the existing native APK workflow.
from pathlib import Path
import re
for p in Path("appsrc/app/src/main/java").rglob("*.java"):
    s=p.read_text()
    if any(k in s for k in ("Online-Update","Datenupdate verfügbar","github.com","api.github.com")):
        print("UPDATE_SOURCE",p)
        for k in ("Online-Update","Datenupdate verfügbar","github.com","api.github.com"):
            for m in list(re.finditer(re.escape(k),s))[:4]:
                print("UPDATE_CONTEXT",k,repr(s[max(0,m.start()-450):m.end()+700]))
