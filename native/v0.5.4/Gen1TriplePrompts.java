package com.novaris.pokedex.nativeapp;

import android.content.Context;
import org.json.JSONObject;

/** Three independently copyable, single-image prompts for Gen 1 lifestyle variants. */
public final class Gen1TriplePrompts {
    private Gen1TriplePrompts() {}

    public static String[] build(Context context, JSONObject root, int dex, String variant) throws Exception {
        if (dex < 1 || dex > 151) throw new IllegalArgumentException("Gen 1 only");
        if (!("Casual".equals(variant) || "Nightlife".equals(variant) || "Lingerie".equals(variant)))
            throw new IllegalArgumentException("Lifestyle variant required");
        String oneDescription = OriginalVariantEngine.description(context, root, dex, variant, false);
        String tripleDescription = OriginalVariantEngine.description(context, root, dex, variant, true);
        String base = OriginalVariantEngine.build(context, root, dex, variant, false);
        if (!base.contains(oneDescription)) throw new IllegalStateException("Single-image blueprint not found");
        String[] results = new String[3];
        for (int i=0; i<3; i++) {
            String marker = "IMAGE " + (i+1) + ":";
            int start = tripleDescription.indexOf(marker);
            if (start < 0) throw new IllegalStateException("Missing concept " + (i+1));
            int end = tripleDescription.indexOf('\n', start);
            if (end < 0) end = tripleDescription.length();
            String concept = tripleDescription.substring(start,end).replaceFirst("^IMAGE [123]:", "IMAGE 1:");
            String firstMarker = "IMAGE 1:";
            int firstStart = oneDescription.indexOf(firstMarker);
            if (firstStart < 0) throw new IllegalStateException("Missing single concept");
            int firstEnd = oneDescription.indexOf('\n',firstStart);
            if (firstEnd < 0) firstEnd = oneDescription.length();
            String distinctDescription = oneDescription.substring(0,firstStart) + concept + oneDescription.substring(firstEnd);
            results[i] = base.replace(oneDescription,distinctDescription);
            if (results[i].equals(base) && i>0) throw new IllegalStateException("Concept substitution failed");
            if (results[i].contains("NOVARIS x3 OUTPUT"))
                throw new IllegalStateException("Multiple-image instruction leaked");
        }
        if (results[0].equals(results[1]) || results[1].equals(results[2]) || results[0].equals(results[2]))
            throw new IllegalStateException("Concepts are not distinct");
        return results;
    }
}
