package com.novaris.pokedex.nativeapp;

/** Shared visual identity and output-count instructions for copied prompts. */
public final class PromptSafety {
    private PromptSafety(){}
    private static final String ANATOMY =
        "NOVARIS HUMAN ANATOMY SAFETY — MANDATORY, HIGHEST VISUAL PRIORITY:\n"+
        "The character is an anatomically normal adult human: exactly two human arms, two human legs, one human head, one human torso. "+
        "Species traits are translated into detachable fashion, equipment, surface decoration, silhouette and lighting, never biological extra anatomy. "+
        "No wings sprouting from shoulders or back, no tail, shell or carapace grown into the torso, no implanted cannons, turbines, nozzles or weapons emerging from flesh. "+
        "Charizard wings: wing-shaped cape, articulated garment panels or separate costume accessories only; visible cloth/attachment construction, no organic wing joints. "+
        "Blastoise shell: separate wearable backplate or structured backpack/armor with clear straps and spacing from the body. "+
        "Blastoise water cannons/turbines: external handheld, backpack-mounted or scene props, mechanically attached to equipment, never skin or spine. "+
        "Translate all other nonhuman species parts by the same wearable-not-anatomical rule. "+
        "Recognizability comes from source colors, motifs, texture, posture and clothing, not added animal body parts. "+
        "Before generating, verify human anatomy and reject/regenerate any design with anatomically attached species appendages.";
    private static final String TRIPLE =
        "NOVARIS x3 OUTPUT — EXACTLY THREE INDEPENDENT IMAGES:\n"+
        "Produce exactly THREE separate finished image outputs in this request: IMAGE 1, IMAGE 2 and IMAGE 3. "+
        "Each is a complete single-character illustration with its own independent composition and camera variation. "+
        "Do not produce only one image; do not combine images into a triptych, collage, contact sheet or multi-panel image. "+
        "Generate three individual images as separate outputs, preserving the same identity and species palette. "+
        "Execute IMAGE 1, IMAGE 2 and IMAGE 3 as three distinct generation tasks with visibly different poses, outfits, camera framing and scene composition; do not stop after IMAGE 1. "+
        "If the image-generation interface permits only one output per invocation, make three consecutive image-generation invocations rather than silently reducing the count. "+
        "If this interface cannot perform all three generation tasks, explicitly state the limitation instead of claiming three images were created.";
    public static String enforce(String prompt, boolean triple){
        if(prompt==null)return "";
        // OriginalVariantEngine already applies this wrapper. Copying x3 must
        // not duplicate the anatomy header, x3 instructions or final check.
        if(prompt.contains("NOVARIS OUTPUT CONTROL") &&
           prompt.contains("NOVARIS HUMAN ANATOMY SAFETY") &&
           (!triple || prompt.contains("NOVARIS x3 OUTPUT"))){
            return prompt;
        }
        String title="";
        java.util.regex.Matcher first=java.util.regex.Pattern.compile("(?m)^#[0-9]{4}\\.[^\\r\\n]+").matcher(prompt);
        if(first.find()){
            title=first.group();
            prompt=(prompt.substring(0,first.start())+prompt.substring(first.end())).trim();
        }
        String header="NOVARIS OUTPUT CONTROL\n"+ANATOMY+(triple?"\n\n"+TRIPLE:"");
        return (title.isEmpty()?"":title+"\n\n")+header+"\n\n"+prompt+"\n\nFINAL PRE-GENERATION CHECK: "+(triple?"three separate images; ":"one image; ")+
            "human anatomy only; all species appendages external wearable props, never organic extensions.";
    }
}
