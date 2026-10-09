package com.phantom.vphone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.widget.*;
import android.view.View;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
 private static final int PICK_APK=101, CAPTURE=102;
 private final ArrayList<CameraChoice> cameras=new ArrayList<>();
 private LinearLayout root, importedList;
 private TextView status, selectedCamera;
 private SharedPreferences prefs;
 private static class CameraChoice {
  final String name,pkg; final Drawable icon;
  CameraChoice(String n,String p,Drawable i){name=n;pkg=p;icon=i;}
  public String toString(){return name+"  ("+pkg+")";}
 }
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  prefs=getSharedPreferences("phantom_settings",MODE_PRIVATE);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(28,30,28,28);
  ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
  TextView title=new TextView(this);title.setText("PHANToM VPhone");title.setTextSize(27);root.addView(title);
  TextView note=new TextView(this);note.setText("Alpha: Camera Provider + APK import. Virtual runtime not yet available.");root.addView(note);
  selectedCamera=new TextView(this);root.addView(selectedCamera);
  button("Choose camera provider",()->chooseCamera());
  button("Test selected camera",()->testCamera());
  button("Test default camera",()->launchCamera(null));
  TextView sub=new TextView(this);sub.setText("Imported APK files (stored privately; not virtual-installed)");sub.setTextSize(19);root.addView(sub);
  button("Import APK file",()->pickApk());
  importedList=new LinearLayout(this);importedList.setOrientation(LinearLayout.VERTICAL);root.addView(importedList);
  status=new TextView(this);root.addView(status);
  refreshSelection();refreshImports();
 }
 private void button(String title,Runnable run){Button b=new Button(this);b.setText(title);b.setOnClickListener(v->run.run());root.addView(b);}
 private void refreshSelection(){String pkg=prefs.getString("camera_package","");selectedCamera.setText("Camera provider: "+(pkg.isEmpty()?"System default":pkg));}
 private void chooseCamera(){
  cameras.clear();PackageManager pm=getPackageManager();
  Intent capture=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
  Set<String> supported=new HashSet<>();
  for(ResolveInfo ri:pm.queryIntentActivities(capture,0)){if(ri.activityInfo!=null)supported.add(ri.activityInfo.packageName);}
  for(ApplicationInfo ai:pm.getInstalledApplications(0)){
   if(ai.packageName.equals(getPackageName()))continue;
   try{
    String name=String.valueOf(pm.getApplicationLabel(ai));
    cameras.add(new CameraChoice(name,ai.packageName,pm.getApplicationIcon(ai)));
   }catch(Exception ignored){}
  }
  Collections.sort(cameras,(a,b)->{
   boolean aa=supported.contains(a.pkg),bb=supported.contains(b.pkg);
   if(aa!=bb)return aa?-1:1;
   return a.name.compareToIgnoreCase(b.name);
  });
  String[] items=new String[cameras.size()+1];items[0]="System default";
  for(int i=0;i<cameras.size();i++)items[i+1]=(supported.contains(cameras.get(i).pkg)?"[Camera] ":"")+cameras.get(i);
  new AlertDialog.Builder(this).setTitle("Select installed camera app").setItems(items,(dialog,index)->{
   String pkg=index==0?"":cameras.get(index-1).pkg;
   prefs.edit().putString("camera_package",pkg).apply();refreshSelection();
  }).setNegativeButton("Cancel",null).show();
 }
 private void testCamera(){String pkg=prefs.getString("camera_package","");launchCamera(pkg.isEmpty()?null:pkg);}
 private void launchCamera(String pkg){
  Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
  if(pkg!=null)intent.setPackage(pkg);
  try{startActivityForResult(intent,CAPTURE);status.setText("Opening camera: "+(pkg==null?"default":pkg));}
  catch(Exception e){status.setText("Camera cannot handle IMAGE_CAPTURE: "+e.getMessage());}
 }
 private void pickApk(){
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);
  i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/vnd.android.package-archive","application/octet-stream"});
  startActivityForResult(i,PICK_APK);
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(result!=RESULT_OK){if(request==CAPTURE)status.setText("Capture canceled");return;}
  if(request==PICK_APK && data!=null && data.getData()!=null){importApk(data.getData());}
  if(request==CAPTURE)status.setText("Camera returned result; data="+(data!=null));
 }
 private void importApk(Uri uri){
  try{
   File dir=new File(getFilesDir(),"imported_apks");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create directory");
   File tmp=File.createTempFile("import_",".apk",dir);
   try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){
    if(in==null)throw new IOException("Cannot read document");
    byte[] buf=new byte[32768];int n;long size=0;
    while((n=in.read(buf))!=-1){size+=n;if(size>300L*1024*1024)throw new IOException("APK exceeds 300 MB");out.write(buf,0,n);}
   }
   android.content.pm.PackageInfo info=getPackageManager().getPackageArchiveInfo(tmp.getAbsolutePath(),0);
   if(info==null||info.packageName==null)throw new IOException("Not a valid standalone APK");
   String safe=info.packageName.replaceAll("[^a-zA-Z0-9._-]","_");
   File dest=new File(dir,safe+".apk");
   if(dest.exists()&&!dest.delete())throw new IOException("Cannot replace old import");
   if(!tmp.renameTo(dest))throw new IOException("Cannot save APK");
   status.setText("Imported "+info.packageName+" (NOT installed in virtual container)");
   refreshImports();
  }catch(Exception e){status.setText("Import failed: "+e.getMessage());}
 }
 private void refreshImports(){
  importedList.removeAllViews();
  File dir=new File(getFilesDir(),"imported_apks");File[] files=dir.listFiles((f,n)->n.endsWith(".apk"));
  if(files==null||files.length==0){TextView empty=new TextView(this);empty.setText("No APKs imported");importedList.addView(empty);return;}
  Arrays.sort(files,Comparator.comparing(File::getName));
  for(File f:files){
   LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
   TextView name=new TextView(this);name.setText(f.getName()+"\nStored only • not virtual-installed");name.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));row.addView(name);
   Button del=new Button(this);del.setText("Remove");del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Delete imported copy of "+f.getName()+"?").setPositiveButton("Delete",(a,b)->{if(f.delete())refreshImports();}).setNegativeButton("Cancel",null).show());row.addView(del);
   importedList.addView(row);
  }
 }
}
