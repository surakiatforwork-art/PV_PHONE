package com.phantom.releaseprobe;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
public class TransitionActivity extends Activity {
 protected void onCreate(Bundle saved) {
  super.onCreate(saved);
  int expected=getIntent().getIntExtra("expectedUid",-1);
  int actual=android.os.Process.myUid();
  TextView text=new TextView(this);text.setTextSize(20);
  text.setText("ACTIVITY "+(expected==actual?"PASS":"FAIL")+" expectedUID="+expected+" UID="+actual);
  setContentView(text);
 }
}
