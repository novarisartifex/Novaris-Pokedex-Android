package android.content.res;
import java.io.*;
import java.nio.file.*;
public class AssetManager {
  private final Path root;
  public AssetManager(String path){root=Paths.get(path);}
  public InputStream open(String file) throws IOException {return Files.newInputStream(root.resolve(file));}
}
