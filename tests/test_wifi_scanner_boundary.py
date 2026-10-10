"""Compile the production scanner and verify real-service and guest-policy boundaries."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from test_location_boundary import ROOT, JAVAC, RUNJAVA


class WifiScannerBoundary(unittest.TestCase):
    @unittest.skipUnless(JAVAC, 'JDK required')
    def test_platform_failure_callbacks_and_guest_denial(self):
        sources = {
            'android/os/Build.java': 'package android.os; public class Build { public static class VERSION { public static int SDK_INT=36; } }',
            'android/net/wifi/IWifiScanner.java': 'package android.net.wifi; public interface IWifiScanner { public class Stub {} }',
            'mirror/android/os/ServiceManager.java': 'package mirror.android.os; public class ServiceManager { public static Call getService=new Call(); public static class Call { public Object call(String name){return "platform";} } }',
            'com/lody/virtual/client/core/VirtualCore.java': 'package com.lody.virtual.client.core; public class VirtualCore { public static VirtualCore get(){return new VirtualCore();} public String getHostPkg(){return "test.host";} }',
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
import java.util.*;public class VPackageManager {
 public static Map<String,Integer> grants=new HashMap<>();public static VPackageManager get(){return new VPackageManager();}
 public int checkPermission(String p,String g,int u){return grants.getOrDefault(g+":"+u+":"+p,-1);}
}''',
            'com/lody/virtual/client/hook/base/BinderInvocationStub.java': '''package com.lody.virtual.client.hook.base;
public class BinderInvocationStub { public Object base; public BinderInvocationStub(Object o){base=o;} public BinderInvocationStub(Class c,Object binder){base=binder;} }''',
            'com/lody/virtual/client/hook/base/BinderInvocationProxy.java': '''package com.lody.virtual.client.hook.base;
import java.util.*;public class BinderInvocationProxy {
 public BinderInvocationStub service; public final Map<String,MethodProxy> proxies=new HashMap<>();
 public BinderInvocationProxy(BinderInvocationStub stub,String name){service=stub;onBindMethods();}
 protected void onBindMethods(){}protected void addMethodProxy(MethodProxy p){proxies.put(p.getMethodName(),p);}
}''',
            'com/lody/virtual/client/hook/proxies/wifi_scanner/GhostWifiScannerImpl.java': 'package com.lody.virtual.client.hook.proxies.wifi_scanner; public class GhostWifiScannerImpl {}',
            'Harness.java': '''import java.lang.reflect.*;
import com.lody.virtual.client.hook.proxies.wifi_scanner.WifiScannerStub;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.ipc.VPackageManager;
public class Harness {
 public static class Listener {int failure,success;}
 public static class Service {
  int calls;Object listener;String pkg,tag;Object result=new Object();
  public Object register(Listener l,String p,String t){calls++;listener=l;pkg=p;tag=t;l.failure++;return result;}
  public Object denied(Listener l,String p,String t){throw new SecurityException("Android denied");}
 }
 public static void main(String[] ignored)throws Throwable{
  WifiScannerStub stub=new WifiScannerStub();if(!"platform".equals(stub.service.base))throw new AssertionError("Ghost scanner used");
  Service service=new Service();Listener listener=new Listener();Method method=Service.class.getMethod("register",Listener.class,String.class,String.class);
  MethodProxy hook=stub.proxies.get("registerScanListener");
  for(String pkg:new String[]{"arbitrary.renamed.provider","another.guest.app"})for(int user:new int[]{0,7}){
   MethodProxy.guest=pkg;MethodProxy.user=user;String prefix=pkg+":"+user+":";
   VPackageManager.grants.put(prefix+"android.permission.ACCESS_WIFI_STATE",0);
   VPackageManager.grants.put(prefix+"android.permission.ACCESS_FINE_LOCATION",0);
   int failures=listener.failure;
   Object result=hook.call(service,method,listener,pkg,"guest.tag");
   if(result!=service.result||service.listener!=listener||!"test.host".equals(service.pkg)||service.tag!=null||listener.failure!=failures+1||listener.success!=0)throw new AssertionError();
   VPackageManager.grants.clear();int calls=service.calls;
   try{hook.call(service,method,listener,pkg,null);throw new AssertionError();}catch(SecurityException expected){}
   if(service.calls!=calls)throw new AssertionError("Guest DENY bypassed");
   VPackageManager.grants.put(prefix+"android.permission.ACCESS_WIFI_STATE",0);
   try{hook.call(service,method,listener,pkg,null);throw new AssertionError();}catch(SecurityException expected){}
   VPackageManager.grants.put(prefix+"android.permission.ACCESS_FINE_LOCATION",0);
   try{hook.call(service,Service.class.getMethod("denied",Listener.class,String.class,String.class),listener,pkg,null);throw new AssertionError();}
   catch(InvocationTargetException e){if(!(e.getCause() instanceof SecurityException))throw e;}
   VPackageManager.grants.clear();
  }
 }
}''',
        }
        sources['com/lody/virtual/client/hook/proxies/wifi_scanner/WifiScannerStub.java'] = (ROOT / 'runtime_overlay/WifiScannerStub.java').read_text()
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
