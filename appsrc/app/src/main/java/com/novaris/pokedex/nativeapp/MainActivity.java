package com.novaris.pokedex.nativeapp;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
    private static final int REQ_IMPORT = 8040;
    private DatabaseManager data;
    private android.content.SharedPreferences prefs;
    private LinearLayout root, content, tabs;
    private TextView versionView;
    private final Map<String,Button> tabButtons = new LinkedHashMap<>();
    private String currentTab = "pokedex";
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicInteger querySeq = new AtomicInteger();
    private ListView activeList;
    private boolean restorePosition;
    private AppReleaseUpdater releaseUpdater;
    private final List<Integer> currentPokemonDexes = new ArrayList<>();

    private EditText pSearch, jSearch, classSearch, charSearch;
    private Spinner pGen, jGen, charGen, charCategory;
    private CheckBox jChanged, pChanged;
    private Button pSelection;
    private TextView pCount, jCount, classCount, charCount;
    private Set<Integer> selectedDex = new LinkedHashSet<>();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs=getSharedPreferences("novaris_native_state",MODE_PRIVATE);
        try { data=new DatabaseManager(this); }
        catch(Exception e){ fatal("Datenbank konnte nicht geladen werden",e); return; }
        currentTab=prefs.getString("active_tab","pokedex");
        buildShell();
        showTab(currentTab,false);
    }

    private void buildShell(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12),dp(10),dp(12),dp(8)); root.setBackgroundColor(Color.rgb(244,246,250));
        LinearLayout head=new LinearLayout(this); head.setOrientation(LinearLayout.HORIZONTAL); head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("Novaris Pokédex Native",22,true); head.addView(title,new LinearLayout.LayoutParams(0,wrap(),1));
        Button source=button("Datenquelle"); source.setOnClickListener(v->pickDataSource()); head.addView(source);
        root.addView(head);
        versionView=text("",12,false); versionView.setTextColor(Color.DKGRAY); root.addView(versionView);
        updateVersionLabel();

        HorizontalScrollView hs=new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false);
        tabs=new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL); hs.addView(tabs,new HorizontalScrollView.LayoutParams(wrap(),wrap()));
        addTab("pokedex","Pokédex"); addTab("jobs","Job-Zuordnung"); addTab("classes","Klassenübersicht"); addTab("characters","Characters");
        root.addView(hs,new LinearLayout.LayoutParams(match(),wrap()));
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content,new LinearLayout.LayoutParams(match(),0,1));
        setContentView(root);
    }

    private void addTab(String id,String label){
        Button b=button(label); b.setAllCaps(false); b.setOnClickListener(v->showTab(id,true)); tabs.addView(b,new LinearLayout.LayoutParams(wrap(),dp(48))); tabButtons.put(id,b);
    }

    private void showTab(String id, boolean saveOld){
        if(!tabButtons.containsKey(id)) id="pokedex";
        if(saveOld) saveScroll();
        currentTab=id; prefs.edit().putString("active_tab",id).apply();
        for(Map.Entry<String,Button> e:tabButtons.entrySet()) e.getValue().setAlpha(e.getKey().equals(id)?1f:.55f);
        content.removeAllViews(); activeList=null; restorePosition=true;
        if(id.equals("pokedex")) buildPokedex();
        else if(id.equals("jobs")) buildJobs();
        else if(id.equals("classes")) buildClasses();
        else buildCharacters();
    }

    private void buildPokedex(){
        pSearch=searchBox("Liste durchsuchen: Name, Nummer, Klasse, Tags …",prefs.getString("p_search","")); content.addView(pSearch);
        pGen=generationSpinner(prefs.getInt("p_gen",0)); content.addView(pGen);
        selectedDex=parseDexSet(prefs.getString("p_selected",""));
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        pSelection=button("Pokémon-Auswahl"); pSelection.setOnClickListener(v->showPokemonSelection()); row.addView(pSelection);
        pCount=text("",13,false); pCount.setGravity(Gravity.END); row.addView(pCount,new LinearLayout.LayoutParams(0,wrap(),1)); content.addView(row); updateSelectionButton();
        pChanged=new CheckBox(this);pChanged.setText("Nur NEU ERSTELLEN");pChanged.setChecked(prefs.getBoolean("p_changed",false));content.addView(pChanged);
        ListView list=new ListView(this); list.setDividerHeight(1); activeList=list; content.addView(list,new LinearLayout.LayoutParams(match(),0,1));
        pSearch.addTextChangedListener(watcher(()->{prefs.edit().putString("p_search",pSearch.getText().toString()).apply();refreshPokedex();}));
        pGen.setOnItemSelectedListener(spinnerListener(pos->{prefs.edit().putInt("p_gen",pos).apply();refreshPokedex();}));
        pChanged.setOnCheckedChangeListener((b,x)->{prefs.edit().putBoolean("p_changed",x).apply();refreshPokedex();});
        refreshPokedex();
    }

    private void refreshPokedex(){
        if(pSearch==null||pGen==null||activeList==null)return; final int seq=querySeq.incrementAndGet();
        final String q=pSearch.getText().toString(); final int gen=pGen.getSelectedItemPosition(); final Set<Integer> sel=new LinkedHashSet<>(selectedDex); final boolean changed=pChanged!=null&&pChanged.isChecked(); final ListView list=activeList;
        handler.removeCallbacksAndMessages("pquery");
        handler.postAtTime(()->exec.execute(()->{
            List<DatabaseManager.PokemonItem> items=data.queryPokemon(q,gen,sel,changed,null);
            runOnUiThread(()->{if(seq!=querySeq.get()||currentTab.equals("pokedex")==false||list!=activeList)return; currentPokemonDexes.clear(); for(DatabaseManager.PokemonItem x:items) currentPokemonDexes.add(x.dex); list.setAdapter(new PokemonAdapter(items,false)); pCount.setText(items.size()+" Einträge"); restoreScrollIfNeeded("p",list);});
        }),"pquery",SystemClock.uptimeMillis()+220);
    }

    private void buildJobs(){
        jSearch=searchBox("Pokémon, Klasse oder Familie suchen …",prefs.getString("j_search",""));content.addView(jSearch);
        jGen=generationSpinner(prefs.getInt("j_gen",0));content.addView(jGen);
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);jChanged=new CheckBox(this);jChanged.setText("nur geänderte");jChanged.setChecked(prefs.getBoolean("j_changed",false));r.addView(jChanged);jCount=text("",13,false);jCount.setGravity(Gravity.END);r.addView(jCount,new LinearLayout.LayoutParams(0,wrap(),1));content.addView(r);
        ListView list=new ListView(this);activeList=list;content.addView(list,new LinearLayout.LayoutParams(match(),0,1));
        jSearch.addTextChangedListener(watcher(()->{prefs.edit().putString("j_search",jSearch.getText().toString()).apply();refreshJobs(null);}));
        jGen.setOnItemSelectedListener(spinnerListener(pos->{prefs.edit().putInt("j_gen",pos).apply();refreshJobs(null);}));
        jChanged.setOnCheckedChangeListener((b,x)->{prefs.edit().putBoolean("j_changed",x).apply();refreshJobs(null);}); refreshJobs(null);
    }

    private void refreshJobs(String exactRole){
        if(jSearch==null||jGen==null||activeList==null)return; final int seq=querySeq.incrementAndGet(); final String q=jSearch.getText().toString(); final int gen=jGen.getSelectedItemPosition(); final boolean changed=jChanged.isChecked(); final ListView list=activeList;
        exec.execute(()->{List<DatabaseManager.PokemonItem> items=data.queryPokemon(q,gen,null,changed,exactRole);runOnUiThread(()->{if(seq!=querySeq.get()||!currentTab.equals("jobs")||list!=activeList)return;list.setAdapter(new PokemonAdapter(items,true));jCount.setText(items.size()+" Einträge");restoreScrollIfNeeded("j",list);});});
    }

    private void buildClasses(){
        classSearch=searchBox("Klasse / Beruf suchen …",prefs.getString("c_search",""));content.addView(classSearch);classCount=text("",13,false);content.addView(classCount);
        ListView list=new ListView(this);activeList=list;content.addView(list,new LinearLayout.LayoutParams(match(),0,1));
        classSearch.addTextChangedListener(watcher(()->{prefs.edit().putString("c_search",classSearch.getText().toString()).apply();refreshClasses();}));refreshClasses();
    }

    private void refreshClasses(){
        if(classSearch==null||activeList==null)return;final int seq=querySeq.incrementAndGet();final String q=classSearch.getText().toString();final ListView list=activeList;
        exec.execute(()->{List<DatabaseManager.ClassItem> items=data.queryClasses(q);runOnUiThread(()->{if(seq!=querySeq.get()||!currentTab.equals("classes")||list!=activeList)return;list.setAdapter(new ClassAdapter(items));classCount.setText(items.size()+" Klassen");restoreScrollIfNeeded("c",list);});});
    }

    private void buildCharacters(){
        charSearch=searchBox("Charakter, Serie, Rolle, Szene suchen …",prefs.getString("ch_search",""));content.addView(charSearch);
        charGen=new Spinner(this);String[] gens={"Alle Anime-Generationen","Originalserie · Kanto / Johto","Advanced Generation · Hoenn","Diamond & Pearl · Sinnoh","Best Wishes! · Einall","XY / XYZ · Kalos","Sun & Moon · Alola","Pokémon Reisen","Pokémon Horizonte","Weitere"};charGen.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,gens));charGen.setSelection(prefs.getInt("ch_gen_pos",0));content.addView(charGen);
        charCategory=new Spinner(this);List<String> cats=new ArrayList<>();cats.add("Alle Rollen");cats.addAll(data.characterCategories());charCategory.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,cats));int cp=Math.min(prefs.getInt("ch_cat_pos",0),cats.size()-1);charCategory.setSelection(Math.max(0,cp));content.addView(charCategory);
        charCount=text("",13,false);content.addView(charCount);ListView list=new ListView(this);activeList=list;content.addView(list,new LinearLayout.LayoutParams(match(),0,1));
        charSearch.addTextChangedListener(watcher(()->{prefs.edit().putString("ch_search",charSearch.getText().toString()).apply();refreshCharacters();}));
        charGen.setOnItemSelectedListener(spinnerListener(pos->{prefs.edit().putInt("ch_gen_pos",pos).apply();refreshCharacters();}));
        charCategory.setOnItemSelectedListener(spinnerListener(pos->{prefs.edit().putInt("ch_cat_pos",pos).apply();refreshCharacters();}));refreshCharacters();
    }

    private void refreshCharacters(){
        if(charSearch==null||charGen==null||charCategory==null||activeList==null)return;final int seq=querySeq.incrementAndGet();final String q=charSearch.getText().toString();final String gen=charGenKey(charGen.getSelectedItemPosition());final String cat=charCategory.getSelectedItemPosition()==0?"all":String.valueOf(charCategory.getSelectedItem());final ListView list=activeList;
        exec.execute(()->{List<DatabaseManager.CharacterItem> items=data.queryCharacters(q,gen,cat);runOnUiThread(()->{if(seq!=querySeq.get()||!currentTab.equals("characters")||list!=activeList)return;list.setAdapter(new CharacterAdapter(items));charCount.setText(items.size()+" Charaktere");restoreScrollIfNeeded("ch",list);});});
    }

    private String charGenKey(int pos){String[] k={"all","original","advanced","dp","bw","xy","sm","journeys","horizons","other"};return k[Math.max(0,Math.min(pos,k.length-1))];}

    private void showPokemonSelection(){
        final Dialog d=new Dialog(this); LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(12),dp(12),dp(12));
        EditText search=searchBox("Pokémon suchen …","");box.addView(search);ListView list=new ListView(this);box.addView(list,new LinearLayout.LayoutParams(match(),0,1));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button all=button("Alle anzeigen");Button done=button("Fertig");actions.addView(all,new LinearLayout.LayoutParams(0,wrap(),1));actions.addView(done,new LinearLayout.LayoutParams(0,wrap(),1));box.addView(actions);
        Set<Integer> working=new LinkedHashSet<>(selectedDex);List<DatabaseManager.PokemonItem> source=data.allPokemonShort();SelectionAdapter adapter=new SelectionAdapter(source,working);list.setAdapter(adapter);search.addTextChangedListener(watcher(()->adapter.filter(search.getText().toString())));
        all.setOnClickListener(v->{working.clear();adapter.notifyDataSetChanged();});done.setOnClickListener(v->{selectedDex.clear();selectedDex.addAll(working);prefs.edit().putString("p_selected",joinDex(selectedDex)).apply();updateSelectionButton();d.dismiss();refreshPokedex();});
        d.setContentView(box);Window w=d.getWindow();if(w!=null)w.setLayout(match(),match());d.show();if(w!=null)w.setLayout(match(),match());
    }

    private void updateSelectionButton(){if(pSelection!=null)pSelection.setText(selectedDex.isEmpty()?"Pokémon-Auswahl: Alle":"Pokémon-Auswahl: "+selectedDex.size());}

    private void pickDataSource(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,REQ_IMPORT);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent intent){super.onActivityResult(requestCode,resultCode,intent);if(requestCode==REQ_IMPORT&&resultCode==RESULT_OK&&intent!=null&&intent.getData()!=null){Uri u=intent.getData();new AlertDialog.Builder(this).setTitle("Datenquelle importieren").setMessage("Die aktuelle native Pokédex-Datenbank wird ersetzt. Filter und Auswahl bleiben erhalten.").setNegativeButton("Abbrechen",null).setPositiveButton("Importieren",(x,y)->exec.execute(()->{try{String v=data.importGzip(u);runOnUiThread(()->{updateVersionLabel();showTab(currentTab,false);toast("Datenquelle "+v+" importiert");});}catch(Exception e){runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Import fehlgeschlagen").setMessage(e.getMessage()).setPositiveButton("OK",null).show());}})).show();}}
    private void updateVersionLabel(){if(versionView!=null)versionView.setText("Native Datenquelle: "+data.metadata("source_version")+" · "+data.metadata("pokemon_count")+" Pokémon · "+data.metadata("character_count")+" Characters");}

    private class PokemonAdapter extends BaseAdapter {
        final List<DatabaseManager.PokemonItem> items; final boolean jobMode; PokemonAdapter(List<DatabaseManager.PokemonItem> x,boolean j){items=x;jobMode=j;}
        public int getCount(){return items.size();}public Object getItem(int p){return items.get(p);}public long getItemId(int p){return items.get(p).dex;}
        public View getView(int pos,View convert,android.view.ViewGroup parent){
            LinearLayout r=(convert instanceof LinearLayout)?(LinearLayout)convert:null;ImageView img;TextView a,b;
            if(r==null){r=new LinearLayout(MainActivity.this);r.setOrientation(LinearLayout.HORIZONTAL);r.setPadding(dp(7),dp(7),dp(7),dp(7));img=new ImageView(MainActivity.this);img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);r.addView(img,new LinearLayout.LayoutParams(dp(72),dp(72)));LinearLayout t=new LinearLayout(MainActivity.this);t.setOrientation(LinearLayout.VERTICAL);a=text("",16,true);b=text("",13,false);t.addView(a);t.addView(b);r.addView(t,new LinearLayout.LayoutParams(0,wrap(),1));r.setTag(new Object[]{img,a,b});}else{Object[] z=(Object[])r.getTag();img=(ImageView)z[0];a=(TextView)z[1];b=(TextView)z[2];}
            DatabaseManager.PokemonItem p=items.get(pos);a.setText(p.label+(p.de==null||p.de.isEmpty()?"":" · "+p.de));String sub=jobMode?("Gen "+p.generation+" · "+safe(p.role)+" · "+safe(p.tier)+"\n"+safe(p.family)):("Generation "+p.generation+(p.role==null||p.role.isEmpty()?"":" · "+p.role));if(p.changed==1)sub="NEU ERSTELLEN · "+sub;b.setText(sub);ImageLoader.load(MainActivity.this,p.art,img);r.setOnClickListener(v->openPokemon(p.dex,pos,items));return r;
        }
    }

    private class ClassAdapter extends BaseAdapter {final List<DatabaseManager.ClassItem> items;ClassAdapter(List<DatabaseManager.ClassItem>x){items=x;}public int getCount(){return items.size();}public Object getItem(int p){return items.get(p);}public long getItemId(int p){return p;}public View getView(int pos,View convert,android.view.ViewGroup parent){TextView v=convert instanceof TextView?(TextView)convert:text("",15,false);v.setPadding(dp(10),dp(12),dp(10),dp(12));DatabaseManager.ClassItem x=items.get(pos);v.setText(x.role+"  ·  "+x.count+"\n"+safe(x.category)+(x.tier==null||x.tier.isEmpty()?"":" · "+x.tier));v.setOnClickListener(z->{prefs.edit().putString("j_search",x.role).apply();showTab("jobs",true);jSearch.setText(x.role);});return v;}}
    private class CharacterAdapter extends BaseAdapter {final List<DatabaseManager.CharacterItem> items;CharacterAdapter(List<DatabaseManager.CharacterItem>x){items=x;}public int getCount(){return items.size();}public Object getItem(int p){return items.get(p);}public long getItemId(int p){return p;}public View getView(int pos,View convert,android.view.ViewGroup parent){LinearLayout r=(convert instanceof LinearLayout)?(LinearLayout)convert:null;ImageView img;TextView a,b;if(r==null){r=new LinearLayout(MainActivity.this);r.setOrientation(LinearLayout.HORIZONTAL);r.setPadding(dp(7),dp(7),dp(7),dp(7));img=new ImageView(MainActivity.this);img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);r.addView(img,new LinearLayout.LayoutParams(dp(72),dp(86)));LinearLayout t=new LinearLayout(MainActivity.this);t.setOrientation(LinearLayout.VERTICAL);a=text("",16,true);b=text("",13,false);t.addView(a);t.addView(b);r.addView(t,new LinearLayout.LayoutParams(0,wrap(),1));r.setTag(new Object[]{img,a,b});}else{Object[] z=(Object[])r.getTag();img=(ImageView)z[0];a=(TextView)z[1];b=(TextView)z[2];}DatabaseManager.CharacterItem c=items.get(pos);a.setText(c.name);b.setText(safe(c.category)+"\n"+safe(c.source));ImageLoader.load(MainActivity.this,c.portrait,img);r.setOnClickListener(v->openCharacter(c.id));return r;}}

    private class SelectionAdapter extends BaseAdapter {final List<DatabaseManager.PokemonItem> all,visible=new ArrayList<>();final Set<Integer> working;SelectionAdapter(List<DatabaseManager.PokemonItem>a,Set<Integer>w){all=a;working=w;visible.addAll(a);}void filter(String q){visible.clear();String x=q==null?"":q.toLowerCase(Locale.ROOT).trim();for(DatabaseManager.PokemonItem p:all)if(x.isEmpty()||(p.label+" "+p.en+" "+p.de).toLowerCase(Locale.ROOT).contains(x))visible.add(p);notifyDataSetChanged();}public int getCount(){return visible.size();}public Object getItem(int p){return visible.get(p);}public long getItemId(int p){return visible.get(p).dex;}public View getView(int pos,View convert,android.view.ViewGroup parent){CheckedTextView v=convert instanceof CheckedTextView?(CheckedTextView)convert:new CheckedTextView(MainActivity.this);v.setPadding(dp(12),dp(12),dp(12),dp(12));v.setCheckMarkDrawable(android.R.drawable.checkbox_on_background);DatabaseManager.PokemonItem p=visible.get(pos);v.setText(p.label+" · "+safe(p.de));v.setChecked(working.contains(p.dex));v.setOnClickListener(z->{if(working.contains(p.dex))working.remove(p.dex);else working.add(p.dex);v.setChecked(working.contains(p.dex));});return v;}}

    @Override protected void onResume(){
    super.onResume();
    if(releaseUpdater==null)releaseUpdater=new AppReleaseUpdater(this);
    getWindow().getDecorView().postDelayed(()->releaseUpdater.attach(),300);
}
    private void openPokemon(int dex,int position,List<DatabaseManager.PokemonItem> items){Intent i=new Intent(this,DetailActivity.class);i.putExtra("type","pokemon");i.putExtra("dex",dex);int[] nav=new int[items.size()];for(int k=0;k<items.size();k++)nav[k]=items.get(k).dex;i.putExtra("nav_dexes",nav);i.putExtra("nav_index",position);startActivity(i);}private void openCharacter(String id){Intent i=new Intent(this,DetailActivity.class);i.putExtra("type","character");i.putExtra("id",id);startActivity(i);}

    private EditText searchBox(String hint,String value){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(16);e.setHint(hint);e.setText(value);e.setSelection(e.getText().length());e.setPadding(dp(10),dp(6),dp(10),dp(6));return e;}
    private Spinner generationSpinner(int selected){Spinner s=new Spinner(this);String[] a={"Alle Generationen","Generation 1","Generation 2","Generation 3","Generation 4","Generation 5","Generation 6","Generation 7","Generation 8","Generation 9"};s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,a));s.setSelection(Math.max(0,Math.min(selected,9)));return s;}
    private TextWatcher watcher(Runnable r){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){r.run();}public void afterTextChanged(Editable e){}};}
    private AdapterView.OnItemSelectedListener spinnerListener(java.util.function.IntConsumer c){return new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?>p,View v,int pos,long id){c.accept(pos);}public void onNothingSelected(AdapterView<?>p){}};}

    private void saveScroll(){if(activeList==null)return;String key=currentTab.equals("pokedex")?"p":currentTab.equals("jobs")?"j":currentTab.equals("classes")?"c":"ch";int pos=activeList.getFirstVisiblePosition();View v=activeList.getChildAt(0);int top=v==null?0:v.getTop();prefs.edit().putInt(key+"_pos",pos).putInt(key+"_top",top).apply();}
    private void restoreScrollIfNeeded(String key,ListView list){if(!restorePosition)return;restorePosition=false;int pos=prefs.getInt(key+"_pos",0),top=prefs.getInt(key+"_top",0);list.post(()->list.setSelectionFromTop(pos,top));}
    @Override protected void onPause(){saveScroll();super.onPause();}
    @Override protected void onDestroy(){if(isFinishing()){exec.shutdownNow();if(data!=null)data.close();}super.onDestroy();}

    private Set<Integer> parseDexSet(String s){Set<Integer>x=new LinkedHashSet<>();if(s!=null)for(String p:s.split(","))try{x.add(Integer.parseInt(p.trim()));}catch(Exception ignored){}return x;}private String joinDex(Set<Integer>s){StringBuilder b=new StringBuilder();for(Integer i:s){if(b.length()>0)b.append(',');b.append(i);}return b.toString();}
    private String safe(String s){return s==null?"":s;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}private int match(){return ViewGroup.LayoutParams.MATCH_PARENT;}private int wrap(){return ViewGroup.LayoutParams.WRAP_CONTENT;}
    private TextView text(String s,int size,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Color.rgb(23,32,51));if(bold)v.setTypeface(null,android.graphics.Typeface.BOLD);v.setPadding(dp(4),dp(4),dp(4),dp(4));return v;}private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void fatal(String title,Exception e){new AlertDialog.Builder(this).setTitle(title).setMessage(e.toString()).setPositiveButton("Schließen",(d,w)->finish()).show();}
}
