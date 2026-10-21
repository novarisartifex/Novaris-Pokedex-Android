package com.novaris.pokedex.nativeapp;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Exact Java port of the V27.42 storedVariantPayloadV2713 / descV2713 assembly. */
public final class OriginalVariantEngine {
    private OriginalVariantEngine(){}
    private static volatile JSONObject lifestyleSpeciesCache;
    private static synchronized JSONObject lifestyleSpecies(Context context) throws Exception {
        if(lifestyleSpeciesCache!=null)return lifestyleSpeciesCache;
        try(InputStream stream=context.getAssets().open("novaris_species_lifestyle_blueprints_v1.json")){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            byte[] buffer=new byte[8192];int n;
            while((n=stream.read(buffer))!=-1)bytes.write(buffer,0,n);
            lifestyleSpeciesCache=new JSONObject(bytes.toString("UTF-8")).getJSONObject("species");
            return lifestyleSpeciesCache;
        }
    }

    public static String build(Context context, JSONObject root, int dex, String label, boolean triple) throws Exception {
        JSONObject species = root.getJSONObject("SPECIES").getJSONObject(String.valueOf(dex));
        String text = species.getString("text");
        if (text.isEmpty()) throw new IllegalStateException("Missing original species text for "+dex);
        String name = species.getString("name");
        String title = String.format(Locale.ROOT, "#%04d.%s — %s",dex,slug(name),label);
        String desc = description(context,root,dex,label,triple);
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
    public static String description(Context context,JSONObject root,int dex,String label,boolean triple)throws Exception{
        boolean life=label.equals("Casual")||label.equals("Nightlife")||label.equals("Lingerie");
        if(life){
            JSONObject b=root.getJSONObject("V2713_LIFESTYLE_BLUEPRINTS").getJSONObject(label);
            // Individually specified first Pokédex block takes precedence over all legacy generated drafts.
            if(dex>=1&&dex<=1025){
                String blockAsset=dex<=50?"novaris_lifestyle_block_0001_0050_v2.json":dex<=100?"novaris_lifestyle_block_0051_0100_v2.json":dex<=150?"novaris_lifestyle_block_0101_0150_v2.json":dex<=200?"novaris_lifestyle_block_0151_0200_v2.json":dex<=250?"novaris_lifestyle_block_0201_0250_v2.json":dex<=300?"novaris_lifestyle_block_0251_0300_v2.json":dex<=350?"novaris_lifestyle_block_0301_0350_v2.json":dex<=400?"novaris_lifestyle_block_0351_0400_v2.json":dex<=450?"novaris_lifestyle_block_0401_0450_v2.json":dex<=500?"novaris_lifestyle_block_0451_0500_v2.json":dex<=550?"novaris_lifestyle_block_0501_0550_v2.json":dex<=600?"novaris_lifestyle_block_0551_0600_v2.json":dex<=650?"novaris_lifestyle_block_0601_0650_v2.json":dex<=700?"novaris_lifestyle_block_0651_0700_v2.json":dex<=750?"novaris_lifestyle_block_0701_0750_v2.json":dex<=800?"novaris_lifestyle_block_0751_0800_v2.json":dex<=850?"novaris_lifestyle_block_0801_0850_v2.json":dex<=900?"novaris_lifestyle_block_0851_0900_v2.json":dex<=950?"novaris_lifestyle_block_0901_0950_v2.json":dex<=1000?"novaris_lifestyle_block_0951_1000_v2.json":"novaris_lifestyle_block_1001_1025_v2.json";
                try(InputStream stream=context.getAssets().open(blockAsset)){
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                    byte[] buf=new byte[8192];int count;
                    while((count=stream.read(buf))!=-1)bytes.write(buf,0,count);
                    JSONObject item=new JSONObject(bytes.toString("UTF-8")).getJSONObject("species").getJSONObject(String.valueOf(dex));
                    JSONObject variant=item.getJSONObject("variants").getJSONObject(label);
                    JSONArray concepts=variant.getJSONArray("concepts");
                    StringBuilder prompt=new StringBuilder("NOVARIS LIFESTYLE — SPECIES-SPECIFIC STORED BLUEPRINT\n");
                    prompt.append("VARIANT: ").append(label).append(". CHARACTER: Preserve exact species colors, face, hair, complexion, markings and normal adult human anatomy from the authoritative Pokémon character description above. Adults aged 25+.\n");
                    prompt.append("SPECIES FASHION TRANSLATION: ").append(variant.getString("identity_translation")).append(". No additional biological limbs, tails or wings. All special shapes must be clearly detachable clothing or props.\n");
                    prompt.append("OUTFIT: ").append(variant.getString("outfit")).append(".\n");
                    prompt.append("MATERIALS: ").append(variant.getString("materials")).append(".\n");
                    prompt.append("Fashion editorial with opaque intimate coverage, no nudity or explicit sexual activity. Cinematic Novaris fantasy world, never a generic real-world club.\n");
                    int limit=triple?3:1;
                    prompt.append(triple?"THREE SEPARATE IMAGES, each a distinct full composition, NOT a collage.\n":"SINGLE IMAGE.\n");
                    for(int i=0;i<limit;i++){
                        JSONObject c=concepts.getJSONObject(i);
                        prompt.append("IMAGE ").append(i+1).append(": Wardrobe: ").append(c.getString("outfit"))
                              .append(". Pose: ").append(c.getString("pose"))
                              .append(". Scene: ").append(c.getString("scene"))
                              .append(". Lighting: ").append(c.getString("lighting")).append(".\n");
                    }
                    return prompt.toString();
                }
            }
            // Prefer stored species-specific blueprints; unreviewed entries are automated drafts.
            try{
                    JSONObject entries=lifestyleSpecies(context);
                    JSONObject entry=entries.optJSONObject(String.valueOf(dex));
                    JSONObject custom=entry==null?null:entry.optJSONObject(label);
                    if(custom!=null){
                        return "NOVARIS LIFESTYLE VARIANT — INDIVIDUAL SPECIES BLUEPRINT\\nVARIANT: "+label+
                            ". CHARACTER: Preserve the exact Pokémon species identity, face, hair, complexion, palette, markings and human-only anatomy from the character blueprint above. All characters are clearly adults aged 25+."+
                            "\\nCLOTHING BLUEPRINT: "+custom.getString("outfit")+". Adapt every color and marking to the Pokémon identity; wearable accessories only. For lingerie: editorial adult fashion with opaque intimate coverage, no nudity."+
                            "\\nMATERIAL BLUEPRINT: "+custom.getString("materials")+". Honor the original species palette and tactile surface cues."+
                            "\\nPRESENCE / BODY LANGUAGE: "+custom.getString("pose")+". Natural human proportions and hands."+
                            "\\nSCENE / PRESENTATION: "+custom.getString("scene")+". Character-first cinematic vertical composition, authentic Novaris fantasy architecture and atmospheric lighting.";
                    }
            }catch(Exception ignored){ /* Retain legacy generated fallback when a blueprint asset is absent. */ }
            // Deterministic, species-specific wardrobe and posture rotation; all source blueprints remain authoritative.
            int seed=Math.floorMod(dex*31+label.hashCode(), 997);
            String[] clothing;
            String[] poses;
            String[] places;
            if(label.equals("Lingerie")){
                clothing=new String[]{
                    "ornate Chantilly-lace balconette set with high-waist briefs, embroidered garter-inspired waist detailing and a dramatic silk robe",
                    "sculpted satin corset with contrasting guipure-lace panels, ribbon lacing, matching tailored shorts and opera-length gloves",
                    "delicate embroidered lace bodysuit with lined geometric cutwork, jewel clasps and a floor-length translucent outer cape",
                    "romantic longline bralette and high-rise lace shorts with scalloped edges, detachable chiffon train and pearl detailing",
                    "black velvet-and-lace bustier with gold filigree embroidery, high-waisted matching bottoms and an open brocade jacket",
                    "iridescent silk slip with intricate lace insets, asymmetric hem and jeweled shoulder chains",
                    "structured brocade corset top with a sweeping satin overskirt, embroidered stockings and ornate clasp details",
                    "sheer-look illusion tulle layered over opaque panels, floral appliques, sculptural straps and a tailored kimono",
                    "vintage-inspired embroidered satin teddy with scalloped lace neckline and a dramatic velvet dressing mantle",
                    "fine floral-lace camisole with satin tap shorts, elaborate ribbon trim and long embroidered gloves",
                    "jewel-toned halter lingerie set with metallic lace overlays, high-rise bottoms and a flowing split-sleeve robe",
                    "couture-inspired lace-up bustier with asymmetric satin draping, matching high-waist briefs and detachable shoulder cape",
                    "delicate point d'esprit tulle overlay with opaque silk foundation, embroidered bodice and ornate waist chains",
                    "gothic-romantic lace bodysuit with velvet ribbon motifs, fitted corset belt and a trailing chiffon wrap",
                    "regal jacquard bustier with scalloped lace edges, matching tailored bloomers and layered silk sleeves",
                    "art-deco geometric lace set with metallic thread embroidery, sculpted waist panel and jewel-toned satin cover-up"
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
                    "dramatic backless-look evening gown with lined illusion mesh, asymmetric train and crystal shoulder ornaments",
                    "tailored leather-and-brocade ensemble with sculpted corset-style jacket, fitted trousers and tall heeled boots",
                    "midnight velvet mini-dress with elaborate lace sleeves, metallic filigree belt and gemstone accents",
                    "liquid-satin halter gown with architectural neckline, high side drape and ornate waist jewelry",
                    "shimmering chainmail-inspired evening top over opaque lining, flowing silk trousers and a cropped velvet cape",
                    "dark romantic lace-panel cocktail dress with structured bodice, dramatic sleeves and jeweled gloves",
                    "iridescent sequin jumpsuit with deep V-shaped tailored lapels, sculpted shoulders and sweeping trouser legs",
                    "gothic aristocrat tuxedo with embroidered waistcoat, fitted high-waist trousers and an ornate capelet",
                    "asymmetric one-shoulder silk dress with layered chiffon, thigh-length draped overskirt and luminous trim",
                    "brocade bustier-style evening top with flowing palazzo pants, layered necklaces and fitted opera gloves",
                    "jewel-toned velvet suit with corset-inspired waistcoat, dramatic lapels and polished high boots",
                    "metallic pleated mini-dress with embroidered detachable sleeves, tall boots and fantasy gemstone jewelry",
                    "elegant off-shoulder satin gown with sculptural draping, embroidered side panels and long trailing ribbons",
                    "masquerade-inspired fitted jacket with lace jabot, high-waisted shorts, patterned tights and ornate mask accessory",
                    "luxurious silk wrap dress with lace insets, asymmetric hem, dramatic belt and crystal hair ornaments",
                    "fantasy cabaret-inspired tailored bodysuit worn under a structured brocade coat, opaque tights and knee-high boots"
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
            // Independent modular wardrobe pools; never repeat the same fixed outfit recipe.
            String[] tops;
            String[] bottoms;
            String[] legwear;
            String[] details;
            if(label.equals("Lingerie")){
                tops=new String[]{
                    "Chantilly lace balconette bra", "plunge bra with embroidered cups", "longline bralette",
                    "lace-up satin bustier", "sculpted overbust corset", "underbust corset over a silk camisole",
                    "floral lace bodysuit", "embroidered mesh-overlay teddy", "silk halterneck lingerie top",
                    "vintage basque", "strapless bandeau with lace edging", "demi-cup bra with satin trim",
                    "romantic babydoll", "lace camisole", "satin chemise", "architectural cut-out bodysuit"
                };
                bottoms=new String[]{
                    "matching high-waist briefs", "Brazilian-cut briefs", "lace boyshorts",
                    "French knickers", "satin tap pants", "embroidered bikini briefs",
                    "tailored silk bloomers", "scalloped lace shorts", "matching high-rise bottoms",
                    "layered chiffon wrap skirt", "silk side-tie briefs", "matching opaque lingerie bottoms"
                };
                legwear=new String[]{
                    "garter belt with attached seamed stockings", "ornate suspender belt and lace-top stockings",
                    "decorative thigh garters with thigh-high stockings", "fishnet stockings with lace tops",
                    "fine patterned tights", "silk stockings with back seams",
                    "embroidered thigh-high stockings", "lace garters and sheer-look lined legwear",
                    "no stockings, elegant bare-leg styling", "decorative garter straps over opaque hosiery"
                };
                details=new String[]{
                    "scalloped Leavers lace and pearl clasps", "guipure lace with velvet ribbon",
                    "floral lace applique and silk bows", "metallic-thread embroidery and jewel chains",
                    "point d'esprit tulle over opaque satin", "delicate straps and geometric lace",
                    "silk robe worn open as an outer layer", "sheer-look chiffon cape over fully lined garments",
                    "opera gloves and an embroidered kimono", "contrast lace panels and corset lacing",
                    "embroidered velvet cover-up", "delicate waist chain and ornate clasps"
                };
                poses=new String[]{
                    "three-quarter standing pose, one hand on hip, shoulders angled",
                    "sitting sideways on a velvet chair, legs elegantly crossed",
                    "standing beside a vanity, fingers touching a jewel clasp",
                    "walking forward while a silk robe trails behind",
                    "looking into an ornate mirror in a natural over-shoulder pose",
                    "leaning against a marble pillar with relaxed weight shift",
                    "perched on a cushioned bench, upright with ankles crossed",
                    "turning mid-step, fabric and hair in motion",
                    "standing with one foot on a low step, arms relaxed",
                    "seated on a chaise, torso upright and gaze direct",
                    "adjusting an embroidered glove, three-quarter profile",
                    "walking through an archway, head turned toward camera",
                    "standing with arms gently folded at the waist",
                    "sitting at a dressing table with one hand on the surface",
                    "profile pose beside a window, weight on back leg",
                    "standing in a graceful contrapposto with a soft shoulder turn"
                };
            }else if(label.equals("Nightlife")){
                tops=new String[]{
                    "lace crop top", "structured corset top", "silk halter top",
                    "off-shoulder satin blouse", "one-shoulder metallic top", "fitted lace bodysuit",
                    "embroidered bustier top", "backless-look lined evening top",
                    "sequin bandeau with tailored overlay", "velvet wrap top",
                    "sculpted brocade waistcoat", "sheer-look mesh overlay over opaque top",
                    "asymmetric cut-out top", "tailored cropped leather jacket",
                    "satin camisole", "ornate fantasy cabaret jacket"
                };
                bottoms=new String[]{
                    "leather mini skirt", "pleated mini skirt", "asymmetric wrap mini skirt",
                    "high-waist pencil skirt", "decorative slit skirt", "fitted velvet mini skirt",
                    "tailored hot pants", "high-waist leather shorts", "flared satin trousers",
                    "sleek leather pants", "wide-leg palazzo trousers", "embroidered brocade shorts",
                    "metallic mini skirt", "layered chiffon mini skirt", "tailored slim trousers",
                    "sequin mini skirt"
                };
                legwear=new String[]{
                    "patterned tights and knee-high boots", "fishnet tights and ankle boots",
                    "thigh-high boots", "lace tights and platform heels",
                    "sheer-look tights and stiletto heels", "opaque tights and polished boots",
                    "bare-leg styling with ornate heels", "decorative stockings and velvet ankle boots"
                };
                details=new String[]{
                    "statement choker and crystal jewelry", "opera gloves and jeweled belt",
                    "metallic filigree chains and ornate clasps", "cropped velvet blazer",
                    "faux-fur stole and gemstone earrings", "masquerade mask carried in one hand",
                    "embroidered bolero and layered necklaces", "asymmetric fantasy capelet",
                    "lace gloves and high-shine accessories", "dramatic belt and crystal hair ornaments",
                    "short leather jacket and silver jewelry", "embroidered silk scarf and jeweled cuffs"
                };
                poses=new String[]{
                    "confident stride across a lantern-lit ballroom",
                    "mid-dance turn with one arm extended naturally",
                    "sitting sideways on a velvet lounge stool, legs crossed",
                    "leaning on a balcony rail and looking toward the city",
                    "walking down a broad staircase, one hand on the rail",
                    "standing in contrapposto with one hand on hip",
                    "looking over one shoulder while walking away",
                    "one foot on a low step, relaxed upright stance",
                    "turning toward a companion just outside the frame",
                    "leaning lightly against a stone arch with crossed ankles",
                    "seated in a booth, shoulders angled toward camera",
                    "holding a masquerade mask at waist height",
                    "standing beneath lanterns with arms relaxed and chin raised",
                    "mid-step crossing a fantasy skybridge, fabric in motion",
                    "standing beside a fountain, one hand adjusting a bracelet",
                    "dynamic side-step on a dance floor with balanced anatomy"
                };
            }else{
                tops=new String[]{"knit sweater","denim jacket","linen shirt","layered cardigan","cropped utility jacket","bomber jacket"};
                bottoms=new String[]{"straight-leg jeans","pleated trousers","cargo skirt","wide-leg chinos","midi skirt","tailored shorts"};
                legwear=new String[]{"comfortable boots","low-profile sneakers","ankle boots","leather flats"};
                details=new String[]{"crossbody bag","woven belt","light scarf","simple jewelry"};
            }
            int wardrobe=seed%clothing.length;
            int posture=Math.floorMod(seed*7+11,poses.length);
            int location=Math.floorMod(seed*11+3,places.length);
            String wardrobeDesign=tops[Math.floorMod(seed*3+1,tops.length)]+", paired with "+bottoms[Math.floorMod(seed*7+2,bottoms.length)]+", "+legwear[Math.floorMod(seed*11+4,legwear.length)]+", and "+details[Math.floorMod(seed*13+6,details.length)];
            String safety=label.equals("Lingerie")?
                " All characters are clearly adults aged 25+. Elegant adult lingerie fashion editorial; opaque coverage of intimate anatomy, no nudity or explicit sexual activity.":
                " Adult character fashion editorial.";
            return "NOVARIS LIFESTYLE VARIANT — FULL POKÉDEX DESCRIPTION\\nVARIANT: "+label+". ROLE SYSTEM: Novaris Lifestyle / non-professional presentation."+safety+
                "\\nCHARACTER: The complete CHARACTER BLUEPRINT, SPECIES COLOR & MATERIAL BLUEPRINT and SPECIES OUTFIT TRANSLATION written above remain authoritative for complexion, face, hair, source colors, species identity and wearable visual anchors. No class equipment or work action."+
                "\\nMATERIAL BLUEPRINT: "+b.getString("m")+". Preserve the Pokémon source-color hierarchy, saturated local colors, contrasting matte and lustrous surfaces, rich textures and species-derived motifs."+
                "\\nCLOTHING BLUEPRINT: "+wardrobeDesign+". Silhouette inspiration: "+clothing[wardrobe]+". Integrate the stored lifestyle clothing blueprint as a design influence: "+b.getString("c")+". Adapt tailoring, lace patterns, trim, fastenings and accessories to the Pokémon rather than generic fashion. Avoid repeating the same silhouette for different Pokémon."+
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