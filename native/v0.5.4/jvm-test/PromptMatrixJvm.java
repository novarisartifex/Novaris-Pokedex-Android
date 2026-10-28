import android.content.Context;
import com.novaris.pokedex.nativeapp.OriginalVariantEngine;
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
          for(boolean triple:new boolean[]{false,true}){
            JSONObject local=new JSONObject(root.toString());
            if(shiny){
              JSONObject species=local.getJSONObject("SPECIES").getJSONObject(Integer.toString(dex));
              species.put("text","NOVARIS SHINY COLOR AUTHORITY: Selected Shiny reference controls every color. "+
                "Do not import the main profession.\nSPECIES IDENTITY: "+species.getString("name"));
            }
            String prompt=OriginalVariantEngine.build(context,local,dex,role,triple);
            String id=dex+"/"+role+"/"+(shiny?"Shiny":"Normal")+"/"+(triple?"x3":"x1");
            require(occurrences(prompt,"NOVARIS OUTPUT CONTROL")==1,id+" output header");
            require(occurrences(prompt,"NOVARIS HUMAN ANATOMY SAFETY")==1,id+" anatomy");
            require(occurrences(prompt,"NOVARIS LIFESTYLE — SPECIES-SPECIFIC STORED BLUEPRINT")==1,id+" blueprint");
            require(occurrences(prompt,"NOVARIS x3 OUTPUT")== (triple?1:0),id+" count");
            require(occurrences(prompt,"SHINY LIFESTYLE PALETTE OVERRIDE")== (shiny?1:0),id+" shiny override");
            require(prompt.contains("VARIANT: "+role),id+" class");
            require(prompt.contains("IMAGE "+(triple?3:1)+":"),id+" image count");
            require(!prompt.contains("IMAGE "+(triple?4:2)+":"),id+" excess image");
            require(occurrences(prompt,"NOVARIS_CORE_END:")<=1,id+" core end marker");
            verified++;
          }
    System.out.println("Executed "+verified+" REAL Java lifestyle prompt assemblies; 16 main-class cases remain untested.");
  }
}
