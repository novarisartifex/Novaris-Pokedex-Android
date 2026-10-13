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
        return title+"\n\n"+text+"\n\n\n"+desc+"\n\n\n"+mode+"\n\n\n"+root.getString("NOVARIS_CORE_V117");
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
            String safe=label.equals("Lingerie")?" Mature adult luxury fashion-editorial with full-coverage styling, neutral elegant pose and refined dressing-room presentation.":"";
            return "NOVARIS LIFESTYLE VARIANT — FULL POKÉDEX DESCRIPTION\nVARIANT: "+label+". ROLE SYSTEM: Novaris Lifestyle / non-professional presentation."+safe+
                "\nCHARACTER: The complete CHARACTER BLUEPRINT, SPECIES COLOR & MATERIAL BLUEPRINT and SPECIES OUTFIT TRANSLATION written above remain exact authority for body family, complexion, face, hair, evolution / prestige tier, source colors, surface identity and wearable visual anchors. Remove all profession / class clothing, equipment, work action and job scene."+
                "\nMATERIAL BLUEPRINT: "+b.getString("m")+". Apply the stored Pokémon color hierarchy and species surface priority directly; keep local colors saturated, warm/cool identity intact, deep darks and clearly different matte / satin / silk / leather / metal / species-surface responses. No pale beige / gray haze or pastel drift."+
                "\nCLOTHING BLUEPRINT: "+b.getString("c")+". Preserve the stored femininity / exposure direction and species-derived silhouette. Every listed garment, closure, trim and accessory is intentional; do not replace it with generic "+label.toLowerCase(Locale.ROOT)+" shorthand."+
                "\nPRESENCE / BODY LANGUAGE: "+b.getString("s")+". Express species temperament through pace, gaze, gesture and confidence only."+
                "\nSCENE / PRESENTATION: "+b.getString("e")+". Character-first vertical cinematic framing, directional key, controlled fill, selective rim, reflected local color, deep contact shadow and material-specific highlights.";
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