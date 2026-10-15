package com.novaris.pokedex.nativeapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageLoader {
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private ImageLoader() {}

    public static void load(Context ctx, String url, ImageView view) {
        if(url==null||url.isEmpty()){view.setImageDrawable(null);return;}
        view.setTag(url); view.setImageDrawable(null);
        File cache=new File(ctx.getCacheDir(),"img"); if(!cache.exists())cache.mkdirs();
        File file=new File(cache,hash(url)+".img");
        EXEC.execute(()->{
            try{
                Bitmap bmp;
                if(file.exists()&&file.length()>0) bmp=BitmapFactory.decodeFile(file.getAbsolutePath());
                else {
                    HttpURLConnection con=(HttpURLConnection)new URL(url).openConnection();con.setConnectTimeout(10000);con.setReadTimeout(15000);con.setInstanceFollowRedirects(true);con.connect();
                    try(InputStream in=con.getInputStream();OutputStream out=new FileOutputStream(file)){byte[] buf=new byte[65536];int n;while((n=in.read(buf))>0)out.write(buf,0,n);} finally {con.disconnect();}
                    bmp=BitmapFactory.decodeFile(file.getAbsolutePath());
                }
                Bitmap finalBmp=bmp; MAIN.post(()->{if(url.equals(view.getTag()))view.setImageBitmap(finalBmp);});
            }catch(Exception ignored){}
        });
    }

    private static String hash(String s){try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] b=md.digest(s.getBytes());StringBuilder x=new StringBuilder();for(byte v:b)x.append(String.format("%02x",v));return x.toString();}catch(Exception e){return Integer.toHexString(s.hashCode());}}
}
