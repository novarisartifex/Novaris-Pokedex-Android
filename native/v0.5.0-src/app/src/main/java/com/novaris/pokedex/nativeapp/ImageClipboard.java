package com.novaris.pokedex.nativeapp;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.widget.Toast;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public final class ImageClipboard {
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private ImageClipboard(){}

    public static void copy(Context context,String url){
        if(url==null||url.trim().isEmpty()){Toast.makeText(context,"Kein Bild verfügbar",Toast.LENGTH_SHORT).show();return;}
        Toast.makeText(context,"Bild wird vorbereitet …",Toast.LENGTH_SHORT).show();
        Context app=context.getApplicationContext();
        EXEC.execute(()->{
            try{
                HttpURLConnection con=(HttpURLConnection)new URL(url).openConnection();con.setConnectTimeout(10000);con.setReadTimeout(20000);con.setInstanceFollowRedirects(true);con.connect();
                Bitmap bmp; try(InputStream in=con.getInputStream()){bmp=BitmapFactory.decodeStream(in);} finally {con.disconnect();}
                if(bmp==null)throw new IOException("Bild konnte nicht gelesen werden");
                File dir=new File(app.getCacheDir(),"clipboard");if(!dir.exists())dir.mkdirs();File f=new File(dir,"novaris-image.png");
                try(OutputStream out=new BufferedOutputStream(new FileOutputStream(f))){if(!bmp.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG konnte nicht gespeichert werden");}
                Uri uri=Uri.parse("content://"+app.getPackageName()+".images/novaris-image.png");
                MAIN.post(()->{
                    ClipboardManager cm=(ClipboardManager)app.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip=ClipData.newUri(app.getContentResolver(),"Novaris image",uri);cm.setPrimaryClip(clip);
                    Toast.makeText(app,"Bild kopiert",Toast.LENGTH_SHORT).show();
                });
            }catch(Exception e){MAIN.post(()->Toast.makeText(app,"Bild kopieren fehlgeschlagen",Toast.LENGTH_LONG).show());}
        });
    }
}
