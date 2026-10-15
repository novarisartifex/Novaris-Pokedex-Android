# Versioned source migration from the existing native APK workflow.
from pathlib import Path
p=Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/DetailActivity.java")
s=p.read_text()
anchor='Button b1=button("Copy Shiny + Core + Dex x1")'
start=s.find(anchor)
if start<0:raise SystemExit("Actual Shiny x1 button missing")
insert=s.find('pr.addView(b1',start)
if insert<0 or insert-start>2000:raise SystemExit("Shiny prompt listener block missing")
wiring='b1.setOnClickListener(v->copy(buildShinyPrompt(item.dex,shinyBase,false)));\n                b3.setOnClickListener(v->copy(buildShinyPrompt(item.dex,shinyBase,true)));\n                '
s=s[:insert]+wiring+s[insert:]
p.write_text(s)
block=s[start:insert+len(wiring)+60]
assert 'buildShinyPrompt(item.dex,shinyBase,false)' in block
assert 'buildShinyPrompt(item.dex,shinyBase,true)' in block
print("Verified actual Shiny x1/x3 buttons call buildShinyPrompt")
