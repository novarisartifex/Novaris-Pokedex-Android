import android.content.Context;
import com.novaris.pokedex.nativeapp.OriginalVariantEngine;
import com.novaris.pokedex.nativeapp.PromptSafety;
import org.json.JSONObject;
import java.nio.file.*;
public final class PromptMatrixJvm {
  private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
  private static int occurrences(String text,String needle){int n=0,p=0;while((p=text.indexOf(needle,p))>=0){n++;p+=needle.length();}return n;}
  public static void main(String[] args)throws Exception{
    Path assets=Paths.get(args[0]);
    JSONObject root=new JSONObject(Files.readString(assets.resolve("novaris_original_variants.json")));
    Context context=new Context(assets.toString());
    int verified=0;
    for(int dex:new int[]{190,324,332,338})
      for(String role:new String[]{"Casual","Nightlife","Lingerie"})
        for(boolean shiny:new boolean[]{false,true})
          for(boolean triple:new boolean[]{false}){
            JSONObject local=new JSONObject(root.toString());
            if(shiny){
              JSONObject species=local.getJSONObject("SPECIES").getJSONObject(Integer.toString(dex));
              species.put("text","NOVARIS SHINY COLOR AUTHORITY: Selected Shiny reference controls every color. "+
                "Do not import the main profession.\nSPECIES IDENTITY: "+species.getString("name"));
            }
            String prompt=OriginalVariantEngine.build(context,local,dex,role,triple);
            String id=dex+"/"+role+"/"+(shiny?"Shiny":"Normal")+"/"+(triple?"x3":"x1");
            require(PromptSafety.enforce(prompt,triple).equals(prompt),id+" safety idempotence");
            require(occurrences(prompt,"NOVARIS OUTPUT CONTROL")==1,id+" output header");
            require(occurrences(prompt,"NOVARIS HUMAN ANATOMY SAFETY")==1,id+" anatomy");
            require(occurrences(prompt,"NOVARIS LIFESTYLE — SPECIES-SPECIFIC STORED BLUEPRINT")==1,id+" blueprint");
            require(occurrences(prompt,"NOVARIS x3 OUTPUT")== (triple?1:0),id+" count");
            require(occurrences(prompt,"SHINY LIFESTYLE PALETTE OVERRIDE")== (shiny?1:0),id+" shiny override");
            require(prompt.contains("VARIANT: "+role),id+" class");
            if(triple){
              require(occurrences(prompt,"NOVARIS OUTPUT MODE — TRIPLE SELECTION")==1,id+" main x3 mode");
              require(prompt.contains("THREE INDEPENDENT INTERPRETATIONS OF THE SAME LIFESTYLE BRIEF"),id+" one lifestyle brief");
              require(occurrences(prompt,"LIFESTYLE BRIEF:")==1,id+" one unnumbered lifestyle brief");
              require(!prompt.contains("IMAGE 1:"),id+" no misleading single-image label");
              require(prompt.contains("selected lifestyle class") && prompt.contains("specified lifestyle scene action"),id+" lifestyle-specific orchestration");
              require(!prompt.contains("IMAGE 2:")&&!prompt.contains("IMAGE 3:"),id+" no other concepts");
              require(!prompt.contains("CONCEPT 2:")&&!prompt.contains("CONCEPT 3:"),id+" no additional concepts");
              require(!prompt.contains("LIFESTYLE ISOLATION:")&&!prompt.contains("NOVARIS LIFESTYLE x3 —"),id+" no extra orchestration");
            }else{
              require(prompt.contains("IMAGE 1:"),id+" single image");
              require(!prompt.contains("IMAGE 2:"),id+" excess image");
            }
            require(occurrences(prompt,root.getString("NOVARIS_CORE_V117"))==1,id+" core exactly once");
            verified++;
          }
    require(verified==24,"Expected 4 species x 3 lifestyle classes x 2 palettes = 24 x1 prompts");
    System.out.println("Executed "+verified+" REAL Java x1 lifestyle prompt assemblies.");
  }
}
