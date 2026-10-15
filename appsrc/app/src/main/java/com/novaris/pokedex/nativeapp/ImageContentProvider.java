package com.novaris.pokedex.nativeapp;

import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.*;

public class ImageContentProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    private File file(){return new File(getContext().getCacheDir(),"clipboard/novaris-image.png");}
    @Override public String getType(Uri uri){return "image/png";}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {return ParcelFileDescriptor.open(file(),ParcelFileDescriptor.MODE_READ_ONLY);}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder){MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});File f=file();c.addRow(new Object[]{"novaris-image.png",f.length()});return c;}
    @Override public int delete(Uri uri,String s,String[] a){return 0;} @Override public int update(Uri uri,ContentValues v,String s,String[] a){return 0;} @Override public Uri insert(Uri uri,ContentValues v){return null;}
}
