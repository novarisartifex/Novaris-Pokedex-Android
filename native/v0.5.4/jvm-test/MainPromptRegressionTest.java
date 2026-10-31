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
            android.widget.Button button=findButton(activity.findViewById(android.R.id.content),"Copy "+role+" x1");
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

}
