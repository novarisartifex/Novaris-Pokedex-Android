from pathlib import Path
p=Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/DetailActivity.java")
s=p.read_text()
import re
# The reconstructed Android source uses addShinyCards(item, formsJson, shinyJson),
# not the older addImagePromptCard(...) signature.
start=s.find('private void addShinyCards(')
if start<0: raise SystemExit("Shiny card method missing")
# Match braces, not the next "private" token: lambdas and nested blocks may
# contain that keyword without ending addShinyCards.
brace=s.find('{',start)
if brace<0: raise SystemExit("Shiny card opening brace missing")
depth=0
end=-1
for pos in range(brace,len(s)):
    if s[pos]=='{': depth+=1
    elif s[pos]=='}':
        depth-=1
        if depth==0:
            end=pos+1
            break
if end<0: raise SystemExit("Shiny card method closing brace missing")
# Guard against inserting a second lifestyle section if a source snapshot already has it.
if 'addShinyLifestyleVariants(item.dex,' in s:
    raise SystemExit("Shiny lifestyle wiring already present; refusing duplicate insertion")
block=s[start:end]
index_match=re.search(r'for\s*\(\s*int\s+(\w+)\s*=\s*0\s*;',block)
if not index_match: raise SystemExit("Shiny card index loop missing")
idx=index_match.group(1)
add_call='addShinyLifestyleVariants(item.dex,'+idx+'+1,name,url);'
# Render the lifestyle section immediately after its Shiny card.
card_match=re.search(r'body\.addView\(card(?:,[^;]*)?\);',block)
if not card_match: raise SystemExit("Shiny card body insertion point missing")
anchor=card_match.group(0)
if block.count(anchor)!=1: raise SystemExit("Shiny card body insertion point ambiguous")
block=block.replace(anchor,anchor+'\n                '+add_call,1)
s=s[:start]+block+s[end:]
method=r'''
    private void addShinyLifestyleVariants(int dex,int shinyIndex,String shinyName,String shinyUrl){
        try{
            DatabaseManager.PokemonDetail detail=data.pokemonDetail(dex);
            if(detail==null)return;
            java.io.InputStream in=getAssets().open("novaris_original_variants.json");
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1)out.write(buf,0,n);
            in.close();
            JSONObject root=new JSONObject(out.toString("UTF-8"));
            String[] variants={"Casual","Nightlife","Lingerie"};
            LinearLayout section=new LinearLayout(this);
            section.setOrientation(LinearLayout.VERTICAL);
            Button toggle=button("Shiny Lifestyle · 3  ▾");
            section.addView(toggle);
            LinearLayout contents=new LinearLayout(this);
            contents.setOrientation(LinearLayout.VERTICAL);
            contents.setVisibility(android.view.View.GONE);
            toggle.setOnClickListener(v->{
                boolean open=contents.getVisibility()!=android.view.View.VISIBLE;
                contents.setVisibility(open?android.view.View.VISIBLE:android.view.View.GONE);
                toggle.setText(open?"Shiny Lifestyle · 3  ▴":"Shiny Lifestyle · 3  ▾");
            });
            section.addView(contents);
            for(String variant:variants){
                String filename=String.format(java.util.Locale.ROOT,"#%04d.%s_%02d_%s_Shiny_%s",dex,
                    OriginalVariantEngine.slug(detail.item.en),shinyIndex,
                    OriginalVariantEngine.slug(shinyName).replaceAll("(?i)(?:_?Shiny)+$", ""),OriginalVariantEngine.slug(variant));
                LinearLayout card=new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(8),dp(10),dp(8),dp(12));
                card.addView(label(variant,16,true));
                ImageView preview=image(dp(84),dp(84));
                card.addView(preview,new LinearLayout.LayoutParams(dp(84),dp(84)));
                ImageLoader.load(this,shinyUrl,preview);
                Button imageButton=button("Copy shiny image");
                imageButton.setOnClickListener(v->ImageClipboard.copy(this,shinyUrl));
                card.addView(imageButton);
                Button fileButton=button(filename);
                fileButton.setOnClickListener(v->copy(filename+DOWNLOAD_SUFFIX));
                card.addView(fileButton);
                LinearLayout actions=new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                Button one=button("Copy "+variant+" x1");
                Button three=button("Copy "+variant+" x3");
                one.setOnClickListener(v->copy(shinyLifestylePrompt(dex,shinyName,variant,false,root)));
                three.setOnClickListener(v->copy(shinyLifestylePrompt(dex,shinyName,variant,true,root)));
                actions.addView(one,new LinearLayout.LayoutParams(0,dp(66),1));
                actions.addView(three,new LinearLayout.LayoutParams(0,dp(66),1));
                card.addView(actions);
                contents.addView(card);
            }
            body.addView(section);
        }catch(Exception e){android.util.Log.e("NovarisShinyLifestyle","Cannot render shiny lifestyle variants",e);}
    }
    private String shinyLifestylePrompt(int dex,String shinyName,String variant,boolean triple,JSONObject root){
        try{
            // Build the selected lifestyle once: one class blueprint, one Core, one safety block.
            // The former buildShinyPrompt() concatenation added a second full prompt,
            // including its own job scene, normal colors and Style Core.
            // Build the class-specific prompt once. Never append a second full Shiny
            // prompt (which contains its own core, job and scene instructions).
            JSONObject shinyRoot=new JSONObject(root.toString());
            JSONObject species=shinyRoot.getJSONObject("SPECIES").getJSONObject(String.valueOf(dex));
            String normalText=species.getString("text");
            // Do not splice a complete main-class Shiny prompt into a lifestyle prompt.
            // Its output-control header previously truncated the extracted palette to empty.
            // The selected Shiny image remains the sole color reference.
            String nonColorIdentity=normalText.replaceAll("(?im)^.*\\b(?:COLOR|PALETTE|COLOUR|HUE|PIGMENT|SHADE|TONE)\\b.*$", "");
            species.put("text", "NOVARIS SHINY COLOR AUTHORITY: Selected form '"+shinyName+
                "'. Use the selected Shiny reference image for ALL species colors, markings, "+
                "skin, hair, clothing accents and accessories. Never use Normal palette colors. "+
                "Preserve non-color Pokémon identity and human-only anatomy. "+
                "Do not import the main profession, work outfit, work pose or workplace.\n"+
                "SPECIES IDENTITY (NON-COLOR FEATURES ONLY): "+nonColorIdentity);
            String original=OriginalVariantEngine.build(this,shinyRoot,dex,variant,triple);
            return original+"\nFINAL SHINY CHECK: Only the selected Shiny reference determines colors; "+
                "the stored "+variant+" blueprint determines clothing, pose, props and setting.";
        }catch(Exception e){return "Shiny lifestyle prompt error: "+e.getMessage();}
    }
'''
anchor2='private void copy(String s){'
if s.count(anchor2)!=1:raise SystemExit("Method insertion anchor missing")
s=s.replace(anchor2,method+'\n    '+anchor2,1)
s=s.replace('name.replaceAll("[^A-Za-z0-9_-]+","_"),safeRole(role));', 'name.replaceAll("[^A-Za-z0-9_-]+","_").replaceAll("(?i)(?:_?Shiny)+$", ""),safeRole(role));')
p.write_text(s)
assert add_call in s
assert 'JSONObject shinyRoot=new JSONObject(root.toString())' in s
print("Installed three Shiny lifestyle cards per existing Shiny form")
