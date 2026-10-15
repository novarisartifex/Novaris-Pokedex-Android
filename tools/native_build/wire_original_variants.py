#!/usr/bin/env python3
"""Native source transformation previously embedded in the APK build workflow."""
from pathlib import Path
p=Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/DetailActivity.java")
s=p.read_text()
needle='private void showPokemon(int dex){'
if needle not in s: raise SystemExit("Pokemon detail anchor absent")
anchor='addFormCards("Weitere Formen",d.item,d.formsJson);'
if anchor not in s:raise SystemExit("Main form variant insertion anchor missing")
s=s.replace(anchor,'addOriginalVariants(dex);\n        '+anchor,1)
method=r"""
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
"""
anchor='private void copy(String s){'
if anchor not in s:raise SystemExit("Variant method anchor missing")
s=s.replace(anchor,method+'\n    '+anchor,1)
p.write_text(s)
assert s.index('addOriginalVariants(dex);') < s.index('addFormCards("Weitere Formen",d.item,d.formsJson);')
assert 'ImageClipboard.copy(this,detail.item.art)' in s
assert 'originalVariantPrompt(dex,variant,lifestyle,true,root)' in s
assert 'x3.setOnClickListener(v->copyTriple(originalVariantPrompt' in s, "Base x3 must use triple-aware copy"
