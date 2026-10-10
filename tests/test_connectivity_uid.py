"""Compile the production proxy and exercise its Binder argument boundary."""
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


class ConnectivityUidContract(unittest.TestCase):
    @unittest.skipUnless(JAVAC, 'JDK required')
    def test_self_uid_only_and_real_result(self):
        sources = {
            'android/content/Context.java': 'package android.content; public class Context { public static final String CONNECTIVITY_SERVICE="connectivity"; }',
            'mirror/android/net/IConnectivityManager.java': 'package mirror.android.net; public class IConnectivityManager { public static class Stub { public static Object asInterface; } }',
            'com/lody/virtual/client/hook/base/MethodProxy.java': '''package com.lody.virtual.client.hook.base;
import java.lang.reflect.Method;
public abstract class MethodProxy {
 public abstract String getMethodName(); public boolean isEnable(){return true;}
 public abstract Object call(Object who, Method method, Object... args) throws Throwable;
 protected static boolean isAppProcess(){return true;}
 protected static int getVUid(){return 110015;} protected static int getBaseVUid(){return 10015;}
 protected static int getRealUid(){return 10196;}
}''',
            'com/lody/virtual/client/hook/base/BinderInvocationProxy.java': '''package com.lody.virtual.client.hook.base;
import java.util.*;
public class BinderInvocationProxy {
 public final Map<String,MethodProxy> proxies=new HashMap<>();
 public BinderInvocationProxy(Object o,String s){onBindMethods();}
 protected void onBindMethods(){} protected void addMethodProxy(MethodProxy p){proxies.put(p.getMethodName(),p);}
}''',
            'Harness.java': '''import com.lody.virtual.client.hook.proxies.connectivity.ConnectivityStub;
import java.lang.reflect.*;
public class Harness {
 public static class Service {
  public Object result=new Object(); public int uid; public boolean blocked;
  public Object network(Object network,int u,boolean b){uid=u;blocked=b;return result;}
  public Object active(int u,boolean b){uid=u;blocked=b;return result;}
  public Object denied(Object n,int u,boolean b){throw new SecurityException("denied");}
 }
 public static void main(String[] args) throws Throwable {
  ConnectivityStub stub=new ConnectivityStub(); Service service=new Service(); Object network=new Object();
  Method method=Service.class.getMethod("network",Object.class,int.class,boolean.class);
  for(int uid:new int[]{110015,10015,10196,99999,0,-1}) {
   Object[] call={network,uid,false}; Object result=stub.proxies.get("getNetworkInfoForUid").call(service,method,call);
   if(result!=service.result || call[0]!=network || service.blocked || service.uid!=((uid==110015||uid==10015)?10196:uid)) throw new AssertionError();
  }
  method=Service.class.getMethod("active",int.class,boolean.class);
  for(String name:new String[]{"getActiveNetworkInfoForUid","getActiveNetworkForUid"}) {
   stub.proxies.get(name).call(service,method,110015,true);
   if(service.uid!=10196||!service.blocked) throw new AssertionError();
  }
  try {stub.proxies.get("getNetworkInfoForUid").call(service,Service.class.getMethod("denied",Object.class,int.class,boolean.class),network,10015,false); throw new AssertionError();}
  catch(InvocationTargetException e){if(!(e.getCause() instanceof SecurityException)) throw e;}
 }
}''',
        }
        sources['com/lody/virtual/client/hook/proxies/connectivity/ConnectivityStub.java'] = (ROOT / 'runtime_overlay/ConnectivityStub.java').read_text()
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
