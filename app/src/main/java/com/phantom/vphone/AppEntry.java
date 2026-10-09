package com.phantom.vphone;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
final class AppEntry {
 final String label;
 final String packageName;
 final Drawable icon;
 final ApplicationInfo info;
 final boolean cameraHandler;
 AppEntry(String label,String packageName,Drawable icon,ApplicationInfo info,boolean cameraHandler){
  this.label=label;this.packageName=packageName;this.icon=icon;this.info=info;this.cameraHandler=cameraHandler;
 }
}