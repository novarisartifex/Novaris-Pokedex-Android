package com.novaris.pokedex.nativeapp;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;

/** Native GitHub Release APK update, independent of the existing Pokédex data updater. */
public final class AppReleaseUpdater {
    private static final String API="https://api.github.com/repos/novarisartifex/Novaris-Pokedex-Android/releases/latest";
    private final Activity activity;
    private boolean added;
    private boolean checking;
    private long lastAutoCheck;
    private String lastPromptedBuild="";
    private static final long AUTO_INTERVAL_MS=60_000L;
    public AppReleaseUpdater(Activity a){activity=a;}
    public void attach(){
        if(added)return;
        View root=activity.getWindow().getDecorView();
        if(!(root instanceof android.widget.FrameLayout))return;
        android.widget.FrameLayout parent=(android.widget.FrameLayout)root;
        Button button=new Button(activity);
        button.setText("App-Update");
        button.setAllCaps(false);
        android.widget.FrameLayout.LayoutParams lp=new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.BOTTOM|android.view.Gravity.END);
        int densityBottom=(int)(activity.getResources().getDisplayMetrics().density*16);
        android.view.WindowInsets insets=parent.getRootWindowInsets();
        int navBottom=insets==null?0:insets.getSystemWindowInsetBottom();
        lp.setMargins(8,8,16,navBottom+densityBottom);
        android.view.View content=parent.findViewById(android.R.id.content);
        if(content!=null){
            int extra=(int)(activity.getResources().getDisplayMetrics().density*72);
            content.setPadding(content.getPaddingLeft(),content.getPaddingTop(),
                content.getPaddingRight(),content.getPaddingBottom()+navBottom+extra);
        }
        parent.addView(button,lp);
        button.setOnClickListener(v->check());
        added=true;
        onAppResume();
    }
    public void onAppResume(){
        long now=android.os.SystemClock.elapsedRealtime();
        if(now-lastAutoCheck<AUTO_INTERVAL_MS)return;
        lastAutoCheck=now;
        check(false);
    }
    public void check(){check(true);}
    private synchronized void check(boolean manual){
        if(checking)return;
        checking=true;
        if(manual)Toast.makeText(activity,"Prüfe App-Release …",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            try{
                HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection();
                c.setConnectTimeout(12000);c.setReadTimeout(12000);
                c.setRequestProperty("Accept","application/vnd.github+json");
                c.setRequestProperty("User-Agent","Novaris-Pokedex-Native");
                if(c.getResponseCode()!=200)throw new IOException("GitHub HTTP "+c.getResponseCode());
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                try(InputStream in=c.getInputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}
                JSONObject release=new JSONObject(out.toString("UTF-8"));
                String tag=release.getString("tag_name");
                String published=release.optString("published_at",release.optString("created_at",""));
                JSONArray assets=release.getJSONArray("assets");
                String url="";
                String assetTime="";
                for(int i=0;i<assets.length();i++){
                    JSONObject asset=assets.getJSONObject(i);
                    if(asset.getString("name").endsWith(".apk")){url=asset.getString("browser_download_url");assetTime=asset.optString("updated_at",asset.optString("created_at",""));break;}
                }
                if(url.isEmpty())throw new IOException("Release ohne APK");
                android.content.pm.PackageInfo info=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);
                String installed=info.versionName;
                long installedCode=android.os.Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
                String body=release.optString("body","");
                java.util.regex.Matcher codeMatcher=java.util.regex.Pattern.compile("NOVARIS_VERSION_CODE=(\\d+)").matcher(body);
                long remoteCode=codeMatcher.find()?Long.parseLong(codeMatcher.group(1)):-1L;
                // Release version code is authoritative; never use upload timestamps to infer installation.
                boolean newer=remoteCode>=0?remoteCode>installedCode:compare(tag,installed)>0;
                final String apkUrl=url;
                final String display=tag+" (Build "+(remoteCode>=0?remoteCode:"unbekannt")+")";
                final String buildKey=tag+"/"+remoteCode;
                activity.runOnUiThread(()->{
                    synchronized(AppReleaseUpdater.this){checking=false;}
                    if(!newer){
                        if(manual)new AlertDialog.Builder(activity).setTitle("App aktuell")
                            .setMessage("Installiert: "+installed+" (Build "+installedCode+")\\nGitHub: "+display)
                            .setPositiveButton("OK",null).show();
                        return;
                    }
                    if(!manual && buildKey.equals(lastPromptedBuild))return;
                    lastPromptedBuild=buildKey;
                    new AlertDialog.Builder(activity).setTitle("App-Update verfügbar")
                        .setMessage("Installiert: "+installed+" (Build "+installedCode+")\\nNeu: "+display+"\\nAPK herunterladen und Android-Installation starten?")
                        .setNegativeButton("Abbrechen",null)
                        .setPositiveButton("Installieren",(d,w)->download(apkUrl)).show();
                });
            }catch(Exception e){activity.runOnUiThread(()->{synchronized(AppReleaseUpdater.this){checking=false;}if(manual)new AlertDialog.Builder(activity).setTitle("App-Update fehlgeschlagen").setMessage(e.getMessage()).setPositiveButton("OK",null).show();});}
        }).start();
    }
    private static int[] numbers(String s){
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(\\d+)").matcher(s);
        int[] a={0,0,0};for(int i=0;i<3&&m.find();i++)a[i]=Integer.parseInt(m.group(1));return a;
    }
    private static int compare(String release,String installed){
        int[] a=numbers(release),b=numbers(installed);
        for(int i=0;i<3;i++)if(a[i]!=b[i])return Integer.compare(a[i],b[i]);
        return 0;
    }
    private void download(String url){
        try{
            DownloadManager manager=(DownloadManager)activity.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request req=new DownloadManager.Request(Uri.parse(url));
            req.setTitle("Novaris Pokédex App-Update");
            req.setDescription("Android APK");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalFilesDir(activity,Environment.DIRECTORY_DOWNLOADS,"Novaris-Pokedex-Update.apk");
            long id=manager.enqueue(req);
            Toast.makeText(activity,"APK wird heruntergeladen",Toast.LENGTH_LONG).show();
            Handler h=new Handler(Looper.getMainLooper());
            Runnable poll=new Runnable(){
                int attempts=0;
                public void run(){
                    if(activity.isFinishing()||attempts++>240){if(attempts>240)Toast.makeText(activity,"APK-Download dauert zu lange. Bitte Downloads prüfen.",Toast.LENGTH_LONG).show();return;}
                    DownloadManager.Query q=new DownloadManager.Query().setFilterById(id);
                    try(android.database.Cursor c=manager.query(q)){
                        if(c!=null&&c.moveToFirst()){
                            int status=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                            if(status==DownloadManager.STATUS_SUCCESSFUL){
                                Uri uri=manager.getUriForDownloadedFile(id);
                                Intent install=new Intent(Intent.ACTION_INSTALL_PACKAGE);
                                install.setDataAndType(uri,"application/vnd.android.package-archive");
                                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
                                try{activity.startActivity(install);}
                                catch(Exception e){new AlertDialog.Builder(activity).setMessage("APK heruntergeladen. Bitte über Downloads öffnen. "+e.getMessage()).setPositiveButton("OK",null).show();}
                                return;
                            }
                            if(status==DownloadManager.STATUS_FAILED){Toast.makeText(activity,"APK-Download fehlgeschlagen",Toast.LENGTH_LONG).show();return;}
                        }
                    }
                    h.postDelayed(this,1000);
                }
            };
            h.postDelayed(poll,1000);
        }catch(Exception e){new AlertDialog.Builder(activity).setMessage(e.getMessage()).setPositiveButton("OK",null).show();}
    }
}
