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
            // Deterministic, species-specific wardrobe and posture rotation; all source blueprints remain authoritative.
            int seed=Math.floorMod(dex*31+label.hashCode(), 997);
            String[] clothing;
            String[] poses;
            String[] places;
            if(label.equals("Lingerie")){
                clothing=new String[]{
                    "structured lace-trim balconette lingerie with matching high-waist briefs and a detachable silk robe",
                    "delicate floral-lace bodysuit with opaque lining, tailored seams and fine embroidery",
                    "satin-and-lace corset-inspired bustier with matching tailored lingerie bottoms and sheer outer wrap",
                    "embroidered longline bralette with matching high-rise briefs and a flowing chiffon dressing gown",
                    "elegant lace-panel teddy with opaque coverage and a detachable velvet kimono",
                    "refined satin slip with contrasting lace appliques, adjustable straps and a short silk cover-up",
                    "architectural mesh-overlay lingerie set with lined panels, decorative straps and geometric embroidery",
                    "vintage-inspired lace-trim camisole and tap shorts with a fitted velvet dressing jacket"
                };
                poses=new String[]{
                    "standing three-quarter turn, weight on back leg, one hand adjusting robe cuff",
                    "seated sideways on an upholstered chair, legs naturally crossed, gaze over shoulder",
                    "walking through a dressing gallery, one hand holding a flowing cover-up",
                    "leaning lightly against a carved pillar, relaxed asymmetrical shoulders",
                    "standing in profile with one arm resting on a vanity, head turned toward camera",
                    "perched on a velvet bench, hands resting naturally, poised upright posture",
                    "mid-step beside a tall mirror, garment movement visible, confident forward gaze",
                    "reclining modestly in an editorial chaise pose, fully supported and elegantly composed"
                };
                places=new String[]{
                    "private Novaris couture atelier with stained-glass lanterns and carved stone",
                    "luxury Novaris fashion salon with velvet furniture and brass filigree",
                    "moonlit Novaris palace dressing gallery with luminous mosaic floors",
                    "enchanted Novaris conservatory with silk screens and warm crystal lights",
                    "historic Novaris manor wardrobe chamber with embroidered tapestries",
                    "high-end Novaris editorial studio with ornate fantasy architecture",
                    "Novaris royal bathhouse antechamber with marble columns, no bathing",
                    "grand Novaris boutique fitting suite with jewel-toned drapery"
                };
            }else if(label.equals("Nightlife")){
                clothing=new String[]{
                    "tailored velvet mini-dress with metallic embroidery and ankle boots",
                    "asymmetric satin evening gown with sculpted shoulder and jewelry",
                    "sleek leather trousers with embroidered corset-style top and fitted jacket",
                    "glittering sequin jumpsuit with dramatic wide-leg silhouette",
                    "silk halter top with high-waisted tailored skirt and jeweled belt",
                    "structured brocade blazer dress with tall boots and ornate fastenings",
                    "layered chiffon party dress with fitted waist and statement accessories",
                    "dark satin two-piece evening suit with a luminous embroidered waistcoat"
                };
                poses=new String[]{
                    "striding confidently toward the viewer, coat and hair moving",
                    "standing at a balcony rail, looking over one shoulder",
                    "mid-dance turn with natural arm movement and dynamic fabric",
                    "sitting on a lounge stool, one elbow lightly on the counter",
                    "leaning against a stone arch, one leg relaxed and gaze direct",
                    "walking down illuminated steps, hand skimming the balustrade",
                    "talking with a companion just outside frame, expressive hand gesture",
                    "standing in a three-quarter fashion pose with one hand on hip"
                };
                places=new String[]{
                    "Novaris moonlit citadel terrace during an elegant masquerade",
                    "crystal-lit Novaris fantasy tavern with richly dressed patrons",
                    "royal Novaris midnight ball beneath floating lanterns",
                    "ornate Novaris canal-side evening promenade with magical reflections",
                    "Novaris opera-house foyer with gilded arches and warm lamps",
                    "enchanted Novaris rooftop garden overlooking castle spires",
                    "Novaris noble district festival beneath jewel-colored banners",
                    "luxury Novaris skybridge lounge with stained glass and distant towers"
                };
            }else{
                clothing=new String[]{
                    "cropped utility jacket with straight-leg jeans and layered shirt",
                    "soft knit sweater with pleated trousers and leather sneakers",
                    "tailored denim overshirt with cargo skirt and ankle boots",
                    "relaxed linen shirt with wide-leg trousers and woven belt",
                    "sporty bomber jacket with fitted tee and tapered joggers",
                    "layered cardigan with midi skirt and practical crossbody bag",
                    "sleek turtleneck with tailored shorts, tights and boots",
                    "casual patterned shirt with cuffed chinos and low-profile trainers"
                };
                poses=new String[]{
                    "walking along a market lane, looking at a shop display",
                    "sitting on broad stone steps with relaxed natural posture",
                    "leaning against a courtyard railing with crossed ankles",
                    "turning mid-stride with one hand adjusting a bag strap",
                    "standing in relaxed contrapposto while reading a notice",
                    "perched on a low wall with hands naturally resting",
                    "reaching toward a market stall, candid three-quarter profile",
                    "walking through a garden arch, gaze directed ahead"
                };
                places=new String[]{
                    "sunlit Novaris old-town market",
                    "Novaris riverside pedestrian quarter",
                    "stone-paved Novaris academy courtyard",
                    "green Novaris botanical garden walkway",
                    "Novaris artisan district with colorful shopfronts",
                    "Novaris castle-town public square",
                    "quiet Novaris canal bridge with hanging flowers",
                    "Novaris neighborhood cafe terrace"
                };
            }
            int wardrobe=seed%clothing.length;
            int posture=(seed/3+3)%poses.length;
            int location=(seed/5+5)%places.length;
            String safety=label.equals("Lingerie")?
                " All characters are clearly adults aged 25+. Elegant adult lingerie fashion editorial; opaque coverage of intimate anatomy, no nudity or explicit sexual activity.":
                " Adult character fashion editorial.";
            return "NOVARIS LIFESTYLE VARIANT — FULL POKÉDEX DESCRIPTION\\nVARIANT: "+label+". ROLE SYSTEM: Novaris Lifestyle / non-professional presentation."+safety+
                "\\nCHARACTER: The complete CHARACTER BLUEPRINT, SPECIES COLOR & MATERIAL BLUEPRINT and SPECIES OUTFIT TRANSLATION written above remain authoritative for complexion, face, hair, source colors, species identity and wearable visual anchors. No class equipment or work action."+
                "\\nMATERIAL BLUEPRINT: "+b.getString("m")+". Preserve the Pokémon source-color hierarchy, saturated local colors, contrasting matte and lustrous surfaces, rich textures and species-derived motifs."+
                "\\nCLOTHING BLUEPRINT: "+clothing[wardrobe]+". Integrate the stored lifestyle clothing blueprint as a design influence: "+b.getString("c")+". Adapt tailoring, lace patterns, trim, fastenings and accessories to the Pokémon rather than generic fashion. Avoid repeating the same silhouette for different Pokémon."+
                "\\nPRESENCE / BODY LANGUAGE: "+poses[posture]+". Reflect the stored character presence: "+b.getString("s")+". Distinct weight distribution, hand gesture and camera angle; natural human anatomy."+
                "\\nSCENE / PRESENTATION: "+places[location]+". Incorporate blueprint atmosphere: "+b.getString("e")+". Remain visibly inside the Novaris fantasy world, not a modern real-world nightclub. Vertical cinematic composition, atmospheric depth, controlled key and rim lighting, textured shadows.";
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