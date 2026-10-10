package com.phantom.releaseprobe;
import android.app.Activity;
import android.content.*;
import android.os.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.*;
import java.io.File;
public class MainActivity extends Activity {
 TextView status;
 protected void onCreate(Bundle saved) {
  super.onCreate(saved);
  LinearLayout root=new LinearLayout(this); root.setOrientation(1);
  status=new TextView(this); status.setTextSize(18); root.addView(status);
  Button thumb=new Button(this);thumb.setText("Capture thumbnail");root.addView(thumb);
  Button output=new Button(this);output.setText("Capture output URI");root.addView(output);
  thumb.setOnClickListener(v->capture(false));output.setOnClickListener(v->capture(true));
  status.setText("CAMERA="+getPackageManager().checkPermission("android.permission.CAMERA",getPackageName())+" SELF="+checkSelfPermission("android.permission.CAMERA")+" UID="+android.os.Process.myUid());
  setContentView(root);
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
