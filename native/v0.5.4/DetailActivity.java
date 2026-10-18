package com.novaris.pokedex.nativeapp;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import org.json.*;

import java.util.*;

public class DetailActivity extends Activity {
    private DatabaseManager data;
    private LinearLayout body;
    private ScrollView scroll;
    private int[] filteredDexes = new int[0];
    private int currentDex;
    private float downX, downY;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        try{data=new DatabaseManager(this);}catch(Exception e){finish();return;}
        filteredDexes=getIntent().getIntArrayExtra("filtered_dexes");
        if(filteredDexes==null)filteredDexes=new int[0];
        currentDex=getIntent().getIntExtra("dex",0);

        scroll=new ScrollView(this);
        body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14),dp(12),dp(14),dp(24));
        body.setBackgroundColor(Color.rgb(244,246,250));
        scroll.addView(body,new ScrollView.LayoutParams(match(),wrap()));
        scroll.setOnTouchListener((v,event)->{
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN: downX=event.getX();downY=event.getY();return false;
                case MotionEvent.ACTION_UP:
                    float dx=event.getX()-downX,dy=event.getY()-downY;
                    if(Math.abs(dx)>dp(90)&&Math.abs(dx)>Math.abs(dy)*1.25f&&filteredDexes.length>1){
                        if(dx<0)showRelative(1); else showRelative(-1);
                        return true;
                    }
            }
            return false;
        });
        setContentView(scroll);
        String type=getIntent().getStringExtra("type");
        if("character".equals(type))showCharacter(getIntent().getStringExtra("id"));
        else showPokemon(currentDex);
    }

    private void showRelative(int delta){
        int idx=-1;
        for(int i=0;i<filteredDexes.length;i++)if(filteredDexes[i]==currentDex){idx=i;break;}
        if(idx<0)return;
        int next=idx+delta;
        if(next<0||next>=filteredDexes.length){
            Toast.makeText(this,delta<0?"Bereits am Anfang":"Bereits am Ende",Toast.LENGTH_SHORT).show();
            return;
        }
        currentDex=filteredDexes[next];
        body.removeAllViews();
        scroll.scrollTo(0,0);
        showPokemon(currentDex);
    }

    private void showPokemon(int dex){
        currentDex=dex;
        DatabaseManager.PokemonDetail d=data.pokemonDetail(dex);
        if(d==null){finish();return;}
        title(d.item.label+(d.item.de==null||d.item.de.isEmpty()?"":" · "+d.item.de));
        if(d.item.changed==1){
            TextView badge=label("NEU ERSTELLEN",14,true);
            badge.setTextColor(Color.rgb(20,120,70));
            badge.setGravity(Gravity.END);
            body.addView(badge);
        }

        ImageView art=image(dp(260),dp(260));
        body.addView(art,new LinearLayout.LayoutParams(match(),dp(270)));
        ImageLoader.load(this,d.item.art,art);

        LinearLayout imgActions=new LinearLayout(this);
        imgActions.setOrientation(LinearLayout.HORIZONTAL);
        Button copyImage=button("Copy image");
        copyImage.setOnClickListener(v->ImageClipboard.copy(this,d.item.art));
        imgActions.addView(copyImage,new LinearLayout.LayoutParams(0,wrap(),1));
        if(d.item.role!=null&&!d.item.role.isEmpty()){
            Button role=button(d.item.role);
            role.setEnabled(false);
            imgActions.addView(role,new LinearLayout.LayoutParams(0,wrap(),1));
        }
        body.addView(imgActions);

        Button filename=button(mainFilename(d.item));
        filename.setOnClickListener(v->copy(filename.getText().toString()));
        body.addView(filename,new LinearLayout.LayoutParams(match(),wrap()));

        LinearLayout promptRow=new LinearLayout(this);
        promptRow.setOrientation(LinearLayout.HORIZONTAL);
        Button x1=button("Copy Core + Dex x1");
        Button x3=button("Copy Core + Dex x3");
        x1.setOnClickListener(v->copy(headedPokemonPrompt(d.item.dex,"",data.buildPokemonPrompt(d.item.dex,false))));
        x3.setOnClickListener(v->copy(headedPokemonPrompt(d.item.dex,"",data.buildPokemonPrompt(d.item.dex,true))));
        promptRow.addView(x1,new LinearLayout.LayoutParams(0,dp(62),1));
        promptRow.addView(x3,new LinearLayout.LayoutParams(0,dp(62),1));
        body.addView(promptRow);

        small("Generation "+d.item.generation+
                (d.item.role==null||d.item.role.isEmpty()?"":" · "+d.item.role)+
                (d.item.tier==null||d.item.tier.isEmpty()?"":" · "+d.item.tier));
        if(d.item.family!=null&&!d.item.family.isEmpty())section("Klassenfamilie",d.item.family);
        addFormCards("Weitere Formen",d.item.dex,d.formsJson,d.item.role);
        addShinyCards("Shiny-Versionen",d.item.dex,d.shinyJson,d.item.role);

        try{
            JSONArray arr=new JSONArray(d.cellsJson);
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.optJSONObject(i);
                if(o==null)continue;
                String h=o.optString("heading","Abschnitt");
                String t=o.optString("text","");
                if(t.trim().isEmpty())continue;
                if(h.equals("Pokémon Artwork")||h.equals("Weitere Formen")||h.equals("Shiny-Versionen"))continue;
                section(h,t);
            }
        }catch(Exception e){section("Daten",d.cellsJson);}
    }

    private void addFormCards(String heading,int dex,String json,String role){
        try{
            JSONArray a=new JSONArray(json);
            if(a.length()==0)return;
            TextView h=label(heading,17,true);
            h.setPadding(dp(4),dp(16),dp(4),dp(7));
            body.addView(h);
            for(int i=0;i<a.length();i++){
                JSONObject o=a.optJSONObject(i);
                if(o==null)continue;
                String name=o.optString("name","Form");
                String url=o.optString("url","");
                String blueprint=o.optString("blueprint","");
                addImagePromptCard(dex,i+1,name,url,blueprint,role,false);
            }
        }catch(Exception ignored){}
    }

    private void addShinyCards(String heading,int dex,String json,String role){
        try{
            JSONArray a=new JSONArray(json);
            if(a.length()==0)return;
            TextView h=label(heading,17,true);
            h.setPadding(dp(4),dp(16),dp(4),dp(7));
            body.addView(h);
            TextView note=label("Shiny-Farbpalette: Referenzbild ist Farbauthorität; normale Farben werden nicht übernommen, wenn das Shiny-Referenzbild abweicht.",13,false);
            note.setPadding(dp(4),0,dp(4),dp(8));
            body.addView(note);
            for(int i=0;i<a.length();i++){
                JSONObject o=a.optJSONObject(i);
                if(o==null)continue;
                String name=o.optString("name","Shiny");
                String url=o.optString("url","");
                addImagePromptCard(dex,i+1,name,url,"",role,true);
            }
        }catch(Exception ignored){}
    }

    private void addImagePromptCard(int dex,int index,String name,String url,String blueprint,String role,boolean shiny){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8),dp(8),dp(8),dp(8));
        card.setBackgroundColor(Color.WHITE);

        TextView n=label(name,15,true);
        card.addView(n);

        ImageView im=image(dp(260),dp(260));
        card.addView(im,new LinearLayout.LayoutParams(match(),dp(270)));
        ImageLoader.load(this,url,im);

        Button ci=button("Copy image");
        ci.setOnClickListener(v->ImageClipboard.copy(this,url));
        card.addView(ci,new LinearLayout.LayoutParams(match(),wrap()));

        String file=shiny?shinyFilename(dex,index,name,role):formFilename(dex,index,name,role);
        Button fn=button(file);
        fn.setOnClickListener(v->copy(file));
        card.addView(fn,new LinearLayout.LayoutParams(match(),wrap()));

        LinearLayout pr=new LinearLayout(this);
        pr.setOrientation(LinearLayout.HORIZONTAL);
        Button b1=button("Copy "+name+(shiny?" Shiny":"")+" + Core + Dex x1");
        Button b3=button("Copy "+name+(shiny?" Shiny":"")+" + Core + Dex x3");
        b1.setOnClickListener(v->copy(headedPokemonPrompt(dex,name,shiny?buildShinyPrompt(dex,name,false):data.buildPokemonPrompt(dex,false,name,blueprint))));
        b3.setOnClickListener(v->copy(headedPokemonPrompt(dex,name,shiny?buildShinyPrompt(dex,name,true):data.buildPokemonPrompt(dex,true,name,blueprint))));
        pr.addView(b1,new LinearLayout.LayoutParams(0,dp(66),1));
        pr.addView(b3,new LinearLayout.LayoutParams(0,dp(66),1));
        card.addView(pr);

        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(match(),wrap());
        lp.setMargins(0,dp(6),0,dp(8));
        body.addView(card,lp);
    }

    private String headedPokemonPrompt(int dex,String variant,String prompt){
        DatabaseManager.PokemonDetail detail=data.pokemonDetail(dex);
        if(detail==null)return prompt;
        DatabaseManager.PokemonItem item=detail.item;
        String heading=String.format(Locale.ROOT,"#%04d.%s",dex,item.en);
        if(item.de!=null&&!item.de.trim().isEmpty()&&!item.de.equalsIgnoreCase(item.en))
            heading+=" / "+item.de;
        if(variant!=null&&!variant.trim().isEmpty())heading+=" — "+variant.trim();
        String content=prompt==null?"":prompt.trim();
        if(content.startsWith(heading))return content;
        return heading+"\n\n"+content;
    }

    private String mainFilename(DatabaseManager.PokemonItem p){
        return String.format(Locale.ROOT,"#%04d.%s_00_Main%s%s",p.dex,p.en,safeRole(p.role),
                p.de==null||p.de.isEmpty()||p.de.equalsIgnoreCase(p.en)?"":" / "+p.de);
    }

    private String formFilename(int dex,int index,String name,String role){
        DatabaseManager.PokemonDetail d=data.pokemonDetail(dex);
        String en=d==null?"Pokemon":d.item.en;
        return String.format(Locale.ROOT,"#%04d.%s_%02d_%s%s",dex,en,index,
                name.replaceAll("[^A-Za-z0-9_-]+","_"),safeRole(role));
    }

    private String shinyFilename(int dex,int index,String name,String role){
        DatabaseManager.PokemonDetail d=data.pokemonDetail(dex);
        String en=d==null?"Pokemon":d.item.en;
        return String.format(Locale.ROOT,"#%04d.%s_%02d_%s_Shiny%s",dex,en,index,
                name.replaceAll("[^A-Za-z0-9_-]+","_"),safeRole(role));
    }

    private String safeRole(String role){
        return role==null||role.trim().isEmpty()?"":"_"+role.trim().replaceAll("[^A-Za-z0-9_-]+","_");
    }

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

    private void showCharacter(String id){
        DatabaseManager.CharacterDetail d=data.characterDetail(id);
        if(d==null){finish();return;}
        title(d.item.name);
        ImageView art=image(dp(220),dp(260));
        body.addView(art,new LinearLayout.LayoutParams(match(),dp(270)));
        ImageLoader.load(this,d.item.portrait,art);
        Button copyImage=button("Copy image");
        copyImage.setOnClickListener(v->ImageClipboard.copy(this,d.item.portrait));
        body.addView(copyImage,new LinearLayout.LayoutParams(match(),wrap()));
        small(join(" · ",d.item.category,d.kind,d.version));
        section("Quelle / Serie",join("\n",d.item.source,d.item.aliases));
        if(d.canon!=null&&!d.canon.isEmpty())section("Kanon / Quelle",d.canon);
        if(d.novaris!=null&&!d.novaris.isEmpty())section("Novaris Interpretation",d.novaris);
        try{
            JSONArray a=new JSONArray(d.scenesJson);
            for(int i=0;i<a.length();i++){
                JSONObject s=a.optJSONObject(i);
                if(s==null)continue;
                String name=s.optString("title","Szene "+(i+1));
                String text=s.optString("prompt",s.toString(2));
                section("Szene "+(i+1)+" · "+name,text);
            }
        }catch(Exception e){if(d.scenesJson!=null)section("Szenen",d.scenesJson);}
    }

    private void section(String heading,String text){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10),dp(9),dp(10),dp(9));
        card.setBackgroundColor(Color.WHITE);
        TextView h=label(heading,16,true);
        card.addView(h);
        TextView t=label(text,14,false);
        t.setTextIsSelectable(true);
        t.setPadding(0,dp(5),0,dp(5));
        card.addView(t);
        Button copy=button("Kopieren");
        copy.setOnClickListener(v->copy(text));
        card.addView(copy,new LinearLayout.LayoutParams(wrap(),wrap()));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(match(),wrap());
        lp.setMargins(0,dp(7),0,dp(7));
        body.addView(card,lp);
    }

    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private void copy(String s){
        android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Novaris",s==null?"":s));
        Toast.makeText(this,"Kopiert",Toast.LENGTH_SHORT).show();
    }
    private void title(String s){TextView t=label(s,24,true);body.addView(t);}
    private void small(String s){TextView t=label(s,13,false);t.setTextColor(Color.DKGRAY);body.addView(t);}
    private TextView label(String s,int size,boolean bold){
        TextView t=new TextView(this);
        t.setText(s==null?"":s);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(23,32,51));
        if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);
        t.setPadding(dp(4),dp(4),dp(4),dp(4));
        return t;
    }
    private ImageView image(int w,int h){ImageView i=new ImageView(this);i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);return i;}
    private String join(String sep,String... xs){
        StringBuilder b=new StringBuilder();
        for(String x:xs)if(x!=null&&!x.trim().isEmpty()){if(b.length()>0)b.append(sep);b.append(x.trim());}
        return b.toString();
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private int match(){return ViewGroup.LayoutParams.MATCH_PARENT;}
    private int wrap(){return ViewGroup.LayoutParams.WRAP_CONTENT;}
    @Override protected void onDestroy(){if(data!=null)data.close();super.onDestroy();}
}
