from pathlib import Path
p=Path("appsrc/app/src/main/java/com/novaris/pokedex/nativeapp/DetailActivity.java")
s=p.read_text()
anchor='addImagePromptCard(dex,i+1,name,url,"",role,true);'
if s.count(anchor)!=1: raise SystemExit("Shiny card insertion anchor missing or ambiguous")
s=s.replace(anchor,anchor+'\n                addShinyLifestyleVariants(dex,i+1,name,url);',1)
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
                    OriginalVariantEngine.slug(shinyName),OriginalVariantEngine.slug(variant));
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
            String original=OriginalVariantEngine.build(this,root,dex,variant,triple);
            String shinyAuthority=buildShinyPrompt(dex,shinyName,triple);
            return "NOVARIS SHINY LIFESTYLE — MANDATORY: This is the selected Shiny form '"+shinyName+
                "', not the normal form. The Shiny reference image copied with this card is the absolute "+
                "authority for colors, markings and their placement. Any normal-form colors in the "+
                "species blueprint, lifestyle descriptions or Style Core are overridden by the Shiny "+
                "reference. Preserve the selected "+variant+" outfit and setting; translate all "+
                "Shiny-specific colors onto clothing, hair, accessories and markings. "+
                "Maintain exactly normal adult human anatomy and detachable species accessories.\n\n"+
                shinyAuthority+"\n\n"+original+
                "\n\nFINAL SHINY COLOR OVERRIDE: Verify all colors and markings against the "+
                "selected Shiny reference image before generating; never revert to the normal palette.";
        }catch(Exception e){return "Shiny lifestyle prompt error: "+e.getMessage();}
    }
'''
anchor2='private void copy(String s){'
if s.count(anchor2)!=1:raise SystemExit("Method insertion anchor missing")
s=s.replace(anchor2,method+'\n    '+anchor2,1)
p.write_text(s)
assert 'addShinyLifestyleVariants(dex,i+1,name,url);' in s
assert 'buildShinyPrompt(dex,shinyName,triple)' in s
print("Installed three Shiny lifestyle cards per existing Shiny form")
