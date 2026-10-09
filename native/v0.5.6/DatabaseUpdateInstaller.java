package com.novaris.pokedex.nativeapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.util.zip.GZIPInputStream;

public final class DatabaseUpdateInstaller {
    private DatabaseUpdateInstaller() {}
    private static final long MAX_UNPACKED = 250L * 1024L * 1024L;

    public static String install(Context context, DatabaseManager current, File archive, int expectedPokemon) throws Exception {
        File active = new File(context.getFilesDir(), "novaris-pokedex-v2.db");
        File staged = new File(context.getFilesDir(), "novaris-pokedex-v2.db.new");
        File backup = new File(context.getFilesDir(), "novaris-pokedex-v2.db.bak");
        if (staged.exists() && !staged.delete()) throw new IOException("Cannot clear staging");
        try {
            long bytes = 0;
            try (InputStream input = new GZIPInputStream(new BufferedInputStream(new FileInputStream(archive)));
                 FileOutputStream output = new FileOutputStream(staged)) {
                byte[] buffer = new byte[65536]; int n;
                while ((n = input.read(buffer)) != -1) {
                    bytes += n;
                    if (bytes > MAX_UNPACKED) throw new IOException("Database exceeds limit");
                    output.write(buffer, 0, n);
                }
                output.getFD().sync();
            }
            String version;
            SQLiteDatabase test = SQLiteDatabase.openDatabase(staged.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            try {
                try (Cursor c = test.rawQuery("PRAGMA integrity_check", null)) {
                    if (!c.moveToFirst() || !"ok".equalsIgnoreCase(c.getString(0))) throw new IOException("SQLite integrity check failed");
                }
                try (Cursor c = test.rawQuery("SELECT COUNT(*) FROM pokemon", null)) {
                    if (!c.moveToFirst() || c.getInt(0) != expectedPokemon) throw new IOException("Pokemon count mismatch");
                }
                try (Cursor c = test.rawQuery("SELECT value FROM metadata WHERE key='source_version'", null)) {
                    if (!c.moveToFirst()) throw new IOException("Missing source version");
                    version = c.getString(0);
                }
                try (Cursor c = test.rawQuery("SELECT dex,label,en_name,de_name,generation,artwork_url,career_role,career_category,career_tier,job_family,changed FROM pokemon LIMIT 1", null)) {
                    if (!c.moveToFirst()) throw new IOException("Incompatible Pokemon schema");
                }
            } finally { test.close(); }
            current.close();
            if (backup.exists() && !backup.delete()) throw new IOException("Cannot clear backup");
            if (active.exists() && !active.renameTo(backup)) throw new IOException("Cannot back up current database");
            try {
                if (!staged.renameTo(active)) throw new IOException("Cannot activate database");
                DatabaseManager replacement = new DatabaseManager(context);
                try {
                    replacement.metadata("source_version");
                } finally { replacement.close(); }
                if (backup.exists()) backup.delete();
                return version;
            } catch (Exception error) {
                if (active.exists()) active.delete();
                if (backup.exists() && !backup.renameTo(active)) throw new IOException("Database rollback failed", error);
                throw error;
            }
        } finally {
            staged.delete();
            archive.delete();
        }
    }
}
