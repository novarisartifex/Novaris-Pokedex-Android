# Versioned source migration from the existing native APK workflow.
from pathlib import Path
p=Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/MainActivity.java")
s=p.read_text()
marker="private boolean restorePosition;"
if marker not in s:raise SystemExit("Updater MainActivity anchor missing")
s=s.replace(marker,marker+"\n    private AppReleaseUpdater releaseUpdater;",1)
anchor="    private void openPokemon("
if anchor not in s:raise SystemExit("Updater method anchor missing")
method="""    @Override protected void onResume(){
    super.onResume();
    if(releaseUpdater==null)releaseUpdater=new AppReleaseUpdater(this);
    getWindow().getDecorView().postDelayed(()->releaseUpdater.attach(),300);
}
"""
s=s.replace(anchor,method+anchor,1)
p.write_text(s)
manifest=Path("appsrc/app/src/main/AndroidManifest.xml")
m=manifest.read_text()
if 'android.permission.REQUEST_INSTALL_PACKAGES' not in m:
    m=m.replace('<application','<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />\n    <application',1)
manifest.write_text(m)
assert 'releaseUpdater.attach()' in s
