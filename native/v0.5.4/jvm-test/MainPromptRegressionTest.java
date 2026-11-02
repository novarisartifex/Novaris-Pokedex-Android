package com.novaris.pokedex.nativeapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 33)
public class MainPromptRegressionTest {
    private static int count(String text,String part) {
        int count=0, from=0;
        while ((from=text.indexOf(part,from))>=0) {count++;from+=part.length();}
        return count;
    }

    @Test public void allSixteenProductionMainPrompts() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        DatabaseManager data=new DatabaseManager(context);
        DetailActivity activity=new DetailActivity();
        Field field=DetailActivity.class.getDeclaredField("data");
        field.setAccessible(true);
        field.set(activity,data);
        Method shiny=DetailActivity.class.getDeclaredMethod("buildShinyPrompt",int.class,String.class,boolean.class);
        shiny.setAccessible(true);
        int checked=0;
        for(int dex:new int[]{190,324,332,338}) {
            assertNotNull("Missing original Pokédex entry "+dex,data.pokemonDetail(dex));
            for(boolean isShiny:new boolean[]{false,true}) {
                for(boolean triple:new boolean[]{false}) {
                    String id=dex+"/"+(isShiny?"Shiny":"Normal")+"/"+(triple?"x3":"x1");
                    String prompt=isShiny
                        ? (String)shiny.invoke(activity,dex,"Shiny",triple)
                        : data.buildPokemonPrompt(dex,triple);
                    assertNotNull(id,prompt);
                    // The production copy buttons apply PromptSafety.enforce before
                    // placing the assembled main-class prompt on the clipboard.
                    // Verify that final output, not the unwrapped database string.
                    prompt=PromptSafety.enforce(prompt,triple);
                    assertTrue(id+" empty",prompt.length()>300);
                    assertEquals(id+" anatomy safety",1,count(prompt,"NOVARIS HUMAN ANATOMY SAFETY"));
                    assertEquals(id+" output control",1,count(prompt,"NOVARIS OUTPUT CONTROL"));
                    // The Core mentions its own marker in its completeness instructions.
                    // Only count the actual standalone terminal marker line.
                    assertEquals(id+" Style Core terminal line",1,count(prompt,"\n[NOVARIS_CORE_END:v1.1.8]\n"));
                    assertEquals(id+" Style Core header",1,count(prompt,"NOVARIS STYLE CORE"));
                    assertEquals(id+" shiny color authority",isShiny?1:0,
                        count(prompt,"SHINY REFERENCE IMAGE — MANDATORY COLOR AUTHORITY"));
                    assertTrue(id+" triple length",!triple||prompt.length()>data.buildPokemonPrompt(dex,false).length());
                    assertFalse(id+" lifestyle contamination",prompt.contains("NOVARIS LIFESTYLE — SPECIES-SPECIFIC STORED BLUEPRINT"));
                    checked++;
                }
            }
        }
        assertEquals(8,checked);
        System.out.println("Executed 8 REAL production x1 DatabaseManager/DetailActivity main prompts");
    }
    private static android.widget.Button findButton(android.view.View view,String label) {
        if(view instanceof android.widget.Button && label.equals(((android.widget.Button)view).getText().toString()))
            return (android.widget.Button)view;
        if(view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) {
                android.widget.Button found=findButton(group.getChildAt(i),label);
                if(found!=null)return found;
            }
        }
        return null;
    }

    @Test public void realMainButtonCopiesSingleImagePrompt() throws Exception {
        android.content.Intent intent=new android.content.Intent(
            ApplicationProvider.getApplicationContext(),DetailActivity.class);
        intent.putExtra("dex",190);
        DetailActivity activity=org.robolectric.Robolectric.buildActivity(DetailActivity.class,intent)
            .setup().get();
        android.widget.Button button=findButton(activity.findViewById(android.R.id.content),"Copy Core + Dex x1");
        assertNotNull("Main x1 button missing from rendered Android activity",button);
        assertTrue("Main x1 button click failed",button.performClick());
        android.content.ClipboardManager clipboard=(android.content.ClipboardManager)
            activity.getSystemService(Context.CLIPBOARD_SERVICE);
        String copied=clipboard.getPrimaryClip().getItemAt(0).coerceToText(activity).toString();
        assertTrue("Pokémon heading",copied.contains("#0190."));
        assertEquals("Anatomy safety",1,count(copied,"NOVARIS HUMAN ANATOMY SAFETY"));
        assertEquals("Output control",1,count(copied,"NOVARIS OUTPUT CONTROL"));
        assertFalse("No triple mode",copied.contains("NOVARIS x3 OUTPUT"));
        activity.finish();
    }

    @Test public void renderedLifestyleButtonsCopyTheirOwnX1Prompts() throws Exception {
        android.content.Intent intent=new android.content.Intent(
            ApplicationProvider.getApplicationContext(),DetailActivity.class);
        intent.putExtra("dex",190);
        DetailActivity activity=org.robolectric.Robolectric.buildActivity(DetailActivity.class,intent)
            .setup().get();
        Method render=DetailActivity.class.getDeclaredMethod("addOriginalVariants",int.class);
        render.setAccessible(true);
        render.invoke(activity,190);
        for(String role:new String[]{"Casual","Nightlife","Lingerie"}) {
            android.widget.Button button=findButton(activity.findViewById(android.R.id.content),"Copy "+role+" x1");
            assertNotNull(role+" actual rendered button",button);
            assertTrue(role+" click",button.performClick());
            android.content.ClipboardManager clipboard=(android.content.ClipboardManager)
                activity.getSystemService(Context.CLIPBOARD_SERVICE);
            String copied=clipboard.getPrimaryClip().getItemAt(0).coerceToText(activity).toString();
            assertTrue(role+" class",copied.contains("VARIANT: "+role));
            assertEquals(role+" anatomy",1,count(copied,"NOVARIS HUMAN ANATOMY SAFETY"));
            assertEquals(role+" output",1,count(copied,"NOVARIS OUTPUT CONTROL"));
            assertTrue(role+" single image",copied.contains("IMAGE 1:"));
            assertFalse(role+" no second image",copied.contains("IMAGE 2:"));
        }
        activity.finish();
    }

    private static android.widget.Button findLastButton(android.view.View view,String label) {
        android.widget.Button result=null;
        if(view instanceof android.widget.Button && label.equals(((android.widget.Button)view).getText().toString()))
            result=(android.widget.Button)view;
        if(view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) {
                android.widget.Button candidate=findLastButton(group.getChildAt(i),label);
                if(candidate!=null)result=candidate;
            }
        }
        return result;
    }

    @Test public void shinyLifestyleButtonsCopyActualX1Prompts() throws Exception {
        android.content.Intent intent=new android.content.Intent(
            ApplicationProvider.getApplicationContext(),DetailActivity.class);
        intent.putExtra("dex",190);
        DetailActivity activity=org.robolectric.Robolectric.buildActivity(DetailActivity.class,intent)
            .setup().get();
        Method render=DetailActivity.class.getDeclaredMethod("addShinyLifestyleVariants",
            int.class,int.class,String.class,String.class);
        render.setAccessible(true);
        render.invoke(activity,190,1,"Shiny","");
        for(String role:new String[]{"Casual","Nightlife","Lingerie"}) {
            android.widget.Button button=findLastButton(activity.findViewById(android.R.id.content),"Copy "+role+" x1");
            assertNotNull(role+" Shiny button",button);
            assertTrue(role+" Shiny click",button.performClick());
            android.content.ClipboardManager clipboard=(android.content.ClipboardManager)
                activity.getSystemService(Context.CLIPBOARD_SERVICE);
            String copied=clipboard.getPrimaryClip().getItemAt(0).coerceToText(activity).toString();
            assertTrue(role+" Shiny palette",copied.contains("SHINY LIFESTYLE PALETTE OVERRIDE"));
            assertTrue(role+" class",copied.contains("VARIANT: "+role));
            assertEquals(role+" safety",1,count(copied,"NOVARIS HUMAN ANATOMY SAFETY"));
            assertFalse(role+" no triple",copied.contains("NOVARIS x3 OUTPUT"));
        }
        activity.finish();
    }

    @Test public void regionalAndSpecialFormLifestyleButtonsCopyRealX1Prompts() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        java.io.InputStream stream=context.getAssets().open("novaris_original_variants.json");
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        byte[] buffer=new byte[8192];int n;
        while((n=stream.read(buffer))!=-1)bytes.write(buffer,0,n);
        stream.close();
        org.json.JSONObject root=new org.json.JSONObject(bytes.toString("UTF-8"));
        org.json.JSONArray assignments=root.getJSONObject("V2713_ASSIGNMENTS").getJSONArray("190");
        android.content.Intent intent=new android.content.Intent(context,DetailActivity.class);
        intent.putExtra("dex",190);
        DetailActivity activity=org.robolectric.Robolectric.buildActivity(DetailActivity.class,intent).setup().get();
        Method render=DetailActivity.class.getDeclaredMethod("addFormClassVariants",
            int.class,int.class,String.class,String.class,String.class,int.class);
        render.setAccessible(true);
        Field bodyField=DetailActivity.class.getDeclaredField("body");
        bodyField.setAccessible(true);
        android.widget.LinearLayout body=(android.widget.LinearLayout)bodyField.get(activity);
        for(String form:new String[]{"Hisui","Alola","Galar","Paldea","Mega","Gigantamax"}) {
            int before=body.getChildCount();
            render.invoke(activity,190,1,form,"","TEST FORM BLUEPRINT",before);
            assertTrue(form+" rendered section",body.getChildCount()>before);
            android.view.View section=body.getChildAt(before);
            android.widget.Button toggle=findButton(section,"Optionale Klassen & Varianten · 6  ▾");
            assertNotNull(form+" toggle",toggle);
            assertTrue(form+" open",toggle.performClick());
            for(int i=3;i<6;i++) {
                String role=assignments.getString(i);
                android.widget.Button button=findButton(section,role+" x1");
                assertNotNull(form+"/"+role+" button",button);
                assertTrue(form+"/"+role+" click",button.performClick());
                android.content.ClipboardManager clipboard=(android.content.ClipboardManager)
                    activity.getSystemService(Context.CLIPBOARD_SERVICE);
                String copied=clipboard.getPrimaryClip().getItemAt(0).coerceToText(activity).toString();
                assertTrue(form+"/"+role+" identity",copied.contains("MANDATORY FORM IDENTITY: "+form));
                assertTrue(form+"/"+role+" blueprint",copied.contains("TEST FORM BLUEPRINT"));
                assertEquals(form+"/"+role+" safety",1,count(copied,"NOVARIS HUMAN ANATOMY SAFETY"));
                assertFalse(form+"/"+role+" no x3",copied.contains("NOVARIS x3 OUTPUT"));
            }
        }
        activity.finish();
    }

    @Test public void savedEditedJobMainMegaAndGigantamaxButtonsCopyX1() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        String key="190";
        String edited="NOVARIS TEST ARTIFICER";
        String description="NOVARIS SAVED TEST JOB: adult human artificer, tailored copper workwear, two human arms and two human legs.";
        context.getSharedPreferences("novaris_job_edits",Context.MODE_PRIVATE).edit().putString(key,edited).commit();
        context.getSharedPreferences("novaris_job_descriptions",Context.MODE_PRIVATE).edit().putString(key,description).commit();
        context.getSharedPreferences("novaris_job_structured",Context.MODE_PRIVATE).edit()
            .putString(key,"{\"group\":\"Civilian\",\"class_family\":\"Artificer\",\"tier\":\"T4\"}").commit();
        try {
            android.content.Intent intent=new android.content.Intent(context,DetailActivity.class);
            intent.putExtra("dex",190);
            DetailActivity activity=org.robolectric.Robolectric.buildActivity(DetailActivity.class,intent).setup().get();
            DatabaseManager data=new DatabaseManager(context);
            DatabaseManager.PokemonDetail detail=data.pokemonDetail(190);
            assertNotNull("Edited job species",detail);
            Method render=DetailActivity.class.getDeclaredMethod("addChangedJobBlock",
                DatabaseManager.PokemonItem.class,String.class);
            render.setAccessible(true);
            Field bodyField=DetailActivity.class.getDeclaredField("body");
            bodyField.setAccessible(true);
            android.widget.LinearLayout body=(android.widget.LinearLayout)bodyField.get(activity);
            int before=body.getChildCount();
            render.invoke(activity,detail.item,
                "[{\"name\":\"Mega\",\"url\":\"\"},{\"name\":\"Gigantamax\",\"url\":\"\"}]");
            assertTrue("Edited job block rendered",body.getChildCount()>before);
            assertNotNull("Edited job title",findButton(body,"Copy image · geänderte Klasse"));
            for(String form:new String[]{"","Mega","Gigantamax"}) {
                String label=form.isEmpty()?"Copy Core + Dex x1":"Copy Core + Dex x1 · "+form;
                android.widget.Button button=findLastButton(body,label);
                assertNotNull("Edited "+form+" button",button);
                assertTrue("Edited "+form+" enabled",button.isEnabled());
                assertTrue("Edited "+form+" click",button.performClick());
                android.content.ClipboardManager clipboard=(android.content.ClipboardManager)
                    activity.getSystemService(Context.CLIPBOARD_SERVICE);
                String copied=clipboard.getPrimaryClip().getItemAt(0).coerceToText(activity).toString();
                assertTrue("Edited "+form+" saved description",copied.contains("NOVARIS SAVED TEST JOB"));
                if(!form.isEmpty()) {
                    assertTrue(form+" form context",copied.contains("SPECIAL FORM: "+form));
                    assertTrue(form+" tier",copied.contains("FORM TIER: T4"));
                }
                assertTrue("Edited "+form+" species heading",copied.startsWith("#0190."));
                assertFalse("Edited "+form+" no triple",copied.contains("NOVARIS x3 OUTPUT"));
            }
            activity.finish();
        } finally {
            context.getSharedPreferences("novaris_job_edits",Context.MODE_PRIVATE).edit().remove(key).commit();
            context.getSharedPreferences("novaris_job_descriptions",Context.MODE_PRIVATE).edit().remove(key).commit();
            context.getSharedPreferences("novaris_job_structured",Context.MODE_PRIVATE).edit().remove(key).commit();
        }
    }


    @Test public void editedJobWithExistingOutputControlGetsOneSafetyHeader() {
        String description="#0190.Aipom\n\nNOVARIS SAVED JOB\n\nNOVARIS OUTPUT CONTROL:\nOne image.";
        String copied=PromptSafety.enforce(description,false);
        assertEquals("Existing output control must not be duplicated",1,count(copied,"NOVARIS OUTPUT CONTROL"));
        assertEquals("Missing anatomy instructions added",1,count(copied,"NOVARIS HUMAN ANATOMY SAFETY"));
        assertTrue("Edited job description preserved",copied.contains("NOVARIS SAVED JOB"));
        assertFalse("No x3 instructions",copied.contains("NOVARIS x3 OUTPUT"));
    }

    @Test public void realShinyMainButtonCopiesX1ToClipboard() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        android.content.Intent intent=new android.content.Intent(context,DetailActivity.class);
        intent.putExtra("dex",190);
        DetailActivity activity=org.robolectric.Robolectric.buildActivity(DetailActivity.class,intent).setup().get();
        android.widget.Button button=findButton(activity.findViewById(android.R.id.content),"Copy Shiny + Core + Dex x1");
        assertNotNull("Actual Shiny main x1 button",button);
        assertTrue("Shiny main click",button.performClick());
        android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
        String copied=clipboard.getPrimaryClip().getItemAt(0).coerceToText(activity).toString();
        assertTrue("Shiny identity",copied.contains("#0190."));
        assertEquals("Shiny color authority",1,count(copied,"SHINY REFERENCE IMAGE — MANDATORY COLOR AUTHORITY"));
        assertEquals("Shiny anatomy",1,count(copied,"NOVARIS HUMAN ANATOMY SAFETY"));
        assertEquals("Shiny output",1,count(copied,"NOVARIS OUTPUT CONTROL"));
        assertFalse("No x3",copied.contains("NOVARIS x3 OUTPUT"));
        activity.finish();
    }

    @Test public void storedPokemonFormsRenderRealX1Buttons() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        DatabaseManager data=new DatabaseManager(context);
        DatabaseManager.PokemonDetail detail=data.pokemonDetail(6);
        assertNotNull("Stored species",detail);
        org.json.JSONArray forms=new org.json.JSONArray(detail.formsJson);
        assertTrue("Stored forms required for end-to-end coverage",forms.length()>0);
        android.content.Intent intent=new android.content.Intent(context,DetailActivity.class);
        intent.putExtra("dex",6);
        DetailActivity activity=org.robolectric.Robolectric.buildActivity(DetailActivity.class,intent).setup().get();
        android.view.View root=activity.findViewById(android.R.id.content);
        int checked=0;
        for(int i=0;i<forms.length();i++) {
            org.json.JSONObject form=forms.optJSONObject(i);
            if(form==null)continue;
            String name=form.optString("name","Form");
            android.widget.Button button=findButton(root,"Copy "+name+" + Core + Dex x1");
            assertNotNull(name+" stored form x1 button",button);
            assertTrue(name+" stored form click",button.performClick());
            android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
            String copied=clipboard.getPrimaryClip().getItemAt(0).coerceToText(activity).toString();
            assertTrue(name+" form identity",copied.contains(name));
            assertFalse(name+" no triple",copied.contains("NOVARIS x3 OUTPUT"));
            checked++;
        }
        assertEquals("Every stored form x1 button exercised",forms.length(),checked);
        activity.finish();
    }

    @Test public void catalogUiSelectionPersistsAndCopiesEditedX1() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        String key="190";
        try {
            context.getSharedPreferences("novaris_job_edits",Context.MODE_PRIVATE).edit().remove(key).commit();
            android.content.Intent intent=new android.content.Intent(context,DetailActivity.class);
            intent.putExtra("dex",190);
            DetailActivity activity=org.robolectric.Robolectric.buildActivity(DetailActivity.class,intent).setup().get();
            android.widget.Button catalog=findButton(activity.findViewById(android.R.id.content),"Klasse aus V27.42-Katalog auswählen");
            assertNotNull("Catalog UI entry",catalog);
            assertTrue("Open catalog",catalog.performClick());
            android.app.AlertDialog groups=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull("Catalog group dialog",groups);
            groups.getListView().performItemClick(groups.getListView().getChildAt(1),1,groups.getListView().getAdapter().getItemId(1));
            android.app.AlertDialog jobs=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull("Catalog jobs dialog",jobs);
            jobs.getListView().performItemClick(jobs.getListView().getChildAt(0),0,jobs.getListView().getAdapter().getItemId(0));
            String selected=context.getSharedPreferences("novaris_job_edits",Context.MODE_PRIVATE).getString(key,"");
            assertFalse("Catalog selection persisted",selected.isEmpty());
            assertTrue("Structured job persisted",context.getSharedPreferences("novaris_job_structured",Context.MODE_PRIVATE).contains(key));
            android.widget.Button copy=findLastButton(activity.findViewById(android.R.id.content),"Copy Core + Dex x1");
            assertNotNull("Edited class x1 button",copy);
            assertTrue("Edited class x1 enabled",copy.isEnabled());
            assertTrue("Edited class x1 click",copy.performClick());
            android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
            String copied=clipboard.getPrimaryClip().getItemAt(0).coerceToText(activity).toString();
            assertTrue("Selected job in prompt",copied.contains(selected));
            assertFalse("No x3",copied.contains("NOVARIS x3 OUTPUT"));
            activity.finish();
        } finally {
            for(String name:new String[]{"novaris_job_edits","novaris_job_structured","novaris_job_descriptions","novaris_job_manual"})
                context.getSharedPreferences(name,Context.MODE_PRIVATE).edit().remove(key).commit();
        }
    }

    @Test public void regionalFormsFromRealPokédexDataCopyX1() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        DatabaseManager data=new DatabaseManager(context);
        int verified=0;
        for(int dex:new int[]{19,27,37,52,74,77,88,103,105,122,144,146,157,194}) {
            DatabaseManager.PokemonDetail detail=data.pokemonDetail(dex);
            if(detail==null)continue;
            org.json.JSONArray forms=new org.json.JSONArray(detail.formsJson);
            java.util.ArrayList<String> regional=new java.util.ArrayList<>();
            for(int i=0;i<forms.length();i++) {
                org.json.JSONObject form=forms.optJSONObject(i);
                if(form==null)continue;
                String name=form.optString("name","");
                String lower=name.toLowerCase(java.util.Locale.ROOT);
                if(lower.contains("alola")||lower.contains("galar")||lower.contains("hisui")||lower.contains("paldea"))
                    regional.add(name);
            }
            if(regional.isEmpty())continue;
            android.content.Intent intent=new android.content.Intent(context,DetailActivity.class);
            intent.putExtra("dex",dex);
            DetailActivity activity=org.robolectric.Robolectric.buildActivity(DetailActivity.class,intent).setup().get();
            android.view.View root=activity.findViewById(android.R.id.content);
            for(String name:regional) {
                android.widget.Button button=findButton(root,"Copy "+name+" + Core + Dex x1");
                assertNotNull(dex+"/"+name+" real regional button",button);
                assertTrue(dex+"/"+name+" click",button.performClick());
                android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
                String copied=clipboard.getPrimaryClip().getItemAt(0).coerceToText(activity).toString();
                assertTrue(dex+"/"+name+" heading",copied.contains(name));
                assertFalse(dex+"/"+name+" no triple",copied.contains("NOVARIS x3 OUTPUT"));
                verified++;
            }
            activity.finish();
        }
        assertTrue("Real stored regional form required",verified>0);
    }

}
