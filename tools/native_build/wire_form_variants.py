#!/usr/bin/env python3
"""Native source transformation previously embedded in the APK build workflow."""
from pathlib import Path
p=Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/DetailActivity.java")
s=p.read_text()
anchor='addFormCards("Weitere Formen",d.item,d.formsJson);'
if anchor not in s:raise SystemExit("Additional form insertion point missing")
s=s.replace(anchor,anchor,1)
hook='addFormCards("Weitere Formen",d.item,d.formsJson);'
if hook not in s:raise SystemExit("Form list insertion point missing")
wiring='\n        try{\n            JSONArray forms=new JSONArray(d.formsJson);\n            for(int fi=0;fi<forms.length();fi++){\n                JSONObject form=forms.optJSONObject(fi);\n                if(form!=null) addFormClassVariants(d.item.dex,fi+1,form.optString("name","Form"),form.optString("url",""),form.optString("blueprint",""),body.getChildCount());\n            }\n        }catch(Exception e){android.util.Log.e("NovarisFormVariants","Cannot render form variants",e);}\n        '
s=s.replace(hook,hook+wiring,1)
method=r"""
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
"""
insertion='private void copy(String s){'
if insertion not in s:raise SystemExit("Copy method insertion point missing")
s=s.replace(insertion,method+'\n    '+insertion,1)
p.write_text(s)
assert 'addFormClassVariants(d.item.dex,fi+1' in s
assert 'body.getChildCount()-forms.length()+fi+1' not in s, "Form variants may be inserted at negative index"
assert 'Math.max(0,Math.min(insertIndex,body.getChildCount()))' in s
assert 'formVariantPrompt(dex,formName,blueprint,variant,true,root)' in s
assert 'three.setOnClickListener(v->copyTriple(formVariantPrompt' in s, "Form x3 must use triple-aware copy"
print("Restored six per-form variants with independent image and prompt buttons")
