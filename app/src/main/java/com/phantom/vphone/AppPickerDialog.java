package com.phantom.vphone;
import android.app.*;
import android.content.*;
import android.graphics.drawable.Drawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
final class AppPickerDialog {
 interface Pick { void onPick(AppEntry app); }
 static void show(Activity a,String title,List<AppEntry> source,boolean allowDefault,Pick pick){
  LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);int pad=dp(a,12);box.setPadding(pad,pad,pad,pad);
  EditText search=new EditText(a);search.setHint("Search app or package");search.setSingleLine(true);box.addView(search);
  ScrollView scroll=new ScrollView(a);LinearLayout rows=new LinearLayout(a);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);
  box.addView(scroll,new LinearLayout.LayoutParams(-1,dp(a,520)));
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle(title).setView(box).setNegativeButton("Cancel",null).create();
  Runnable render=()->{
   rows.removeAllViews();
   if(allowDefault)row(a,rows,a.getDrawable(android.R.drawable.ic_menu_camera),"System default","Android camera chooser",()->{pick.onPick(null);dialog.dismiss();});
   String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);int shown=0;
   for(AppEntry e:source){
    if(!e.label.toLowerCase(Locale.ROOT).contains(q)&&!e.packageName.toLowerCase(Locale.ROOT).contains(q))continue;
    String sub=e.packageName+(e.cameraHandler?" โ€ข Camera capture":"");
    row(a,rows,e.icon,e.label,sub,()->{pick.onPick(e);dialog.dismiss();});
    if(++shown>=300)break;
   }
   if(shown==0&&!allowDefault){TextView t=new TextView(a);t.setText("No matching apps");t.setPadding(pad,pad,pad,pad);rows.addView(t);}
  };
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int af){} public void onTextChanged(CharSequence s,int st,int b,int c){render.run();} public void afterTextChanged(Editable e){}});
  render.run();dialog.show();
 }
 private static void row(Activity a,LinearLayout parent,Drawable icon,String title,String sub,Runnable click){
  LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(a,8),dp(a,8),dp(a,8),dp(a,8));row.setMinimumHeight(dp(a,64));
  ImageView iv=new ImageView(a);if(icon!=null)iv.setImageDrawable(icon);else iv.setImageResource(android.R.drawable.sym_def_app_icon);
  LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(a,48),dp(a,48));ip.setMargins(0,0,dp(a,12),0);row.addView(iv,ip);
  LinearLayout text=new LinearLayout(a);text.setOrientation(LinearLayout.VERTICAL);
  TextView n=new TextView(a);n.setText(title);n.setTextSize(16);text.addView(n);
  TextView s=new TextView(a);s.setText(sub);s.setTextSize(11);s.setMaxLines(2);text.addView(s);
  row.addView(text,new LinearLayout.LayoutParams(0,-2,1));row.setOnClickListener(v->click.run());parent.addView(row);
  View line=new View(a);line.setBackgroundColor(0x22000000);parent.addView(line,new LinearLayout.LayoutParams(-1,dp(a,1)));
 }
 private static int dp(Context c,int v){return (int)(v*c.getResources().getDisplayMetrics().density+.5f);}
}
