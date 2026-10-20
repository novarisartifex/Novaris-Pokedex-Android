package com.novaris.pokedex.nativeapp;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Exact Java port of the V27.42 storedVariantPayloadV2713 / descV2713 assembly. */
public final class OriginalVariantEngine {
    private OriginalVariantEngine(){}

    public static String build(Context context, JSONObject root, int dex, String label, boolean triple) throws Exception {
        JSONObject species = root.getJSONObject("SPECIES").getJSONObject(String.valueOf(dex));
        String text = species.getString("text");
        if (text.isEmpty()) throw new IllegalStateException("Missing original species text for "+dex);
        String name = species.getString("name");
        String title = String.format(Locale.ROOT, "#%04d.%s — %s",dex,slug(name),label);
        String desc = description(root,dex,label);
        String mode = root.getString(triple?"NOVARIS_OUTPUT_TRIPLE":"NOVARIS_OUTPUT_SINGLE");
        return PromptSafety.enforce(title+"\n\n"+text+"\n\n\n"+desc+"\n\n\n"+mode+"\n\n\n"+root.getString("NOVARIS_CORE_V117"),triple);
    }

    /** Three independent, copyable single-image briefs. Each invocation requests exactly one image. */
    public static String buildScene(Context context,JSONObject root,int dex,String label,int scene)throws Exception{
        if(scene<1||scene>3)throw new IllegalArgumentException("scene must be 1..3");
        String base=build(context,root,dex,label,false);
        String[] scenes=sceneBriefs(label);
        return base+"\n\nNOVARIS INDIVIDUAL IMAGE BRIEF "+scene+"/3 — GENERATE ONE IMAGE ONLY. "+
            "This is a standalone request, not a collage and not an instruction to create the other two images. "+
            "Preserve the same adult character identity and Pokémon species color palette, but follow this unique composition exactly: "+
            scenes[scene-1]+"\nDo not reuse the other scenes' wardrobe, pose, camera angle or setting.";
    }

    private static String[] sceneBriefs(String label){
        switch(label){
            case "Lingerie":
                scenes=new String[]{
                    "LOOK 1 — luxury silk loungewear: fully lined opaque satin camisole and relaxed tailored trousers; standing three-quarter portrait in a bright designer fashion showroom; balanced frontal softbox lighting, relaxed arms, polished editorial elegance.",
                    "LOOK 2 — refined sleepwear editorial: opaque high-neck silk slip layered under a structured lightweight robe; seated upright in a sophisticated hotel lounge chair; warm side light, hands resting naturally, cinematic medium-full portrait.",
                    "LOOK 3 — premium evening lounge set: opaque long-sleeve satin blouse with matching wide-leg silk trousers, species-color embroidered trim; walking naturally through an upscale gallery corridor; dramatic rim lighting, full-body fashion campaign shot."
                };break;
            case "Nightlife":
                scenes=new String[]{
                    "LOOK 1 — stylish nightclub entrance: structured dark tailored jacket with vivid species-color satin details, polished boots; confident full-body standing pose outside a neon-lit venue; rain reflections and strong cinematic contrast.",
                    "LOOK 2 — rooftop night event: distinct asymmetric fashion-forward evening outfit with layered metallic accessories, species-inspired color accents; leaning casually by a rooftop railing; wide skyline bokeh, three-quarter camera angle, cool moonlit rim.",
                    "LOOK 3 — atmospheric jazz lounge: elegant velvet and silk night-out ensemble with distinctive silhouette and species motifs; walking past art-deco lighting, natural hands and composed gaze; warm amber spotlights, dynamic full-length editorial composition."
                };break;
            case "Casual":
                scenes=new String[]{
                    "LOOK 1 — casual city daytime: species-color overshirt, clean fitted tee, relaxed jeans and sneakers; walking on a lively pedestrian street; candid full-body eye-level photograph.",
                    "LOOK 2 — relaxed weekend cafe: soft knit top, tailored chinos and simple accessories with species motif; seated naturally at an outdoor cafe; warm afternoon side light and three-quarter composition.",
                    "LOOK 3 — urban park streetwear: distinctive layered lightweight jacket, wide-leg trousers and practical shoes; standing near a tree-lined pathway; overcast cinematic light and slightly low full-body camera angle."
                };break;
            default:
                scenes=new String[]{
                    "SCENE 1: clear full-body front three-quarter view, signature outfit and confident neutral stance, environmental establishing shot.",
                    "SCENE 2: distinct camera perspective and different outfit layering, character actively interacting with scene, medium-full portrait.",
                    "SCENE 3: different outfit construction, dynamic but anatomically natural pose, dramatic cinematic full-length composition."
                };
        }

        return scenes;
    }

    public static String buildTripleSceneSafe(Context context,JSONObject root,int dex,String label){
        try{
            String base=build(context,root,dex,label,true);
            String[] scenes=sceneBriefs(label);
            return base+"\n\nTHREE DISTINCT REQUIRED IMAGE DESCRIPTIONS — ONE PROMPT, THREE SEPARATE OUTPUTS. "+
                "Generate IMAGE 1, IMAGE 2 and IMAGE 3 as three independent finished images, never a collage or multi-panel composition. "+
                "All three depict the same adult human character and Pokémon identity; each MUST use its own specified wardrobe, setting, pose, lighting and camera angle. "+
                "IMAGE 1: "+scenes[0]+"\n\nIMAGE 2: "+scenes[1]+"\n\nIMAGE 3: "+scenes[2]+
                "\n\nOUTPUT CHECK: Return three separate images, one for each description, not three panels in one image.";
        }catch(Exception e){return "Dreifach-Bildbeschreibung fehlgeschlagen: "+e.getMessage();}
    }

    public static String buildSceneSafe(Context context,JSONObject root,int dex,String label,int scene){
        try{return buildScene(context,root,dex,label,scene);}
        catch(Exception e){return "Bildbeschreibung fehlgeschlagen: "+e.getMessage();}
    }

    public static String slug(String s){
        return (s==null?"":s).trim().replace("♀","Female").replace("♂","Male")
                .replaceAll("[’'`]", "")
                .replaceAll("[^A-Za-z0-9%+.-]+","_")
                .replaceAll("_+","_").replaceAll("^_+|_+$","");
    }

    private static String sysName(String code){
        switch(code){
            case "ffxiv":return "Final Fantasy XIV";
            case "ro":return "Ragnarok Online";
            case "civil":return "Civilian profession system";
            case "gather":return "Gatherer";
            case "craft":return "Crafter";
            default:return "Independent Novaris role";
        }
    }
    private static String val(JSONObject o,String k,String fallback){
        String v=o==null?"":o.optString(k,"");
        return v.isEmpty()?fallback:v;
    }
    private static String meta(JSONArray a,int i){
        return a==null?"":a.optString(i,"");
    }
    public static String description(JSONObject root,int dex,String label)throws Exception{
        boolean life=label.equals("Casual")||label.equals("Nightlife")||label.equals("Lingerie");
        if(life){
            JSONObject b=root.getJSONObject("V2713_LIFESTYLE_BLUEPRINTS").getJSONObject(label);
            String safe=label.equals("Lingerie")?
                " ADULT FASHION EDITORIAL ONLY: All subjects are visibly mature adults aged 25+. Tasteful luxury sleepwear and elegant boudoir-inspired fashion, non-erotic, non-suggestive. Opaque silk camisole with matching tailored shorts, refined satin slip dress with full lining, or elegant matching lounge set. Fully covered chest and intimate areas, no sheer panels, no nudity, no underwear-only framing, no sexualized pose or bedroom intimacy. Confident natural standing or seated fashion-catalog poses. If any stored clothing or pose blueprint conflicts with these requirements, this safety direction overrides it.":
                label.equals("Casual")?" ADULT EVERYDAY FASHION: Three distinct wearable streetwear looks, fully clothed, with different locations and natural candid poses; no sensual emphasis.":"";
            return "NOVARIS LIFESTYLE VARIANT — FULL POKÉDEX DESCRIPTION\nVARIANT: "+label+". ROLE SYSTEM: Novaris Lifestyle / non-professional presentation."+safe+
                "\nCHARACTER: The complete CHARACTER BLUEPRINT, SPECIES COLOR & MATERIAL BLUEPRINT and SPECIES OUTFIT TRANSLATION written above remain exact authority for body family, complexion, face, hair, evolution / prestige tier, source colors, surface identity and wearable visual anchors. Remove all profession / class clothing, equipment, work action and job scene."+
                "\nMATERIAL BLUEPRINT: "+b.getString("m")+". Apply the stored Pokémon color hierarchy and species surface priority directly; keep local colors saturated, warm/cool identity intact, deep darks and clearly different matte / satin / silk / leather / metal / species-surface responses. No pale beige / gray haze or pastel drift."+
                "\nCLOTHING BLUEPRINT: "+(label.equals("Lingerie")?"Translate species colors and markings into opaque, fully lined premium satin and silk lounge-fashion outfits; prioritize modest editorial coverage over any conflicting source garment directions.":b.getString("c"))+". Preserve species-derived silhouette through normal human fashion tailoring, seams, trim and accessories; avoid revealing or sexualized styling."+
                "\nPRESENCE / BODY LANGUAGE: "+(label.equals("Lingerie")?"Relaxed luxury fashion-catalog stance, upright posture, natural hands, calm confident gaze; no provocative gestures or suggestive positioning.":b.getString("s"))+". Express species temperament through gaze, gesture and confidence only."+
                "\nSCENE / PRESENTATION: "+(label.equals("Lingerie")?"Upscale fashion showroom, elegant hotel lounge or softly lit editorial studio; no bed-focused or intimate setting.":b.getString("e"))+". Character-first vertical cinematic framing, directional key, controlled fill, selective rim, reflected local color, deep contact shadow and material-specific highlights.";
        }
        JSONObject b=root.getJSONObject("V2713_CLASS_BLUEPRINTS").optJSONObject(label);
        JSONArray m=root.getJSONObject("V2713_ASSIGNMENT_META").optJSONArray(dex+"|"+label);
        String code=meta(m,0);if(code.isEmpty())code=val(b,"sys","ind");
        String purpose=meta(m,1);if(purpose.isEmpty())purpose=val(b,"p","Class-authentic "+label+" execution.");
        if(purpose.endsWith("."))purpose=purpose.substring(0,purpose.length()-1);
        String spec=meta(m,2);spec=spec.isEmpty()?"":" SPECIALIZATION: "+spec+".";
        return "NOVARIS OPTIONAL CLASS — FULL POKÉDEX DESCRIPTION\nVARIANT: "+label+". ROLE SYSTEM: "+sysName(code)+". ROLE PURPOSE: "+purpose+"."+spec+
            "\nCHARACTER: The complete CHARACTER BLUEPRINT, SPECIES COLOR & MATERIAL BLUEPRINT and SPECIES OUTFIT TRANSLATION written above remain exact authority for body family, complexion, face, hair, evolution / prestige tier, source colors, surface identity and wearable visual anchors. Remove the main profession completely; this optional class is the only active professional layer."+
            "\nMATERIAL BLUEPRINT: "+val(b,"m","premium class-authentic cloth, leather and metal")+". Recolor and rematerialize through the stored Pokémon source-color hierarchy and species surface priority; preserve saturation, warm/cool identity, deep shadows and visibly different material responses instead of generic beige / gray / matte treatment."+
            "\nCLOTHING BLUEPRINT: "+val(b,"c","complete fitted "+label+" outfit with authentic primary equipment")+". Build torso, waist, lower-body construction, outer layer where appropriate, hand / arm treatment, footwear, closures, equipment and species-derived trim deliberately. Translate markings into seams, piping, embroidery, panel breaks, lining, clasps or hardware. The silhouette must read immediately as "+label+" while remaining this Pokémon character rather than generic cosplay."+
            "\nPRESENCE / ACTION: "+val(b,"s","task-driven "+label+" stance")+". Both hands, gaze and weight shift connect to a concrete "+label+" objective; no main-class action or pose survives."+
            "\nSCENE / PRESENTATION: believable Novaris environment that proves the "+label+" role and supports the stored species ecology / temperament. Character-first vertical cinematic framing with directional key, controlled fill, selective rim, reflected local color, deep contact shadows and material-specific highlights.";
    }
}