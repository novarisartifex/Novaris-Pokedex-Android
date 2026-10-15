package com.novaris.pokedex.nativeapp;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import org.json.*;

public class DetailActivity extends Activity {
    private static final String DOWNLOAD_SUFFIX = " Ist der Dateiname. Erstelle mir jetzt einen Downloadlink für dieses Bild.";
    private DatabaseManager data;
    private LinearLayout body;
    private int[] navDexes = new int[0];
    private int navIndex = -1;
    private float swipeDownX, swipeDownY;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        try{data=new DatabaseManager(this);}catch(Exception e){finish();return;}
        ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(14),dp(12),dp(14),dp(24));body.setBackgroundColor(Color.rgb(244,246,250));scroll.addView(body,new ScrollView.LayoutParams(match(),wrap()));setContentView(scroll);
        navDexes=getIntent().getIntArrayExtra("nav_dexes");if(navDexes==null)navDexes=new int[0];navIndex=getIntent().getIntExtra("nav_index",-1);
        scroll.setOnTouchListener((v,event)->handleSwipe(event,scroll));
        String type=getIntent().getStringExtra("type");if("character".equals(type))showCharacter(getIntent().getStringExtra("id"));else showPokemon(getIntent().getIntExtra("dex",0));
    }

    private void showPokemon(int dex){
        body.removeAllViews();
        DatabaseManager.PokemonDetail d=data.pokemonDetail(dex);if(d==null){finish();return;}
        title(d.item.label+(d.item.de==null||d.item.de.isEmpty()?"":" · "+d.item.de));
        if(d.item.changed==1){TextView badge=label("NEU ERSTELLEN",14,true);badge.setTextColor(Color.rgb(20,120,70));badge.setGravity(Gravity.END);body.addView(badge);}

        ImageView art=image(dp(260),dp(260));body.addView(art,new LinearLayout.LayoutParams(match(),dp(270)));ImageLoader.load(this,d.item.art,art);
        LinearLayout imgActions=new LinearLayout(this);imgActions.setOrientation(LinearLayout.HORIZONTAL);
        Button copyImage=button("Copy image");copyImage.setOnClickListener(v->ImageClipboard.copy(this,d.item.art));imgActions.addView(copyImage,new LinearLayout.LayoutParams(0,wrap(),1));
        if(d.item.role!=null&&!d.item.role.isEmpty()){Button role=button(d.item.role);role.setEnabled(false);imgActions.addView(role,new LinearLayout.LayoutParams(0,wrap(),1));}
        body.addView(imgActions);
        addJobEditor(d.item.dex,d.item.role);

        String mainFile=archiveFilename(d.item,d.formsJson,"");
        String mainDisplay=mainFile+((d.item.de==null||d.item.de.isEmpty()||d.item.de.equalsIgnoreCase(d.item.en))?"":" / "+d.item.de);
        Button filename=button(mainDisplay);
        filename.setOnClickListener(v->copy(mainFile+DOWNLOAD_SUFFIX));body.addView(filename,new LinearLayout.LayoutParams(match(),wrap()));

        LinearLayout promptRow=new LinearLayout(this);promptRow.setOrientation(LinearLayout.HORIZONTAL);
        Button x1=button("Copy Core + Dex x1");Button x3=button("Copy Core + Dex x3");
        x1.setOnClickListener(v->copy(applyEditedJob(d.item.dex,data.buildPokemonPrompt(d.item.dex,false),d.item.role)));x3.setOnClickListener(v->copyTriple(data.buildPokemonPrompt(d.item.dex,true)));
        promptRow.addView(x1,new LinearLayout.LayoutParams(0,dp(62),1));promptRow.addView(x3,new LinearLayout.LayoutParams(0,dp(62),1));body.addView(promptRow);

        small("Generation "+d.item.generation+(d.item.role==null||d.item.role.isEmpty()?"":" · "+d.item.role)+(d.item.tier==null||d.item.tier.isEmpty()?"":" · "+d.item.tier));
        if(d.item.family!=null&&!d.item.family.isEmpty()) section("Klassenfamilie",d.item.family);
        addOriginalVariants(dex);
        addFormCards("Weitere Formen",d.item,d.formsJson);
        try{
            JSONArray forms=new JSONArray(d.formsJson);
            for(int fi=0;fi<forms.length();fi++){
                JSONObject form=forms.optJSONObject(fi);
                if(form!=null) addFormClassVariants(d.item.dex,fi+1,form.optString("name","Form"),form.optString("url",""),form.optString("blueprint",""),body.getChildCount());
            }
        }catch(Exception e){android.util.Log.e("NovarisFormVariants","Cannot render form variants",e);}
        addShinyCards(d.item,d.formsJson,d.shinyJson);
        try{JSONArray arr=new JSONArray(d.cellsJson);for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o==null)continue;String h=o.optString("heading","Abschnitt");String t=o.optString("text","");if(t.trim().isEmpty())continue;if(h.equals("Pokémon Artwork")||h.equals("Weitere Formen")||h.equals("Shiny-Versionen"))continue;section(h,t);}}catch(Exception e){section("Daten",d.cellsJson);}
    }

    private void addFormCards(String heading,DatabaseManager.PokemonItem item,String json){
        try{
            JSONArray a=new JSONArray(json);if(a.length()==0)return;TextView h=label(heading,17,true);h.setPadding(dp(4),dp(16),dp(4),dp(7));body.addView(h);
            for(int i=0;i<a.length();i++){
                JSONObject o=a.optJSONObject(i);if(o==null)continue;String name=o.optString("name","Form"),url=o.optString("url",""),blueprint=o.optString("blueprint","");
                LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(8),dp(8),dp(8),dp(8));card.setBackgroundColor(Color.WHITE);
                TextView n=label(name,15,true);card.addView(n);
                ImageView im=image(dp(190),dp(190));card.addView(im,new LinearLayout.LayoutParams(match(),dp(200)));ImageLoader.load(this,url,im);
                String formFile=archiveFilename(item,json,name);
                Button ci=button("Copy image");ci.setOnClickListener(v->ImageClipboard.copy(this,url));card.addView(ci,new LinearLayout.LayoutParams(match(),wrap()));
                Button fn=button(formFile);fn.setOnClickListener(v->copy(formFile+DOWNLOAD_SUFFIX));card.addView(fn,new LinearLayout.LayoutParams(match(),wrap()));
                LinearLayout pr=new LinearLayout(this);pr.setOrientation(LinearLayout.HORIZONTAL);Button b1=button("Copy "+name+" + Core + Dex x1");Button b3=button("Copy "+name+" + Core + Dex x3");
                b1.setOnClickListener(v->copy(data.buildPokemonPrompt(item.dex,false,name,blueprint)));b3.setOnClickListener(v->copyTriple(data.buildPokemonPrompt(item.dex,true,name,blueprint)));pr.addView(b1,new LinearLayout.LayoutParams(0,dp(66),1));pr.addView(b3,new LinearLayout.LayoutParams(0,dp(66),1));card.addView(pr);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(match(),wrap());lp.setMargins(0,dp(6),0,dp(8));body.addView(card,lp);
            }
        }catch(Exception ignored){}
    }

    private void showCharacter(String id){
        DatabaseManager.CharacterDetail d=data.characterDetail(id);if(d==null){finish();return;}
        title(d.item.name);ImageView art=image(dp(220),dp(260));body.addView(art,new LinearLayout.LayoutParams(match(),dp(270)));ImageLoader.load(this,d.item.portrait,art);
        Button copyImage=button("Copy image");copyImage.setOnClickListener(v->ImageClipboard.copy(this,d.item.portrait));body.addView(copyImage,new LinearLayout.LayoutParams(match(),wrap()));
        small(join(" · ",d.item.category,d.kind,d.version));section("Quelle / Serie",join("\n",d.item.source,d.item.aliases));
        if(d.canon!=null&&!d.canon.isEmpty())section("Kanon / Quelle",d.canon);if(d.novaris!=null&&!d.novaris.isEmpty())section("Novaris Interpretation",d.novaris);
        try{JSONArray a=new JSONArray(d.scenesJson);for(int i=0;i<a.length();i++){JSONObject s=a.optJSONObject(i);if(s==null)continue;String name=s.optString("title","Szene "+(i+1));String text=s.optString("prompt",s.toString(2));section("Szene "+(i+1)+" · "+name,text);}}catch(Exception e){if(d.scenesJson!=null)section("Szenen",d.scenesJson);}
    }

    private void addShinyCards(DatabaseManager.PokemonItem item,String formsJson,String json){
        try{
            JSONArray a=new JSONArray(json);if(a.length()==0)return;TextView h=label("Shiny-Versionen",17,true);h.setPadding(dp(4),dp(16),dp(4),dp(7));body.addView(h);
            for(int i=0;i<a.length();i++){
                JSONObject o=a.optJSONObject(i);if(o==null)continue;String name=o.optString("name","Shiny"),url=o.optString("url","");
                LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(8),dp(8),dp(8),dp(8));card.setBackgroundColor(Color.WHITE);
                TextView n=label(name,15,true);card.addView(n);
                ImageView im=image(dp(190),dp(190));card.addView(im,new LinearLayout.LayoutParams(match(),dp(200)));ImageLoader.load(this,url,im);
                String shinyBase=name.replace(" — Shiny","").trim();
                boolean baseShiny=shinyBase.equalsIgnoreCase(item.en)||shinyBase.equalsIgnoreCase(item.label)||shinyBase.equalsIgnoreCase(item.de);
                String shinyFile=shinyArchiveFilename(item,formsJson,baseShiny?"":shinyBase);
                Button ci=button("Copy shiny image");ci.setOnClickListener(v->ImageClipboard.copy(this,url));card.addView(ci,new LinearLayout.LayoutParams(match(),wrap()));
                Button fn=button(shinyFile);fn.setOnClickListener(v->copy(shinyFile+DOWNLOAD_SUFFIX));card.addView(fn,new LinearLayout.LayoutParams(match(),wrap()));
                LinearLayout pr=new LinearLayout(this);pr.setOrientation(LinearLayout.HORIZONTAL);Button b1=button("Copy Shiny + Core + Dex x1");Button b3=button("Copy Shiny + Core + Dex x3");
                b1.setOnClickListener(v->copy(data.buildPokemonShinyPrompt(item.dex,false,name)));b3.setOnClickListener(v->copyTriple(data.buildPokemonShinyPrompt(item.dex,true,name)));
                b1.setOnClickListener(v->copy(buildShinyPrompt(item.dex,shinyBase,false)));
                b3.setOnClickListener(v->copyTriple(buildShinyPrompt(item.dex,shinyBase,true)));
                pr.addView(b1,new LinearLayout.LayoutParams(0,dp(66),1));pr.addView(b3,new LinearLayout.LayoutParams(0,dp(66),1));card.addView(pr);
                small("Shiny-Farbpalette: Referenzbild ist Farbautorität; normale Speziesfarben werden bei Abweichungen überschrieben.");
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(match(),wrap());lp.setMargins(0,dp(6),0,dp(8));body.addView(card,lp);
            }
        }catch(Exception ignored){}
    }


    private String shinyArchiveFilename(DatabaseManager.PokemonItem item,String formsJson,String formLabel){
        String normal=archiveFilename(item,formsJson,formLabel);
        String role=filenameSlug(item.role);
        if(!role.isEmpty()){
            String suffix="_"+role;
            if(normal.endsWith(suffix))return normal.substring(0,normal.length()-suffix.length())+"_Shiny"+suffix;
        }
        return normal+"_Shiny";
    }

    private String archiveFilename(DatabaseManager.PokemonItem item,String formsJson,String formLabel){
        String base=String.format(java.util.Locale.ROOT,"#%04d.%s",item.dex,filenameSlug(item.dex==718?"Zygarde":item.en));
        try{
            JSONArray forms=new JSONArray(formsJson==null?"[]":formsJson);
            if(forms.length()>0){
                String wanted=formLabel==null?"":formLabel.trim();
                if(wanted.isEmpty()||wanted.equalsIgnoreCase("standard")||wanted.equalsIgnoreCase("main")){
                    base += "_00_"+baseFormToken(item.dex);
                }else{
                    int idx=-1;
                    for(int i=0;i<forms.length();i++){JSONObject f=forms.optJSONObject(i);if(f!=null&&wanted.equalsIgnoreCase(f.optString("name",""))){idx=i;break;}}
                    if(idx<0)idx=forms.length();
                    base += "_"+String.format(java.util.Locale.ROOT,"%02d",idx+1)+"_"+archiveFormToken(item.dex,item.en,wanted);
                }
            }
        }catch(Exception ignored){}
        String role=filenameSlug(item.role);
        return role.isEmpty()?base:base+"_"+role;
    }

    private String baseFormToken(int dex){
        if(dex==718)return "50%_Forme";
        if(dex==741)return "Flamenco";
        if(dex==745)return "Midday";
        if(dex==746)return "Solo_Form";
        return "Main";
    }

    private String archiveFormToken(int dex,String species,String label){
        String chosen=label==null?"":label.trim();
        if(dex==741){
            if(chosen.equalsIgnoreCase("pom pom"))chosen="Cheerleading";
            else if(chosen.equalsIgnoreCase("pau"))chosen="Hula";
            else if(chosen.equalsIgnoreCase("sensu"))chosen="Buyo";
        }
        String sp=species==null?"":species.trim();
        if(!sp.isEmpty()&&chosen.toLowerCase(java.util.Locale.ROOT).startsWith(sp.toLowerCase(java.util.Locale.ROOT)+" "))chosen=chosen.substring(sp.length()).trim();
        return filenameSlug(chosen.isEmpty()?"Form":chosen);
    }

    private String filenameSlug(String value){
        String x=value==null?"":value.trim();
        x=x.replace("♀","Female").replace("♂","Male").replace("’","").replace("'","").replace("`","");
        x=x.replaceAll("[^A-Za-z0-9%+.-]+","_").replaceAll("_+","_").replaceAll("^_+|_+$","");
        return x;
    }

    private void section(String heading,String text){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(10),dp(9),dp(10),dp(9));card.setBackgroundColor(Color.WHITE);
        TextView h=label(heading,16,true);card.addView(h);TextView t=label(text,14,false);t.setTextIsSelectable(true);t.setPadding(0,dp(5),0,dp(5));card.addView(t);
        Button copy=button("Kopieren");copy.setOnClickListener(v->copy(text));card.addView(copy,new LinearLayout.LayoutParams(wrap(),wrap()));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(match(),wrap());lp.setMargins(0,dp(7),0,dp(7));body.addView(card,lp);
    }
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private boolean handleSwipe(MotionEvent e,ScrollView scroll){
        if(!"pokemon".equals(getIntent().getStringExtra("type")))return false;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){swipeDownX=e.getX();swipeDownY=e.getY();return false;}
        if(e.getActionMasked()==MotionEvent.ACTION_UP){float dx=e.getX()-swipeDownX,dy=e.getY()-swipeDownY;if(Math.abs(dx)>dp(80)&&Math.abs(dx)>Math.abs(dy)*1.2f&&navDexes.length>1&&navIndex>=0){int next=navIndex+(dx<0?1:-1);if(next>=0&&next<navDexes.length){navIndex=next;showPokemon(navDexes[navIndex]);scroll.post(()->scroll.scrollTo(0,0));}}}return false;}
    private String buildShinyPrompt(int dex,String name,boolean triple){
        String base=data.buildPokemonPrompt(dex,triple);
        StringBuilder instruction=new StringBuilder();
        instruction.append("SHINY REFERENCE IMAGE — MANDATORY COLOR AUTHORITY: The supplied reference image for this selected Shiny form is the authoritative source for all Shiny-specific colors, markings, glowing accents and their placement. Explicitly inspect that Shiny reference image and translate its palette and distribution to the Novaris humanoid design. Any normal-form palette or color statements in the Pokédex data or Style Core are overridden wherever they conflict with the Shiny reference. Preserve species anatomy, identity, silhouette and recognizable features; use the Style Core for composition, clothing and rendering only where compatible with the Shiny colors. Do not copy colors from the normal-form reference when the Shiny reference differs. Variant: ").append(name).append(".");
        if(dex==197){
            instruction.append(" UMBREON / NACHTARA SHINY — CRITICAL: The ring markings and matching luminous accents are BLUE, NOT YELLOW. Do not generate the normal yellow rings. Keep Umbreon's dark body and recognizable ring pattern, using the supplied Shiny reference image to match the exact blue hue and placement.");
        }
        instruction.append("\nFINAL COLOR VALIDATION: Before generating the image, compare each distinctive marking against the supplied Shiny reference image. If a normal-color feature conflicts, replace it with the Shiny-reference color. This instruction takes precedence over conflicting color descriptions anywhere earlier in the combined prompt.");
        return base+"\n\n"+instruction.toString();
    }
    
private void addOriginalVariants(int dex){
    try{
        java.io.InputStream in=getAssets().open("novaris_original_variants.json");
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
        byte[] buf=new byte[8192];int n;
        while((n=in.read(buf))!=-1)out.write(buf,0,n);
        in.close();
        JSONObject root=new JSONObject(out.toString("UTF-8"));
        JSONArray variants=root.getJSONObject("V2713_ASSIGNMENTS").optJSONArray(String.valueOf(dex));
        if(variants==null||variants.length()!=6)return;
        LinearLayout section=new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        Button toggle=button("Optionale Klassen & Varianten · 6  ▾");
        section.addView(toggle);
        LinearLayout contents=new LinearLayout(this);
        contents.setOrientation(LinearLayout.VERTICAL);
        contents.setVisibility(android.view.View.GONE);
        toggle.setOnClickListener(v->{boolean open=contents.getVisibility()!=android.view.View.VISIBLE;contents.setVisibility(open?android.view.View.VISIBLE:android.view.View.GONE);toggle.setText(open?"Optionale Klassen & Varianten · 6  ▴":"Optionale Klassen & Varianten · 6  ▾");});
        section.addView(contents);
        for(int i=0;i<6;i++){
            final String variant=variants.optString(i);
            final boolean lifestyle=i>=3;
            DatabaseManager.PokemonDetail detail=data.pokemonDetail(dex);
            if(detail==null)continue;
            String filename=String.format(java.util.Locale.ROOT,"#%04d.%s_%s",dex,OriginalVariantEngine.slug(detail.item.en),OriginalVariantEngine.slug(variant));
            LinearLayout card=new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(8),dp(10),dp(8),dp(14));
            TextView name=label(variant,16,true);
            card.addView(name);
            ImageView picture=image(dp(84),dp(84));
            card.addView(picture,new LinearLayout.LayoutParams(dp(84),dp(84)));
            ImageLoader.load(this,detail.item.art,picture);
            Button copyImage=button("Copy image");
            copyImage.setOnClickListener(v->ImageClipboard.copy(this,detail.item.art));
            card.addView(copyImage,new LinearLayout.LayoutParams(match(),wrap()));
            Button copyFilename=button(filename);
            copyFilename.setOnClickListener(v->copy(filename+DOWNLOAD_SUFFIX));
            card.addView(copyFilename,new LinearLayout.LayoutParams(match(),wrap()));
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            Button x1=button("Copy "+variant+" + Core + Dex x1");
            Button x3=button("Copy "+variant+" + Core + Dex x3");
            x1.setOnClickListener(v->copy(originalVariantPrompt(dex,variant,lifestyle,false,root)));
            x3.setOnClickListener(v->copyTriple(originalVariantPrompt(dex,variant,lifestyle,true,root)));
            row.addView(x1,new LinearLayout.LayoutParams(0,dp(66),1));
            row.addView(x3,new LinearLayout.LayoutParams(0,dp(66),1));
            card.addView(row);
            contents.addView(card);
        }
        body.addView(section);
    }catch(Exception e){android.util.Log.e("NovarisVariants","Cannot load original variants",e);}
}
private String originalVariantPrompt(int dex,String variant,boolean lifestyle,boolean triple,JSONObject root){
    try{
        return OriginalVariantEngine.build(this,root,dex,variant,triple);
    }catch(Exception e){
        android.util.Log.e("NovarisVariants","Original prompt failed",e);
        return "Variant prompt error: "+e.getMessage();
    }
}

    
private void addFormClassVariants(int dex,int formIndex,String formName,String formUrl,String blueprint,int insertIndex){
    try{
        java.io.InputStream in=getAssets().open("novaris_original_variants.json");
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
        byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
        in.close();
        JSONObject root=new JSONObject(out.toString("UTF-8"));
        JSONArray variants=root.getJSONObject("V2713_ASSIGNMENTS").optJSONArray(String.valueOf(dex));
        if(variants==null||variants.length()!=6)return;
        DatabaseManager.PokemonDetail detail=data.pokemonDetail(dex);
        if(detail==null)return;
        LinearLayout section=new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(dp(8),dp(8),dp(8),dp(8));
        Button toggle=button("Optionale Klassen & Varianten · 6  ▾");
        section.addView(toggle);
        LinearLayout contents=new LinearLayout(this);
        contents.setOrientation(LinearLayout.VERTICAL);
        contents.setVisibility(android.view.View.GONE);
        toggle.setOnClickListener(v->{
            boolean open=contents.getVisibility()!=android.view.View.VISIBLE;
            contents.setVisibility(open?android.view.View.VISIBLE:android.view.View.GONE);
            toggle.setText(open?"Optionale Klassen & Varianten · 6  ▴":"Optionale Klassen & Varianten · 6  ▾");
        });
        section.addView(contents);
        for(int i=0;i<6;i++){
            final String variant=variants.optString(i);
            final String title=String.format(java.util.Locale.ROOT,"#%04d.%s_%02d_%s_%s",dex,
                OriginalVariantEngine.slug(detail.item.en),formIndex,OriginalVariantEngine.slug(formName),OriginalVariantEngine.slug(variant));
            LinearLayout card=new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(8),dp(10),dp(8),dp(10));
            card.addView(label((i<3?"OPTIONALE KLASSE ":"LIFESTYLE ")+(i+1)+"/6",14,true));
            ImageView preview=image(dp(84),dp(84));
            card.addView(preview,new LinearLayout.LayoutParams(dp(84),dp(84)));
            ImageLoader.load(this,formUrl,preview);
            Button imageButton=button("Copy image");
            imageButton.setOnClickListener(v->ImageClipboard.copy(this,formUrl));
            card.addView(imageButton);
            Button fileButton=button(title);
            fileButton.setOnClickListener(v->copy(title+DOWNLOAD_SUFFIX));
            card.addView(fileButton);
            LinearLayout actions=new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button one=button(variant+" x1");
            Button three=button(variant+" x3");
            one.setOnClickListener(v->copy(formVariantPrompt(dex,formName,blueprint,variant,false,root)));
            three.setOnClickListener(v->copyTriple(formVariantPrompt(dex,formName,blueprint,variant,true,root)));
            actions.addView(one,new LinearLayout.LayoutParams(0,dp(62),1));
            actions.addView(three,new LinearLayout.LayoutParams(0,dp(62),1));
            card.addView(actions);
            contents.addView(card);
        }
        body.addView(section,Math.max(0,Math.min(insertIndex,body.getChildCount())));
    }catch(Exception e){android.util.Log.e("NovarisFormVariants","Cannot build form variants",e);}
}
private String formVariantPrompt(int dex,String formName,String blueprint,String variant,boolean triple,JSONObject root){
    try{
        String original=OriginalVariantEngine.build(this,root,dex,variant,triple);
        String formContext="MANDATORY FORM IDENTITY: "+formName+
            ". Use the exact selected form reference image, not the base species. "+
            "FORM BLUEPRINT: "+blueprint+
            ". The form's source colors, silhouette and details override the normal form where different. "+
            "Keep the selected optional class or lifestyle role unchanged.";
        return formContext+"\n\n"+original;
    }catch(Exception e){return "Variant prompt error: "+e.getMessage();}
}

    private void copyTriple(String s){ copy(PromptSafety.enforce(s,true)); }
    
    private String editedJob(int dex,String fallback){
        return getSharedPreferences("novaris_job_edits",MODE_PRIVATE).getString(String.valueOf(dex),fallback==null?"":fallback);
    }
    private void addJobEditor(int dex,String current){
        Button edit=button("Job-Zuordnung auswählen / Export");
        body.addView(edit);
        edit.setOnClickListener(v->openJobCatalog(dex,current));
    }
    private void openJobCatalog(int dex,String current){
        try{
            java.io.InputStream input=getAssets().open("novaris_job_catalog_v2742.json");
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
            byte[] buffer=new byte[8192];int count;
            while((count=input.read(buffer))!=-1)bytes.write(buffer,0,count);
            input.close();
            org.json.JSONObject catalog=new org.json.JSONObject(bytes.toString("UTF-8"));
            String[] systems={"Ragnarok Online","Final Fantasy XIV","Civilian","Gatherer","Crafter"};
            new AlertDialog.Builder(this).setTitle("Grundklasse / Berufsgruppe")
              .setItems(systems,(dialog,system)->{
                  if(system==0||system==2)showJobFamilies(dex,current,catalog,system);
                  else showFlatJobCatalog(dex,catalog,system);
              })
              .setNeutralButton("Import / Export",(dialog,which)->new AlertDialog.Builder(this)
                  .setItems(new String[]{"JSON importieren","JSON exportieren"},(d2,choice)->{
                      if(choice==0)importJobEdits();else exportJobEdits();
                  }).show())
              .setNegativeButton("Abbrechen",null).show();
        }catch(Exception e){Toast.makeText(this,"Jobkatalog nicht verfügbar: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void showFlatJobCatalog(int dex,org.json.JSONObject catalog,int system){
        try{
            String key=system==1?"V2622_FFXIV":system==3?"V2610_GATHER":"V2610_CRAFT";
            org.json.JSONObject jobs=catalog.getJSONObject(key);
            java.util.ArrayList<String> names=new java.util.ArrayList<>();
            java.util.Iterator<String> it=jobs.keys();
            while(it.hasNext())names.add(it.next());
            java.util.Collections.sort(names);
            new AlertDialog.Builder(this).setTitle("Klasse / Beruf")
              .setItems(names.toArray(new String[0]),(dialog,index)->saveStructuredJob(dex,names.get(index),system==1?"Final Fantasy XIV":system==3?"Gatherer":"Crafter","", "", ""))
              .setNegativeButton("Abbrechen",null).show();
        }catch(Exception e){Toast.makeText(this,"Jobauswahl fehlgeschlagen: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void saveStructuredJob(int dex,String selected,String group,String family,String tier,String specialization){
        try{
            org.json.JSONObject record=new org.json.JSONObject();
            record.put("label",selected);
            record.put("role",selected);
            record.put("group",group);
            record.put("class_family",family);
            record.put("tier",tier);
            record.put("specialization",specialization);
            String prefix="Ragnarok Online".equals(group)?"ro:":"Civilian".equals(group)?"civil:":"Final Fantasy XIV".equals(group)?"ffxiv:":"Gatherer".equals(group)?"gather:":"craft:";
            record.put("id",prefix+("Civilian".equals(group)?family:selected));
            record.put("root_key","Ragnarok Online".equals(group)?"ro-root:"+family.split(" → ")[0]:"group:"+("Final Fantasy XIV".equals(group)?"FFXIV":group));
            boolean structuredSaved=getSharedPreferences("novaris_job_structured",MODE_PRIVATE).edit().putString(String.valueOf(dex),record.toString()).commit();
            if(!structuredSaved)throw new java.io.IOException("Strukturierte Jobdaten nicht gespeichert");
            saveSelectedJob(dex,selected);
        }catch(Exception e){Toast.makeText(this,"Job nicht gespeichert: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void saveSelectedJob(int dex,String selected){
        if(!getSharedPreferences("novaris_job_edits",MODE_PRIVATE).edit().putString(String.valueOf(dex),selected).commit()){
            Toast.makeText(this,"Job konnte nicht gespeichert werden",Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this,"Job lokal gespeichert: "+selected,Toast.LENGTH_SHORT).show();
        body.removeAllViews();showPokemon(dex);
    }
    private void showJobFamilies(int dex,String current,org.json.JSONObject catalog,int system){
        try{
            org.json.JSONObject families=catalog.getJSONObject(system==0?"V2610_RAG":"V2610_CIV");
            java.util.ArrayList<String> names=new java.util.ArrayList<>();
            java.util.Iterator<String> it=families.keys();
            while(it.hasNext())names.add(it.next());
            java.util.Collections.sort(names);
            if(system==0){
                java.util.ArrayList<String> roots=new java.util.ArrayList<>();
                for(String family:names){
                    org.json.JSONObject item=families.optJSONObject(family);
                    String root=item==null?"":item.optString("root","");
                    if(!root.isEmpty()&&!roots.contains(root))roots.add(root);
                }
                java.util.Collections.sort(roots);
                new AlertDialog.Builder(this).setTitle("Ragnarok Grundklasse wählen")
                  .setItems(roots.toArray(new String[0]),(dialog,index)->{
                      java.util.ArrayList<String> branches=new java.util.ArrayList<>();
                      for(String family:names){
                          org.json.JSONObject item=families.optJSONObject(family);
                          if(item!=null&&roots.get(index).equals(item.optString("root","")))branches.add(family);
                      }
                      new AlertDialog.Builder(this).setTitle("Klassenlaufbahn wählen")
                        .setItems(branches.toArray(new String[0]),(d2,i)->showJobStages(dex,current,families,branches.get(i),system))
                        .setNegativeButton("Abbrechen",null).show();
                  }).setNegativeButton("Abbrechen",null).show();
            }else{
                new AlertDialog.Builder(this).setTitle("Berufsfamilie auswählen")
                  .setItems(names.toArray(new String[0]),(dialog,index)->showJobStages(dex,current,families,names.get(index),system))
                  .setNegativeButton("Abbrechen",null).show();
            }
        }catch(Exception e){Toast.makeText(this,"Familienauswahl fehlgeschlagen",Toast.LENGTH_LONG).show();}
    }
    private void showJobStages(int dex,String current,org.json.JSONObject families,String family,int system){
        try{
            org.json.JSONObject entry=families.optJSONObject(family);
            java.util.ArrayList<String> stages=new java.util.ArrayList<>();
            if(system==2 && entry!=null){
                org.json.JSONArray a=entry.optJSONArray("specializations");
                if(a!=null)for(int i=0;i<a.length();i++)stages.add("C"+(i+1)+" · "+a.optString(i));
            }else if(entry!=null){
                org.json.JSONObject tierMap=entry.optJSONObject("tier_map");
                if(tierMap!=null){
                    String[] tiers={"T1","T2","Transcendent","T3","T4"};
                    for(String tier:tiers){
                        String name=tierMap.optString(tier,"");
                        if(!name.isEmpty())stages.add(name);
                    }
                }
            }
            if(stages.isEmpty())stages.add(family);
            new AlertDialog.Builder(this).setTitle(family+" – Stufe auswählen")
              .setItems(stages.toArray(new String[0]),(dialog,index)->{
                  String selected=stages.get(index);
                  if(system==2&&selected.startsWith("C")){
                      int separator=selected.indexOf(" · ");
                      if(separator>=0)selected=selected.substring(separator+3);
                  }
                  String tier=system==2?"C"+(index+1):"";
                  if(system==0&&entry!=null){
                      org.json.JSONObject tierMap=entry.optJSONObject("tier_map");
                      if(tierMap!=null){
                          java.util.Iterator<String> keys=tierMap.keys();
                          while(keys.hasNext()){String key=keys.next();if(selected.equals(tierMap.optString(key)))tier=key;}
                      }
                  }
                  saveStructuredJob(dex,selected,system==0?"Ragnarok Online":"Civilian",family,tier,system==2?selected:"");
              }).setNegativeButton("Abbrechen",null).show();
        }catch(Exception e){Toast.makeText(this,"Stufenauswahl fehlgeschlagen",Toast.LENGTH_LONG).show();}
    }
    private org.json.JSONObject loadJobBaseline()throws Exception{
        java.io.InputStream input=getAssets().open("novaris_job_baseline_v2742.json");
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        byte[] buffer=new byte[8192];int count;
        while((count=input.read(buffer))!=-1)bytes.write(buffer,0,count);
        input.close();
        org.json.JSONArray rows=new org.json.JSONArray(bytes.toString("UTF-8"));
        org.json.JSONObject byDex=new org.json.JSONObject();
        for(int i=0;i<rows.length();i++){
            org.json.JSONObject row=rows.getJSONObject(i);
            byDex.put(String.valueOf(row.getInt("dex")),row);
        }
        return byDex;
    }
    private void importJobEdits(){
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent,9913);
    }
    private void readJobEdits(android.net.Uri uri)throws Exception{
        java.io.InputStream input=getContentResolver().openInputStream(uri);
        if(input==null)throw new java.io.IOException("Datei nicht lesbar");
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        byte[] buffer=new byte[8192];int n;
        try(java.io.InputStream stream=input){
            while((n=stream.read(buffer))!=-1){
                if(bytes.size()+n>4000000)throw new java.io.IOException("Datei zu groß");
                bytes.write(buffer,0,n);
            }
        }
        org.json.JSONObject root=new org.json.JSONObject(bytes.toString("UTF-8"));
        if(!"NOVARIS_JOB_ASSIGNMENTS_v16".equals(root.optString("format")))
            throw new org.json.JSONException("Unbekanntes Zuordnungsformat");
        org.json.JSONArray assignments=root.getJSONArray("assignments");
        if(root.has("assignmentCount")&&root.getInt("assignmentCount")!=assignments.length())
            throw new org.json.JSONException("Zuordnungsanzahl stimmt nicht überein");
        android.content.SharedPreferences.Editor editor=getSharedPreferences("novaris_job_edits",MODE_PRIVATE).edit();
        java.util.HashMap<String,String> validated=new java.util.HashMap<>();
        java.util.HashMap<String,String> structuredValidated=new java.util.HashMap<>();
        org.json.JSONObject baseline=loadJobBaseline();
        int imported=0;
        for(int i=0;i<assignments.length();i++){
            org.json.JSONObject item=assignments.getJSONObject(i);
            int dex=item.getInt("dex");
            if(dex<1||dex>1025)throw new org.json.JSONException("Ungültige Dex-ID");
            if(!baseline.has(String.valueOf(dex)))throw new org.json.JSONException("Dex-ID nicht im Originalkatalog: "+dex);
            if(validated.containsKey(String.valueOf(dex)))throw new org.json.JSONException("Doppelte Dex-ID: "+dex);
            org.json.JSONObject selected=item.getJSONObject("selected");
            String role=selected.optString("role",selected.optString("label","")).trim();
            if(role.isEmpty())throw new org.json.JSONException("Leere Jobzuordnung für Dex "+dex);
            String label=selected.optString("label","").trim();
            if(!label.isEmpty()&&!label.equals(role))throw new org.json.JSONException("Widersprüchliche Jobbezeichnung für Dex "+dex);
            if(role.length()>250)throw new org.json.JSONException("Jobbezeichnung zu lang für Dex "+dex);
            validated.put(String.valueOf(dex),role);
            structuredValidated.put(String.valueOf(dex),selected.toString());
            imported++;
        }
        android.content.SharedPreferences flatPrefs=getSharedPreferences("novaris_job_edits",MODE_PRIVATE);
        android.content.SharedPreferences structuredPrefs=getSharedPreferences("novaris_job_structured",MODE_PRIVATE);
        java.util.Map<String,?> oldStructured=structuredPrefs.getAll();
        android.content.SharedPreferences.Editor structuredEditor=structuredPrefs.edit();
        for(java.util.Map.Entry<String,String> entry:structuredValidated.entrySet())
            structuredEditor.putString(entry.getKey(),entry.getValue());
        if(!structuredEditor.commit())throw new java.io.IOException("Strukturierte Daten konnten nicht gespeichert werden");
        for(java.util.Map.Entry<String,String> entry:validated.entrySet())
            editor.putString(entry.getKey(),entry.getValue());
        if(!editor.commit()){
            android.content.SharedPreferences.Editor rollback=structuredPrefs.edit().clear();
            for(java.util.Map.Entry<String,?> entry:oldStructured.entrySet())
                if(entry.getValue() instanceof String)rollback.putString(entry.getKey(),(String)entry.getValue());
            rollback.commit();
            throw new java.io.IOException("Lokales Speichern fehlgeschlagen; strukturierte Daten zurückgesetzt");
        }
        Toast.makeText(this,imported+" Job-Zuordnungen importiert",Toast.LENGTH_LONG).show();
        recreate();
    }
    private void exportJobEdits(){
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(android.content.Intent.EXTRA_TITLE,"novaris_job_aenderungen.json");
        startActivityForResult(intent,9912);
    }
    @Override protected void onActivityResult(int request,int result,android.content.Intent dataIntent){
        super.onActivityResult(request,result,dataIntent);
        if(result!=RESULT_OK||dataIntent==null||dataIntent.getData()==null)return;
        if(request==9913){
            try{readJobEdits(dataIntent.getData());}
            catch(Exception e){Toast.makeText(this,"Import fehlgeschlagen: "+e.getMessage(),Toast.LENGTH_LONG).show();}
            return;
        }
        if(request!=9912)return;
        try{
            org.json.JSONObject root=new org.json.JSONObject();
            root.put("format","NOVARIS_JOB_ASSIGNMENTS_v16");
            root.put("pokedexVersion","V27.0");
            org.json.JSONArray assignments=new org.json.JSONArray();
            org.json.JSONObject baseline=loadJobBaseline();
            java.util.Map<String,?> edits=getSharedPreferences("novaris_job_edits",MODE_PRIVATE).getAll();
            java.util.ArrayList<String> sortedDexes=new java.util.ArrayList<>(edits.keySet());
            for(String key:sortedDexes){
                int number=Integer.parseInt(key);
                if(number<1||number>1025||!baseline.has(key))throw new org.json.JSONException("Ungültige lokale Dex-ID: "+key);
            }
            java.util.Collections.sort(sortedDexes,(a,b)->Integer.compare(Integer.parseInt(a),Integer.parseInt(b)));
            for(String dexKey:sortedDexes){
                Object editedRole=edits.get(dexKey);
                org.json.JSONObject original=baseline.optJSONObject(dexKey);
                org.json.JSONObject item=new org.json.JSONObject();
                item.put("dex",Integer.parseInt(dexKey));
                item.put("original",original==null?new org.json.JSONObject():original);
                org.json.JSONObject selected=new org.json.JSONObject();
                String structured=getSharedPreferences("novaris_job_structured",MODE_PRIVATE).getString(dexKey,null);
                if(structured!=null)selected=new org.json.JSONObject(structured);
                selected.put("role",String.valueOf(editedRole));
                selected.put("label",String.valueOf(editedRole));
                item.put("selected",selected);
                assignments.put(item);
            }
            root.put("assignments",assignments);
            root.put("assignmentCount",assignments.length());
            try(java.io.OutputStream output=getContentResolver().openOutputStream(dataIntent.getData())){
                output.write(root.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            Toast.makeText(this,"Job-Änderungen exportiert",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"Export fehlgeschlagen: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private String applyEditedJob(int dex,String prompt,String original){
        String edited=editedJob(dex,original);
        if(prompt==null||original==null||original.isEmpty()||edited.equals(original))return prompt;
        return prompt.replace(original,edited);
    }
    
private void copy(String s){
        if(s!=null && !s.startsWith("NOVARIS OUTPUT CONTROL") && (s.contains("SPECIES IDENTITY:") || s.contains("NOVARIS OPTIONAL CLASS") || s.contains("NOVARIS LIFESTYLE VARIANT"))) s=PromptSafety.enforce(s,false);android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("Novaris",s==null?"":s));Toast.makeText(this,"Kopiert",Toast.LENGTH_SHORT).show();}
    private void title(String s){TextView t=label(s,24,true);body.addView(t);}private void small(String s){TextView t=label(s,13,false);t.setTextColor(Color.DKGRAY);body.addView(t);}private TextView label(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s==null?"":s);t.setTextSize(size);t.setTextColor(Color.rgb(23,32,51));if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);t.setPadding(dp(4),dp(4),dp(4),dp(4));return t;}private ImageView image(int w,int h){ImageView i=new ImageView(this);i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);return i;}
    private String join(String sep,String... xs){StringBuilder b=new StringBuilder();for(String x:xs)if(x!=null&&!x.trim().isEmpty()){if(b.length()>0)b.append(sep);b.append(x.trim());}return b.toString();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}private int match(){return ViewGroup.LayoutParams.MATCH_PARENT;}private int wrap(){return ViewGroup.LayoutParams.WRAP_CONTENT;}
    @Override protected void onDestroy(){if(data!=null)data.close();super.onDestroy();}
}
