package com.phantom.releaseprobe;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.ParcelFileDescriptor;import java.io.*;
public class CaptureProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 public String getType(Uri uri){return "image/jpeg";}
 public Cursor query(Uri uri,String[] p,String s,String[] a,String o){return null;}
 public Uri insert(Uri u,ContentValues v){return null;}
 public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
 public int delete(Uri u,String s,String[] a){return 0;}
 public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{
  return ParcelFileDescriptor.open(new File(getContext().getFilesDir(),"capture.jpg"),ParcelFileDescriptor.parseMode(mode));
 }
}