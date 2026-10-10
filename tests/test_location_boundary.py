"""Exercise production shared location hooks with package/user policy isolation."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = Path(os.environ.get('JAVA_HOME', '')) / 'bin'
JAVAC = shutil.which('javac') or (str(JAVA / 'javac.exe') if (JAVA / 'javac.exe').exists() else None)
RUNJAVA = shutil.which('java') or str(JAVA / 'java.exe')


class LocationBoundary(unittest.TestCase):
    @unittest.skipUnless(JAVAC, 'JDK required')
    def test_packages_users_arguments_and_denial(self):
        sources = {
            'android/content/pm/PackageManager.java': 'package android.content.pm; public class PackageManager { public static final int PERMISSION_GRANTED=0; public static int hostFine=0; public int checkPermission(String permission,String pkg){return hostFine;} }',
            'com/lody/virtual/client/core/VirtualCore.java': 'package com.lody.virtual.client.core; public class VirtualCore { public static VirtualCore get(){return new VirtualCore();} public String getHostPkg(){return "test.host";} public android.content.pm.PackageManager getPackageManager(){return new android.content.pm.PackageManager();} }',
            'com/lody/virtual/client/hook/base/MethodProxy.java': '''package com.lody.virtual.client.hook.base;
import java.lang.reflect.Method;
public abstract class MethodProxy {
 public static String guest; public static int user;
 protected static String getAppPkg(){return guest;} protected static int getAppUserId(){return user;}
 protected static boolean isAppProcess(){return true;}
 public abstract String getMethodName(); public boolean isEnable(){return true;}
 public abstract Object call(Object who,Method method,Object... args) throws Throwable;
}''',
            'com/lody/virtual/client/ipc/VPackageManager.java': '''package com.lody.virtual.client.ipc;
import java.util.*;
public class VPackageManager {
 public static final Map<String,Integer> grants=new HashMap<>();
 public static VPackageManager get(){return new VPackageManager();}
 public int checkPermission(String p,String g,int u){return grants.getOrDefault(g+":"+u+":"+p,-1);}
}''',
            'com/lody/virtual/client/hook/proxies/location/LocationManagerStub.java': '''package com.lody.virtual.client.hook.proxies.location;
import java.util.*;import com.lody.virtual.client.hook.base.MethodProxy;
public class LocationManagerStub {
 public final Map<String,MethodProxy> proxies=new HashMap<>();
 public LocationManagerStub getInvocationStub(){return this;}
 public void removeMethodProxy(String name){proxies.remove(name);}
 public void addMethodProxy(MethodProxy p){proxies.put(p.getMethodName(),p);}
}''',
            'Harness.java': '''import java.lang.reflect.*;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.hook.proxies.location.*;
public class Harness {
 public static class Service {
  public Object result=new Object(); public Object[] seen; public int calls;
  public Object register(String provider,Object request,Object callback,String pkg,String tag,String listener){calls++;seen=new Object[]{provider,request,callback,pkg,tag,listener};return result;}
  public Object denied(String p,Object r,Object c,String pkg,String tag,String id){throw new SecurityException("Android denied");}
 }
 public static void main(String[] ignored) throws Throwable {
  LocationManagerStub stub=new LocationManagerStub();GuestLocationHooks.bind(stub);
  Service service=new Service();Object request=new Object(),callback=new Object();
  Method method=Service.class.getMethod("register",String.class,Object.class,Object.class,String.class,String.class,String.class);
  MethodProxy hook=stub.proxies.get("registerLocationListener");
  for(String guest:new String[]{"arbitrary.camera.mod.v123","other.navigation.app"})for(int user:new int[]{0,7}){
   MethodProxy.guest=guest;MethodProxy.user=user;
   String key=guest+":"+user+":android.permission.ACCESS_FINE_LOCATION";
   VPackageManager.grants.put(key,0);
   Object result=hook.call(service,method,"gps",request,callback,guest,"guest.tag","listener-id");
   if(result!=service.result||service.seen[1]!=request||service.seen[2]!=callback||!"gps".equals(service.seen[0])||!"test.host".equals(service.seen[3])||service.seen[4]!=null||!"listener-id".equals(service.seen[5]))throw new AssertionError();
   VPackageManager.grants.remove(key);int calls=service.calls;
   try{hook.call(service,method,"gps",request,callback,guest,null,"id");throw new AssertionError();}catch(SecurityException expected){}
   if(service.calls!=calls)throw new AssertionError("Denied guest reached Android");
   VPackageManager.grants.put(guest+":"+user+":android.permission.ACCESS_COARSE_LOCATION",0);
   try{hook.call(service,method,"gps",request,callback,guest,null,"id");throw new AssertionError("Precise host fix exposed to coarse-only guest");}catch(SecurityException expected){}
   if(service.calls!=calls)throw new AssertionError("Fine denial bypassed by shared UID");
   VPackageManager.grants.clear();
   VPackageManager.grants.put(key,0);
   try{hook.call(service,Service.class.getMethod("denied",String.class,Object.class,Object.class,String.class,String.class,String.class),"gps",request,callback,guest,null,"id");throw new AssertionError();}
   catch(InvocationTargetException expected){if(!(expected.getCause() instanceof SecurityException))throw expected;}
   VPackageManager.grants.clear();
  }
 }
}''',
        }
        sources['com/lody/virtual/client/hook/proxies/location/GuestLocationHooks.java'] = (ROOT / 'runtime_overlay/GuestLocationHooks.java').read_text()
        with tempfile.TemporaryDirectory() as directory:
            paths = []
            for name, source in sources.items():
                path = Path(directory) / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(source)
                paths.append(str(path))
            subprocess.run([JAVAC, '-d', directory, *paths], check=True, capture_output=True)
            subprocess.run([RUNJAVA, '-cp', directory, 'Harness'], check=True, capture_output=True)


if __name__ == '__main__':
    unittest.main()
