package com.phantom.releaseprobe;
import android.app.Activity;
import android.content.*;
import android.os.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.*;
import java.io.File;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import java.util.List;
public class MainActivity extends Activity {
 TextView status;
 protected void onCreate(Bundle saved) {
  super.onCreate(saved);
  LinearLayout root=new LinearLayout(this); root.setOrientation(1);
  status=new TextView(this); status.setTextSize(18); root.addView(status);
  Button thumb=new Button(this);thumb.setText("Capture thumbnail");root.addView(thumb);
  Button output=new Button(this);output.setText("Capture output URI");root.addView(output);
  thumb.setOnClickListener(v->capture(false));output.setOnClickListener(v->capture(true));
  Button channels=new Button(this);channels.setText("Test notification channels");root.addView(channels);
  channels.setOnClickListener(v->testChannels());
  Button locales=new Button(this);locales.setText("Test guest locales");root.addView(locales);
  locales.setOnClickListener(v->testLocales());
  Button provider=new Button(this);provider.setText("Test guest provider");root.addView(provider);
  provider.setOnClickListener(v->testProvider());
  status.setText("CAMERA="+getPackageManager().checkPermission("android.permission.CAMERA",getPackageName())+" SELF="+checkSelfPermission("android.permission.CAMERA")+" UID="+android.os.Process.myUid());
  setContentView(root);
 }
 void testLocales() {
  try {
   Object manager=getSystemService("locale");
   java.lang.reflect.Method get=manager.getClass().getMethod("getApplicationLocales");
   java.lang.reflect.Method set=manager.getClass().getMethod("setApplicationLocales",LocaleList.class);
   LocaleList initial=(LocaleList)get.invoke(manager);
   try {
    set.invoke(manager,LocaleList.forLanguageTags("fr-FR"));
    if(!"fr-FR".equals(((LocaleList)get.invoke(manager)).toLanguageTags()))throw new AssertionError("Locale roundtrip");
   }finally{set.invoke(manager,initial);}
   status.setText("LOCALES PASS initial="+initial.toLanguageTags()+" UID="+android.os.Process.myUid());
  }catch(Throwable e){status.setText("LOCALES FAIL: "+e);android.util.Log.e("PHANToM.Probe","locales",e);}
 }
 void testProvider() {
  try {
   String other=getPackageName().endsWith("other")?"com.phantom.releaseprobe":"com.phantom.releaseprobeother";
   try(android.database.Cursor cursor=getContentResolver().query(Uri.parse("content://"+other+".capture/identity"),null,null,null,null)) {
    if(cursor==null||!cursor.moveToFirst())throw new AssertionError("No provider result");
    if(cursor.getInt(0)!=android.os.Process.myUid()||!getPackageName().equals(cursor.getString(1)))throw new AssertionError("Caller identity mismatch: "+cursor.getInt(0)+" / "+cursor.getString(1));
    status.setText("PROVIDER PASS UID="+cursor.getInt(0)+" package="+cursor.getString(1));
   }
  }catch(Throwable e){status.setText("PROVIDER FAIL: "+e);android.util.Log.e("PHANToM.Probe","provider",e);}
 }
 void testChannels() {
  try {
   NotificationManager manager=getSystemService(NotificationManager.class);
   NotificationChannelGroup group=new NotificationChannelGroup("shared-group","Probe group");
   manager.createNotificationChannelGroup(group);
   NotificationChannel channel=new NotificationChannel("shared-channel",getPackageName(),NotificationManager.IMPORTANCE_DEFAULT);
   channel.setGroup("shared-group");channel.setDescription("roundtrip description");
   manager.createNotificationChannel(channel);
   if(!"shared-channel".equals(channel.getId())||!"shared-group".equals(group.getId()))throw new AssertionError("Input mutated");
   NotificationChannel read=manager.getNotificationChannel("shared-channel");
   if(read==null||!getPackageName().contentEquals(read.getName())||!"shared-channel".equals(read.getId())||!"shared-group".equals(read.getGroup())||!"roundtrip description".equals(read.getDescription()))throw new AssertionError("Channel roundtrip failed");
   List<NotificationChannel> all=manager.getNotificationChannels();
   boolean found=false;
   for(NotificationChannel item:all){if(item.getId().startsWith("phantom:"))throw new AssertionError("Host ID leaked");if("shared-channel".equals(item.getId()))found=true;}
   if(!found)throw new AssertionError("Channel missing from list");
   NotificationChannelGroup readGroup=manager.getNotificationChannelGroup("shared-group");
   if(readGroup==null||!"shared-group".equals(readGroup.getId()))throw new AssertionError("Group roundtrip failed");
   if(manager.getNotificationChannelGroups().isEmpty())throw new AssertionError("Group list empty");
   manager.deleteNotificationChannel("shared-channel");
   if(manager.getNotificationChannel("shared-channel")!=null)throw new AssertionError("Channel delete failed");
   manager.createNotificationChannel(channel);
   manager.notify(9001,new Notification.Builder(this,"shared-channel").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Guest notification probe").setContentText("Notification channel roundtrip passed").build());
   status.setText("CHANNELS PASS UID="+android.os.Process.myUid()+" listed="+all.size()+" notification posted");
  }catch(Throwable e){status.setText("CHANNELS FAIL: "+e);android.util.Log.e("PHANToM.Probe","channels",e);}
 }
 void capture(boolean output) {
  try {
   Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
   if(output) {
    Uri uri=Uri.parse("content://com.phantom.releaseprobe.capture/photo");
    i.putExtra(MediaStore.EXTRA_OUTPUT,uri);
    i.setClipData(ClipData.newRawUri("capture",uri));
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
   }
   startActivityForResult(i,output?7002:7001);
  }catch(Throwable e){status.setText("FAIL: "+e);}
 }
 protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  boolean thumb=data!=null&&data.getParcelableExtra("data")!=null;
  long size=new File(getFilesDir(),"capture.jpg").length();
  status.setText("request="+request+" result="+result+" thumbnail="+thumb+" outputBytes="+size);
 }
}
