# Versioned source migration from the existing native APK workflow.
from pathlib import Path
from xml.etree import ElementTree as ET
import shutil
icon=Path("26993.png")
if not icon.is_file():raise SystemExit("Arcanine icon 26993.png missing")
res=Path("appsrc/app/src/main/res")
for folder in ("mipmap-mdpi","mipmap-hdpi","mipmap-xhdpi","mipmap-xxhdpi","mipmap-xxxhdpi"):
    target=res/folder
    target.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(icon,target/"ic_launcher.png")
    shutil.copyfile(icon,target/"ic_launcher_round.png")
manifest=Path("appsrc/app/src/main/AndroidManifest.xml")
content=manifest.read_text()
content=content.replace('android:icon="@drawable/ic_launcher"','android:icon="@mipmap/ic_launcher"')
content=content.replace('android:icon="@mipmap/ic_launcher_foreground"','android:icon="@mipmap/ic_launcher"')
if 'android:icon=' not in content:
    content=content.replace('<application','<application android:icon="@mipmap/ic_launcher"',1)
if 'android:roundIcon=' not in content:
    content=content.replace('<application','<application android:roundIcon="@mipmap/ic_launcher_round"',1)
manifest.write_text(content)
print("Arcanine launcher icon installed")
