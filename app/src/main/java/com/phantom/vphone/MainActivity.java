package com.phantom.vphone;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.provider.MediaStore;
import android.widget.*;
import android.view.*;
import java.io.File;
import java.util.*;

public class MainActivity extends Activity {
 static final int PICK=101,CAPTURE=102;
 LinearLayout root,imports;TextView status,selected;SharedPreferences prefs;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);prefs=getSharedPreferences("phantom_settings",MODE_PRIVATE);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,30,24,30);
  ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
  TextView title=new TextView(this);title.setText("PHANToM VPhone");title.setTextSize(27);root.addView(title);
  TextView note=new TextView(this);note.setText("Alpha preview: app packages are imported but not virtual-installed yet.");root.addView(note);
  selected=new TextView(this);root.addView(selected);
  button("Choose camera provider",()->chooseCamera());
  button("Test selected camera",()->launchCamera(prefs.getString("camera_package","")));
  TextView sub=new TextView(this);sub.setText("Import app package");sub.setTextSize(20);root.addView(sub);
  button("Choose from installed apps",()->chooseInstalled());
  button("Choose APK / APKS / APKM / XAPK / APK+",()->pickDocument());
  imports=new LinearLayout(this);imports.setOrientation(LinearLayout.VERTICAL);root.addView(imports);
  status=new TextView(this);root.addView(status);refresh();
 }
 void button(String text,Runnable r){Button b=new Button(this);b.setText(text);b.setOnClickListener(v->r.run());root.addView(b);}
 List<AppEntry> apps(){
  PackageManager pm=getPackageManager();Set<String> cameras=new HashSet<>();
  for(ResolveInfo r:pm.queryIntentActivities(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),0))if(r.activityInfo!=null)cameras.add(r.activityInfo.packageName);
  ArrayList<AppEntry> out=new ArrayList<>();
  for(ApplicationInfo ai:pm.getInstalledApplications(0)){if(ai.packageName.equals(getPackageName()))continue;try{out.add(new AppEntry(String.valueOf(pm.getApplicationLabel(ai)),ai.packageName,pm.getApplicationIcon(ai),ai,cameras.contains(ai.packageName)));}catch(Exception ignored){}}
  out.sort((a,b)->{if(a.cameraHandler!=b.cameraHandler)return a.cameraHandler?-1:1;return a.label.compareToIgnoreCase(b.label);});return out;
 }
 void chooseCamera(){AppPickerDialog.show(this,"Select camera app",apps(),true,e->{prefs.edit().putString("camera_package",e==null?"":e.packageName).apply();refresh();});}
 void chooseInstalled(){AppPickerDialog.show(this,"Import installed app",apps(),false,e->{if(e==null)return;status.setText("Importing "+e.label+"…");new Thread(()->{try{String result=PackageImporter.installed(this,e);runOnUiThread(()->{status.setText("Imported "+result+" — not virtual-installed");refreshImports();});}catch(Exception ex){runOnUiThread(()->status.setText("Import failed: "+ex.getMessage()));}}).start();});}
 void launchCamera(String pkg){Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);if(pkg!=null&&!pkg.isEmpty())i.setPackage(pkg);try{startActivityForResult(i,CAPTURE);status.setText("Opening "+(pkg==null||pkg.isEmpty()?"system camera":pkg));}catch(Exception e){status.setText("Camera unavailable: "+e.getMessage());}}
 void pickDocument(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK);}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK){if(req==CAPTURE)status.setText("Capture canceled");return;}if(req==PICK&&data!=null&&data.getData()!=null){final android.net.Uri uri=data.getData();status.setText("Importing package…");new Thread(()->{try{String pkg=PackageImporter.document(this,uri);runOnUiThread(()->{status.setText("Imported "+pkg+" — not virtual-installed");refreshImports();});}catch(Exception e){runOnUiThread(()->status.setText("Import failed: "+e.getMessage()));}}).start();}else if(req==CAPTURE)status.setText("Camera returned result; data="+(data!=null));}
 void refresh(){String pkg=prefs.getString("camera_package","");selected.setText("Camera provider: "+(pkg.isEmpty()?"System default":pkg));refreshImports();}
 void refreshImports(){if(imports==null)return;imports.removeAllViews();File[] dirs=PackageImporter.root(this).listFiles(File::isDirectory);if(dirs==null||dirs.length==0){TextView t=new TextView(this);t.setText("No imported packages");imports.addView(t);return;}Arrays.sort(dirs,Comparator.comparing(File::getName));for(File f:dirs){if(f.getName().startsWith(".tmp_"))continue;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);File[] apks=f.listFiles((x,n)->n.endsWith(".apk"));TextView n=new TextView(this);n.setText(f.getName()+"\n"+(apks==null?0:apks.length)+" APK part(s) • imported only");row.addView(n,new LinearLayout.LayoutParams(0,-2,1));Button del=new Button(this);del.setText("Remove");del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Remove imported copy?").setPositiveButton("Remove",(a,b)->{PackageImporter.delete(f);refreshImports();}).setNegativeButton("Cancel",null).show());row.addView(del);imports.addView(row);}}
}