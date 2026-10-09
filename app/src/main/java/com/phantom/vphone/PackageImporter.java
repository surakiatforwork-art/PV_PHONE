package com.phantom.vphone;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.*;
import java.util.*;
import java.util.zip.*;
final class PackageImporter {
 static final long MAX_TOTAL=1200L*1024*1024;
 static File root(Context c){File f=new File(c.getFilesDir(),"imported_packages");if(!f.exists())f.mkdirs();return f;}
 static String installed(Context c,AppEntry app)throws Exception{
  ApplicationInfo ai=c.getPackageManager().getApplicationInfo(app.packageName,0);
  File tmp=temp(c),dest=new File(root(c),safe(app.packageName));
  try{
   long total=copy(new FileInputStream(ai.sourceDir),new File(tmp,"base.apk"),MAX_TOTAL);
   if(ai.splitSourceDirs!=null)for(int i=0;i<ai.splitSourceDirs.length;i++)total+=copy(new FileInputStream(ai.splitSourceDirs[i]),new File(tmp,"split_"+i+".apk"),MAX_TOTAL-total);
   replace(tmp,dest);return app.packageName+" ("+(1+(ai.splitSourceDirs==null?0:ai.splitSourceDirs.length))+" APK parts)";
  }finally{if(tmp.exists())delete(tmp);}
 }
 static String document(Context c,Uri uri)throws Exception{
  String name=fileName(c,uri);String lower=name.toLowerCase(Locale.ROOT);File tmp=temp(c),raw=new File(tmp,"input.bin");
  try{
   copy(c.getContentResolver().openInputStream(uri),raw,MAX_TOTAL);
   if(lower.endsWith(".apk")){File base=new File(tmp,"base.apk");if(!raw.renameTo(base))throw new IOException("Cannot prepare APK");}
   else if(lower.endsWith(".apks")||lower.endsWith(".apkm")||lower.endsWith(".xapk")||lower.endsWith(".apk+")){extractArchive(raw,tmp);raw.delete();}
   else throw new IOException("Supported: APK, APKS, APKM, XAPK, APK+");
   File base=findBase(c,tmp);if(base==null)throw new IOException("No valid base APK found");
   if(!base.getName().equals("base.apk")){File normalized=new File(tmp,"base.apk");if(normalized.exists())normalized.delete();if(!base.renameTo(normalized))throw new IOException("Cannot normalize base APK");base=normalized;}
   PackageInfo info=c.getPackageManager().getPackageArchiveInfo(base.getAbsolutePath(),0);if(info==null||info.packageName==null)throw new IOException("Invalid base APK");
   File dest=new File(root(c),safe(info.packageName));replace(tmp,dest);return info.packageName;
  }finally{if(tmp.exists())delete(tmp);}
 }
 private static void extractArchive(File raw,File dir)throws Exception{
  int count=0;long total=0;
  try(ZipFile z=new ZipFile(raw)){Enumeration<? extends ZipEntry> es=z.entries();while(es.hasMoreElements()){
   ZipEntry e=es.nextElement();if(e.isDirectory()||!e.getName().toLowerCase(Locale.ROOT).endsWith(".apk"))continue;
   if(++count>150)throw new IOException("Too many APK parts");
   String n=e.getName().substring(e.getName().lastIndexOf('/')+1);if(n.isEmpty())n="part_"+count+".apk";
   File out=new File(dir,n);if(out.exists())out=new File(dir,"part_"+count+".apk");
   total+=copy(z.getInputStream(e),out,MAX_TOTAL-total);
  }}catch(ZipException e){throw new IOException("APK+ is not a ZIP-based package supported by this build",e);}
  if(count==0)throw new IOException("Archive contains no APK parts");
 }
 private static File findBase(Context c,File dir){
  File direct=new File(dir,"base.apk");if(direct.exists()&&c.getPackageManager().getPackageArchiveInfo(direct.getAbsolutePath(),0)!=null)return direct;
  File[] fs=dir.listFiles((f,n)->n.toLowerCase(Locale.ROOT).endsWith(".apk"));if(fs==null)return null;
  for(File f:fs){PackageInfo p=c.getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(),0);if(p!=null&&p.applicationInfo!=null&&p.applicationInfo.splitName==null)return f;}return null;
 }
 private static long copy(InputStream in,File out,long remaining)throws IOException{
  if(remaining<=0)throw new IOException("Package exceeds size limit");
  long total=0;try(InputStream src=in;OutputStream dst=new FileOutputStream(out)){if(src==null)throw new IOException("Cannot read input");byte[] b=new byte[65536];int n;while((n=src.read(b))!=-1){total+=n;if(total>remaining)throw new IOException("Package exceeds size limit");dst.write(b,0,n);}}return total;
 }
 private static File temp(Context c)throws IOException{File f=new File(root(c),".tmp_"+System.nanoTime());if(!f.mkdirs())throw new IOException("Cannot create temp directory");return f;}
 private static void replace(File from,File to)throws IOException{if(to.exists())delete(to);if(!from.renameTo(to))throw new IOException("Cannot store imported package");}
 static void delete(File f){if(f.isDirectory()){File[] cs=f.listFiles();if(cs!=null)for(File c:cs)delete(c);}f.delete();}
 private static String safe(String s){return s.replaceAll("[^a-zA-Z0-9._-]","_");}
 private static String fileName(Context c,Uri u){String n=null;Cursor cur=c.getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null);if(cur!=null){try{if(cur.moveToFirst())n=cur.getString(0);}finally{cur.close();}}return n==null?"package":n;}
}