package com.phantom.releaseprobe;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.ParcelFileDescriptor;import java.io.*;
public class CaptureProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 public String getType(Uri uri){return "image/jpeg";}
 public Cursor query(Uri uri,String[] p,String s,String[] a,String o){
  android.database.MatrixCursor result=new android.database.MatrixCursor(new String[]{"uid","package"});
  result.addRow(new Object[]{android.os.Binder.getCallingUid(),getCallingPackage()});
  return result;
 }
 public Uri insert(Uri u,ContentValues v){return null;}
 public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
 public int delete(Uri u,String s,String[] a){return 0;}
 public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{
  File target=("external".equals(u.getLastPathSegment())?new File(getContext().getExternalCacheDir(),"browser-uploads/capture.jpg"):new File(getContext().getFilesDir(),"capture.jpg"));
  android.util.Log.i("PHANToM.Capture","fixture target="+target+" parentExists="+target.getParentFile().isDirectory());
  return ParcelFileDescriptor.open(target,ParcelFileDescriptor.parseMode(mode));
 }
}
