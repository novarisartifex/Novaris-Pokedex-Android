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
                for(boolean triple:new boolean[]{false,true}) {
                    String id=dex+"/"+(isShiny?"Shiny":"Normal")+"/"+(triple?"x3":"x1");
                    String prompt=isShiny
                        ? (String)shiny.invoke(activity,dex,"Shiny",triple)
                        : data.buildPokemonPrompt(dex,triple);
                    assertNotNull(id,prompt);
                    assertTrue(id+" empty",prompt.length()>300);
                    assertEquals(id+" anatomy safety",1,count(prompt,"NOVARIS HUMAN ANATOMY SAFETY"));
                    assertEquals(id+" output control",1,count(prompt,"NOVARIS OUTPUT CONTROL"));
                    assertEquals(id+" Style Core end",1,count(prompt,"[NOVARIS_CORE_END:v1.1.8]"));
                    assertEquals(id+" Style Core header",1,count(prompt,"NOVARIS STYLE CORE"));
                    assertEquals(id+" shiny color authority",isShiny?1:0,
                        count(prompt,"SHINY REFERENCE IMAGE — MANDATORY COLOR AUTHORITY"));
                    assertTrue(id+" triple length",!triple||prompt.length()>data.buildPokemonPrompt(dex,false).length());
                    assertFalse(id+" lifestyle contamination",prompt.contains("NOVARIS LIFESTYLE — SPECIES-SPECIFIC STORED BLUEPRINT"));
                    checked++;
                }
            }
        }
        assertEquals(16,checked);
        System.out.println("Executed 16 REAL production DatabaseManager/DetailActivity main prompts");
    }
}
