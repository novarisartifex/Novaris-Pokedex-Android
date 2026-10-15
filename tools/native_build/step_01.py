# Versioned source migration from the existing native APK workflow.
from pathlib import Path

main = Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/MainActivity.java")
s = main.read_text()

old = 'private boolean restorePosition;'
new = 'private boolean restorePosition;\n    private final List<Integer> currentPokemonDexes = new ArrayList<>();'
if old not in s:
    raise SystemExit("MainActivity field anchor not found")
s = s.replace(old, new, 1)

old = 'runOnUiThread(()->{if(seq!=querySeq.get()||currentTab.equals("pokedex")==false||list!=activeList)return; list.setAdapter(new PokemonAdapter(items,false)); pCount.setText(items.size()+" Einträge"); restoreScrollIfNeeded("p",list);});'
new = 'runOnUiThread(()->{if(seq!=querySeq.get()||currentTab.equals("pokedex")==false||list!=activeList)return; currentPokemonDexes.clear(); for(DatabaseManager.PokemonItem x:items) currentPokemonDexes.add(x.dex); list.setAdapter(new PokemonAdapter(items,false)); pCount.setText(items.size()+" Einträge"); restoreScrollIfNeeded("p",list);});'
if old not in s:
    raise SystemExit("refreshPokedex anchor not found")
s = s.replace(old, new, 1)

old = 'r.setOnClickListener(v->openPokemon(p.dex));return r;'
new = 'r.setOnClickListener(v->openPokemon(p.dex,pos,items));return r;'
if old not in s:
    raise SystemExit("Pokemon click anchor not found")
s = s.replace(old, new, 1)

old = 'private void openPokemon(int dex){Intent i=new Intent(this,DetailActivity.class);i.putExtra("type","pokemon");i.putExtra("dex",dex);startActivity(i);}private void openCharacter'
new = 'private void openPokemon(int dex,int position,List<DatabaseManager.PokemonItem> items){Intent i=new Intent(this,DetailActivity.class);i.putExtra("type","pokemon");i.putExtra("dex",dex);int[] nav=new int[items.size()];for(int k=0;k<items.size();k++)nav[k]=items.get(k).dex;i.putExtra("nav_dexes",nav);i.putExtra("nav_index",position);startActivity(i);}private void openCharacter'
if old not in s:
    raise SystemExit("openPokemon anchor not found")
s = s.replace(old, new, 1)

main.write_text(s)

detail = Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/DetailActivity.java")
s = detail.read_text()

old = 'private LinearLayout body;'
new = 'private LinearLayout body;\n    private int[] navDexes = new int[0];\n    private int navIndex = -1;\n    private float swipeDownX, swipeDownY;'
if old not in s:
    raise SystemExit("Detail fields anchor not found")
s = s.replace(old, new, 1)

old = 'ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);'
new = 'ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);'
# Add swipe listener immediately after setContentView.
anchor = 'setContentView(scroll);\n        String type=getIntent().getStringExtra("type");'
repl = 'setContentView(scroll);\n        navDexes=getIntent().getIntArrayExtra("nav_dexes");if(navDexes==null)navDexes=new int[0];navIndex=getIntent().getIntExtra("nav_index",-1);\n        scroll.setOnTouchListener((v,event)->handleSwipe(event,scroll));\n        String type=getIntent().getStringExtra("type");'
if anchor not in s:
    raise SystemExit("Detail onCreate anchor not found")
s = s.replace(anchor, repl, 1)

old = 'private void showPokemon(int dex){\n        DatabaseManager.PokemonDetail d=data.pokemonDetail(dex);'
new = 'private void showPokemon(int dex){\n        body.removeAllViews();\n        DatabaseManager.PokemonDetail d=data.pokemonDetail(dex);'
if old not in s:
    raise SystemExit("showPokemon anchor not found")
s = s.replace(old, new, 1)

# Put image-copy before filename button for forms and shinies.
old = 'Button fn=button(formFile);fn.setOnClickListener(v->copy(formFile+DOWNLOAD_SUFFIX));card.addView(fn,new LinearLayout.LayoutParams(match(),wrap()));\n                Button ci=button("Copy image");ci.setOnClickListener(v->ImageClipboard.copy(this,url));card.addView(ci,new LinearLayout.LayoutParams(match(),wrap()));'
new = 'Button ci=button("Copy image");ci.setOnClickListener(v->ImageClipboard.copy(this,url));card.addView(ci,new LinearLayout.LayoutParams(match(),wrap()));\n                Button fn=button(formFile);fn.setOnClickListener(v->copy(formFile+DOWNLOAD_SUFFIX));card.addView(fn,new LinearLayout.LayoutParams(match(),wrap()));'
if old not in s:
    raise SystemExit("form button order anchor not found")
s = s.replace(old, new, 1)

old = 'Button fn=button(shinyFile);fn.setOnClickListener(v->copy(shinyFile+DOWNLOAD_SUFFIX));card.addView(fn,new LinearLayout.LayoutParams(match(),wrap()));\n                Button ci=button("Copy shiny image");ci.setOnClickListener(v->ImageClipboard.copy(this,url));card.addView(ci,new LinearLayout.LayoutParams(match(),wrap()));'
new = 'Button ci=button("Copy shiny image");ci.setOnClickListener(v->ImageClipboard.copy(this,url));card.addView(ci,new LinearLayout.LayoutParams(match(),wrap()));\n                Button fn=button(shinyFile);fn.setOnClickListener(v->copy(shinyFile+DOWNLOAD_SUFFIX));card.addView(fn,new LinearLayout.LayoutParams(match(),wrap()));'
if old not in s:
    raise SystemExit("shiny button order anchor not found")
s = s.replace(old, new, 1)

anchor = 'private void copy(String s){'
method = '''private boolean handleSwipe(MotionEvent e,ScrollView scroll){\n        if(!"pokemon".equals(getIntent().getStringExtra("type")))return false;\n        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){swipeDownX=e.getX();swipeDownY=e.getY();return false;}\n        if(e.getActionMasked()==MotionEvent.ACTION_UP){float dx=e.getX()-swipeDownX,dy=e.getY()-swipeDownY;if(Math.abs(dx)>dp(80)&&Math.abs(dx)>Math.abs(dy)*1.2f&&navDexes.length>1&&navIndex>=0){int next=navIndex+(dx<0?1:-1);if(next>=0&&next<navDexes.length){navIndex=next;showPokemon(navDexes[navIndex]);scroll.post(()->scroll.scrollTo(0,0));}}}return false;}\n    private void copy(String s){'''
if anchor not in s:
    raise SystemExit("copy anchor not found")
s = s.replace(anchor, method, 1)

detail.write_text(s)
