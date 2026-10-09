package com.novaris.pokedex.nativeapp;

import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class RemoteDataUpdater {
    public static final String MANIFEST_URL = "https://raw.githubusercontent.com/novarisartifex/Novaris-Pokedex-Data/main/manifest.json";
    private RemoteDataUpdater() {}

    public static JSONObject check() throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(MANIFEST_URL).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(20000);
        try {
            if(c.getResponseCode()!=200) throw new IOException("Manifest HTTP "+c.getResponseCode());
            try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buf=new byte[8192]; int n;
                while((n=in.read(buf))!=-1) {
                    if(out.size()+n>131072) throw new IOException("Manifest too large");
                    out.write(buf,0,n);
                }
                JSONObject root=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));
                if(root.getInt("schema_version")!=1 || !"published".equals(root.getString("status"))) throw new IOException("Invalid manifest");
                return root.getJSONObject("latest");
            }
        } finally { c.disconnect(); }
    }

    public static File download(Context context, JSONObject release) throws Exception {
        String url=release.getString("url");
        String expected=release.getString("sha256");
        long size=release.getLong("size_bytes");
        if(!url.startsWith("https://github.com/novarisartifex/Novaris-Pokedex-Data/releases/download/") ||
           !expected.matches("[0-9a-fA-F]{64}") || size<=0 || size>104857600)
            throw new IOException("Invalid release");
        File target=new File(context.getCacheDir(),"novaris-update.pending");
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(20000);
        try {
            if(c.getResponseCode()!=200 || !"https".equalsIgnoreCase(c.getURL().getProtocol()))
                throw new IOException("Download unavailable");
            long total=0;
            try(InputStream in=c.getInputStream(); FileOutputStream out=new FileOutputStream(target)) {
                byte[] buf=new byte[32768]; int n;
                while((n=in.read(buf))!=-1) {
                    total+=n;
                    if(total>size) throw new IOException("Oversized download");
                    hash.update(buf,0,n);
                    out.write(buf,0,n);
                }
                out.getFD().sync();
            }
            StringBuilder actual=new StringBuilder();
            for(byte b:hash.digest()) actual.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            if(total!=size || !expected.equalsIgnoreCase(actual.toString())) throw new IOException("Checksum mismatch");
            return target;
        } catch(Exception e) {
            target.delete();
            throw e;
        } finally { c.disconnect(); }
    }
}
