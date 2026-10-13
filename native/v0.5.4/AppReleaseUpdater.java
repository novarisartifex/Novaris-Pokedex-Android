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
        lp.setMargins(8,8,16,16);
        parent.addView(button,lp);
        button.setOnClickListener(v->check());
        added=true;
    }
    public void check(){
        Toast.makeText(activity,"Prüfe App-Release …",Toast.LENGTH_SHORT).show();
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
                JSONArray assets=release.getJSONArray("assets");
                String url="";
                for(int i=0;i<assets.length();i++){
                    JSONObject asset=assets.getJSONObject(i);
                    if(asset.getString("name").endsWith(".apk")){url=asset.getString("browser_download_url");break;}
                }
                if(url.isEmpty())throw new IOException("Release ohne APK");
                String installed=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName;
                final String apkUrl=url;
                activity.runOnUiThread(()->{
                    if(compare(tag,installed)<=0){
                        new AlertDialog.Builder(activity).setTitle("App aktuell").setMessage("Installiert: "+installed+"\nGitHub: "+tag).setPositiveButton("OK",null).show();
                    }else{
                        new AlertDialog.Builder(activity).setTitle("App-Update verfügbar")
                            .setMessage("Installiert: "+installed+"\nNeu: "+tag+"\nAPK herunterladen und Android-Installation starten?")
                            .setNegativeButton("Abbrechen",null).setPositiveButton("Installieren",(d,w)->download(apkUrl)).show();
                    }
                });
            }catch(Exception e){activity.runOnUiThread(()->new AlertDialog.Builder(activity).setTitle("App-Update fehlgeschlagen").setMessage(e.getMessage()).setPositiveButton("OK",null).show());}
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
                    if(activity.isFinishing()||attempts++>240)return;
                    DownloadManager.Query q=new DownloadManager.Query().setFilterById(id);
                    try(android.database.Cursor c=manager.query(q)){
                        if(c!=null&&c.moveToFirst()){
                            int status=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                            if(status==DownloadManager.STATUS_SUCCESSFUL){
                                Uri uri=manager.getUriForDownloadedFile(id);
                                Intent install=new Intent(Intent.ACTION_VIEW);
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
