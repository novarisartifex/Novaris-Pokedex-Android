package android.content;
import android.content.res.AssetManager;
public class Context {
  private final AssetManager assets;
  public Context(String path){assets=new AssetManager(path);}
  public AssetManager getAssets(){return assets;}
}
