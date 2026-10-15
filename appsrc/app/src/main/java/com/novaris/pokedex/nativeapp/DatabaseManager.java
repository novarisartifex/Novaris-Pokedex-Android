package com.novaris.pokedex.nativeapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.json.*;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public final class DatabaseManager {
    private final Context context;
    private final File dbFile;
    private SQLiteDatabase db;

    public static final class PokemonItem {
        public int dex, generation, changed;
        public String label, en, de, art, role, category, tier, family;
    }
    public static final class CharacterItem { public String id, name, aliases, source, category, generation, portrait; }
    public static final class ClassItem { public String role, category, tier; public int count; }
    public static final class PokemonDetail { public PokemonItem item; public String cellsJson, formsJson, shinyJson, attrsJson; }
    public static final class CharacterDetail { public CharacterItem item; public String canon, novaris, scenesJson, rawJson, kind, version; }

    public DatabaseManager(Context context) throws IOException {
        this.context = context.getApplicationContext();
        this.dbFile = new File(this.context.getFilesDir(), "novaris-pokedex-v2.db");
        ensureDatabase();
        open();
    }

    private void ensureDatabase() throws IOException {
        if (dbFile.exists() && dbFile.length() > 1024) return;
        File tmp = new File(dbFile.getAbsolutePath() + ".tmp");
        try (InputStream raw = context.getAssets().open("pokedex.db.gzdata");
             GZIPInputStream in = new GZIPInputStream(new BufferedInputStream(raw));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
            byte[] buf = new byte[1024 * 1024]; int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        if (!tmp.renameTo(dbFile)) { copyFile(tmp, dbFile); tmp.delete(); }
    }

    private static void copyFile(File from, File to) throws IOException {
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[1024 * 1024]; int n; while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private synchronized void open() { if (db != null && db.isOpen()) return; db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY); }
    public synchronized void close() { if (db != null && db.isOpen()) db.close(); db = null; }

    public synchronized String metadata(String key) {
        open(); try (Cursor c = db.rawQuery("SELECT value FROM metadata WHERE key=?", new String[]{key})) { return c.moveToFirst() ? c.getString(0) : ""; }
    }

    public synchronized List<PokemonItem> queryPokemon(String search, int generation, Set<Integer> selected, boolean changedOnly, String exactRole) {
        open(); StringBuilder sql = new StringBuilder("SELECT dex,label,en_name,de_name,generation,artwork_url,career_role,career_category,career_tier,job_family,changed FROM pokemon WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (generation > 0) { sql.append(" AND generation=?"); args.add(String.valueOf(generation)); }
        if (changedOnly) sql.append(" AND changed=1");
        if (exactRole != null && !exactRole.isEmpty()) { sql.append(" AND career_role=?"); args.add(exactRole); }
        if (search != null && !search.trim().isEmpty()) { sql.append(" AND search_text LIKE ?"); args.add("%" + search.trim().toLowerCase(Locale.ROOT) + "%"); }
        if (selected != null && !selected.isEmpty()) {
            sql.append(" AND dex IN ("); int i=0; for (Integer ignored : selected) { if (i++>0) sql.append(','); sql.append('?'); } sql.append(')');
            for (Integer d : selected) args.add(String.valueOf(d));
        }
        sql.append(" ORDER BY dex"); List<PokemonItem> out = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]))) { while (c.moveToNext()) out.add(readPokemon(c)); }
        return out;
    }

    public synchronized List<PokemonItem> allPokemonShort() {
        open(); List<PokemonItem> out = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT dex,label,en_name,de_name,generation,artwork_url,career_role,career_category,career_tier,job_family,changed FROM pokemon ORDER BY dex", null)) { while (c.moveToNext()) out.add(readPokemon(c)); }
        return out;
    }

    private static PokemonItem readPokemon(Cursor c) {
        PokemonItem p = new PokemonItem(); p.dex=c.getInt(0); p.label=c.getString(1); p.en=c.getString(2); p.de=c.getString(3); p.generation=c.getInt(4);
        p.art=c.getString(5); p.role=c.getString(6); p.category=c.getString(7); p.tier=c.getString(8); p.family=c.getString(9); p.changed=c.getInt(10); return p;
    }

    public synchronized PokemonDetail pokemonDetail(int dex) {
        open(); try (Cursor c = db.rawQuery("SELECT dex,label,en_name,de_name,generation,artwork_url,career_role,career_category,career_tier,job_family,changed,cells_json,forms_json,shiny_json,attrs_json FROM pokemon WHERE dex=?", new String[]{String.valueOf(dex)})) {
            if (!c.moveToFirst()) return null; PokemonDetail d = new PokemonDetail(); d.item = readPokemon(c); d.cellsJson=c.getString(11); d.formsJson=c.getString(12); d.shinyJson=c.getString(13); d.attrsJson=c.getString(14); return d;
        }
    }

    public synchronized String buildPokemonPrompt(int dex, boolean triple) { return buildPokemonPrompt(dex,triple,"",""); }

    public synchronized String buildPokemonPrompt(int dex, boolean triple, String formName, String formBlueprint) {
        return buildPokemonPromptInternal(dex,triple,formName,formBlueprint,"");
    }

    public synchronized String buildPokemonShinyPrompt(int dex, boolean triple, String shinyName) {
        PokemonDetail d=pokemonDetail(dex); if(d==null)return "";
        String clean=shinyBaseName(shinyName);
        String formName="", blueprint="";
        try {
            JSONArray forms=new JSONArray(d.formsJson);
            for(int i=0;i<forms.length();i++){
                JSONObject f=forms.optJSONObject(i); if(f==null)continue;
                String n=f.optString("name","");
                if(!n.isEmpty() && normalizeVariant(n).equals(normalizeVariant(clean))){formName=n;blueprint=f.optString("blueprint","");break;}
            }
        } catch(Exception ignored) {}
        String variantLabel=(formName!=null&&!formName.isEmpty())?formName:clean;
        String shinyDirective="[SHINY VARIANT OVERRIDE]\n"+
                "This generation uses the Shiny color variant shown in the attached Shiny reference image.\n"+
                "COLOR AUTHORITY: The attached Shiny image overrides the normal species palette wherever colors differ.\n"+
                "Preserve species anatomy, silhouette, markings, form identity, Novaris class/job design, materials, personality, and all non-color traits.\n"+
                "Translate only the visible Shiny color changes into the Novaris character design, including species-derived skin/fur/scales/feathers, hair, eyes, markings, armor accents, materials, and magical glow where appropriate.\n"+
                "Do not revert to the standard Pokémon palette and do not invent additional recolors that are not supported by the Shiny reference.\n"+
                "SHINY VARIANT: "+safe(variantLabel);
        return buildPokemonPromptInternal(dex,triple,formName,blueprint,shinyDirective);
    }

    private synchronized String buildPokemonPromptInternal(int dex, boolean triple, String formName, String formBlueprint, String extraDirective) {
        PokemonDetail d=pokemonDetail(dex); if(d==null)return "";
        String dexText="";
        try {
            JSONObject a=new JSONObject(d.attrsJson);
            dexText=a.optString("context","");
            if(dexText.trim().isEmpty()) dexText=a.optString("transform-context","");
        } catch(Exception ignored) {}
        dexText=dexText.replaceFirst("(?is)\\n?\\[TRANSFORM PROMPT\\]\\s*$","").trim();
        StringBuilder header=new StringBuilder(String.format(Locale.ROOT,"#%04d.%s",d.item.dex,safe(d.item.en)));
        if(d.item.de!=null&&!d.item.de.trim().isEmpty()&&!d.item.de.equalsIgnoreCase(d.item.en))header.append(" / ").append(d.item.de.trim());
        if(formName!=null&&!formName.trim().isEmpty())header.append(" — ").append(formName.trim());
        if(extraDirective!=null&&!extraDirective.trim().isEmpty())header.append(" — Shiny");
        String mode=metadata(triple?"prompt_output_triple":"prompt_output_single");
        String core=metadata("prompt_core");
        StringBuilder out=new StringBuilder();
        out.append(header).append("\n\n").append(dexText);
        if(formBlueprint!=null&&!formBlueprint.trim().isEmpty()) {
            String fv=metadata("prompt_form_variant");
            out.append("\n\n\n").append(fv).append("\n\n\n").append(formBlueprint.trim());
        }
        if(extraDirective!=null&&!extraDirective.trim().isEmpty())out.append("\n\n\n").append(extraDirective.trim());
        out.append("\n\n\n").append(mode).append("\n\n\n").append(core);
        return out.toString();
    }

    private static String shinyBaseName(String s){
        String x=safe(s).trim();
        x=x.replaceAll("(?i)\\s*[—-]\\s*Shiny\\s*$","").trim();
        return x;
    }
    private static String normalizeVariant(String s){return safe(s).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","");}

    public synchronized List<ClassItem> queryClasses(String search) {
        open(); String q = search == null ? "" : search.trim();
        String sql = "SELECT career_role,career_category,career_tier,COUNT(*) FROM pokemon WHERE career_role<>''" +
                (q.isEmpty()?"":" AND (career_role LIKE ? OR career_category LIKE ? OR career_tier LIKE ? OR job_family LIKE ?)") +
                " GROUP BY career_role,career_category,career_tier ORDER BY career_role COLLATE NOCASE";
        String[] args = q.isEmpty()?new String[0]:new String[]{"%"+q+"%","%"+q+"%","%"+q+"%","%"+q+"%"}; List<ClassItem> out = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql,args)) { while(c.moveToNext()) { ClassItem x=new ClassItem(); x.role=c.getString(0); x.category=c.getString(1); x.tier=c.getString(2); x.count=c.getInt(3); out.add(x); } }
        return out;
    }

    public synchronized List<String> characterCategories() { open(); List<String> out=new ArrayList<>(); try(Cursor c=db.rawQuery("SELECT DISTINCT category FROM characters WHERE category<>'' ORDER BY category COLLATE NOCASE",null)) { while(c.moveToNext()) out.add(c.getString(0)); } return out; }

    public synchronized List<CharacterItem> queryCharacters(String search, String generation, String category) {
        open(); StringBuilder sql=new StringBuilder("SELECT id,name,aliases,source,category,generation_key,portrait_url FROM characters WHERE 1=1"); List<String> args=new ArrayList<>();
        if(generation!=null&&!generation.equals("all")){sql.append(" AND generation_key=?");args.add(generation);} if(category!=null&&!category.equals("all")){sql.append(" AND category=?");args.add(category);}
        if(search!=null&&!search.trim().isEmpty()){sql.append(" AND search_text LIKE ?");args.add("%"+search.trim().toLowerCase(Locale.ROOT)+"%");} sql.append(" ORDER BY name COLLATE NOCASE"); List<CharacterItem> out=new ArrayList<>();
        try(Cursor c=db.rawQuery(sql.toString(),args.toArray(new String[0]))){ while(c.moveToNext()){CharacterItem x=new CharacterItem();x.id=c.getString(0);x.name=c.getString(1);x.aliases=c.getString(2);x.source=c.getString(3);x.category=c.getString(4);x.generation=c.getString(5);x.portrait=c.getString(6);out.add(x);} } return out;
    }

    public synchronized CharacterDetail characterDetail(String id) {
        open(); try(Cursor c=db.rawQuery("SELECT id,name,aliases,source,category,generation_key,portrait_url,canon_text,novaris_text,scenes_json,raw_json,kind,version FROM characters WHERE id=?",new String[]{id})){
            if(!c.moveToFirst())return null; CharacterDetail d=new CharacterDetail(); CharacterItem x=new CharacterItem(); x.id=c.getString(0);x.name=c.getString(1);x.aliases=c.getString(2);x.source=c.getString(3);x.category=c.getString(4);x.generation=c.getString(5);x.portrait=c.getString(6);d.item=x;
            d.canon=c.getString(7);d.novaris=c.getString(8);d.scenesJson=c.getString(9);d.rawJson=c.getString(10);d.kind=c.getString(11);d.version=c.getString(12);return d;
        }
    }

    public synchronized String importGzip(Uri uri) throws Exception {
        close(); File tmp = new File(dbFile.getAbsolutePath()+".import");
        try(InputStream raw=context.getContentResolver().openInputStream(uri)){
            if(raw==null)throw new IOException("Datei konnte nicht geöffnet werden"); InputStream in; BufferedInputStream bin=new BufferedInputStream(raw); bin.mark(4); int a=bin.read(), b=bin.read(); bin.reset(); if(a==0x1f&&b==0x8b) in=new GZIPInputStream(bin); else in=bin;
            try(OutputStream out=new BufferedOutputStream(new FileOutputStream(tmp))){byte[] buf=new byte[1024*1024];int n;while((n=in.read(buf))>0)out.write(buf,0,n);} finally {in.close();}
        }
        SQLiteDatabase test=SQLiteDatabase.openDatabase(tmp.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY); String version;
        try(Cursor c=test.rawQuery("SELECT value FROM metadata WHERE key='source_version'",null)){if(!c.moveToFirst())throw new IOException("Keine Novaris-Datenquelle");version=c.getString(0);} try(Cursor c=test.rawQuery("SELECT COUNT(*) FROM pokemon",null)){if(!c.moveToFirst()||c.getInt(0)<1)throw new IOException("Pokédex-Tabelle leer");} test.close();
        File bak=new File(dbFile.getAbsolutePath()+".bak"); if(bak.exists())bak.delete(); if(dbFile.exists())dbFile.renameTo(bak); if(!tmp.renameTo(dbFile)){copyFile(tmp,dbFile);tmp.delete();} if(bak.exists())bak.delete(); open(); return version;
    }

    private static String safe(String s){return s==null?"":s;}
}
