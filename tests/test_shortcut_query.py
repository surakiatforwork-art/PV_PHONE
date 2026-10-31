from pathlib import Path
import os,shutil,subprocess,tempfile,unittest
ROOT=Path(__file__).resolve().parents[1]
JDK=Path(os.environ.get('JAVA_HOME',''))/'bin'
JAVAC=shutil.which('javac') or str(JDK/'javac.exe')
JAVA=shutil.which('java') or str(JDK/'java.exe')

class ShortcutQuery(unittest.TestCase):
 def test_identity_filtering_and_platform_error(self):
  sources={
   'android/content/Intent.java':'''package android.content;
public class Intent {String pkg; public static Intent parseUri(String s,int flags)throws Exception {if(s.equals("bad"))throw new Exception();Intent i=new Intent();i.pkg=s;return i;} public Component getComponent(){return null;} public String getPackage(){return pkg;}public static class Component{public String getPackageName(){return "unused";}}}''',
   'android/os/PersistableBundle.java':'''package android.os;import java.util.*;public class PersistableBundle {Map<String,Object> data=new HashMap<>();public boolean containsKey(String k){return data.containsKey(k);}public int getInt(String k){return (Integer)data.get(k);}public String getString(String k){return (String)data.get(k);}public void putInt(String k,int v){data.put(k,v);}public void putString(String k,String v){data.put(k,v);}}''',
   'android/content/pm/ShortcutInfo.java':'''package android.content.pm;public class ShortcutInfo {public android.os.PersistableBundle[] routes;}''',
   'mirror/android/content/pm/ShortcutInfo.java':'''package mirror.android.content.pm;public class ShortcutInfo {public static Reader mIntentPersistableExtrases=new Reader();public static class Reader{public android.os.PersistableBundle[] get(android.content.pm.ShortcutInfo s){return s.routes;}}}''',
   'com/lody/virtual/client/core/VirtualCore.java':'''package com.lody.virtual.client.core;public class VirtualCore {public static VirtualCore get(){return new VirtualCore();}public int myUid(){return 210585;}}''',
   'com/lody/virtual/client/hook/base/MethodProxy.java':'''package com.lody.virtual.client.hook.base;public class MethodProxy {public static String guest;public static int user;public String getMethodName(){return "";}public boolean isEnable(){return true;}public static boolean isAppProcess(){return true;}public static String getAppPkg(){return guest;}public static int getAppUserId(){return user;}public static String getHostPkg(){return "test.host";}public Object call(Object o,java.lang.reflect.Method m,Object... a)throws Throwable{return m.invoke(o,a);}}''',
   'com/lody/virtual/helper/compat/ParceledListSliceCompat.java':'''package com.lody.virtual.helper.compat;import java.util.*;public class ParceledListSliceCompat {public static List getList(Object o){return (List)o;}public static Object create(List o){return o;}}''',
   'Harness.java':'''import java.util.*;import java.lang.reflect.*;import android.content.pm.ShortcutInfo;import android.os.PersistableBundle;import com.lody.virtual.client.hook.base.MethodProxy;import com.lody.virtual.client.hook.proxies.shortcut.GuestShortcutQuery;
public class Harness {
 public static class Service{Object result;int calls;Object[] args;boolean deny;public Object query(String pkg,int flags,int user){calls++;args=new Object[]{pkg,flags,user};if(deny)throw new SecurityException("Android denied");return result;}}
 static void check(boolean b){if(!b)throw new AssertionError();}
 static ShortcutInfo route(String pkg,int user){ShortcutInfo s=new ShortcutInfo();PersistableBundle b=new PersistableBundle();b.putString("_VA_|_uri_",pkg);b.putInt("_VA_|_user_id_",user);s.routes=new PersistableBundle[]{b};return s;}
 public static void main(String[] ignored)throws Throwable{
  GuestShortcutQuery hook=new GuestShortcutQuery();Service service=new Service();Method method=Service.class.getMethod("query",String.class,int.class,int.class);
  for(String pkg:new String[]{"arbitrary.game.package","renamed.app.client"})for(int user:new int[]{0,7}){
   MethodProxy.guest=pkg;MethodProxy.user=user;
   ShortcutInfo own=route(pkg,user),other=route("other.guest",user),slot=route(pkg,user+1),host=new ShortcutInfo(),bad=route("bad",user),mixed=route(pkg,user);mixed.routes=new PersistableBundle[]{mixed.routes[0],other.routes[0]};
   service.result=Arrays.asList(own,other,slot,host,bad,mixed);
   List result=(List)hook.call(service,method,pkg,133,user);
   check(result.size()==1&&result.get(0)==own);check(service.args[0].equals("test.host")&&service.args[1].equals(133)&&service.args[2].equals(2));
   int calls=service.calls;try{hook.call(service,method,"wrong.caller",133,user);throw new AssertionError();}catch(SecurityException expected){}check(calls==service.calls);
   service.deny=true;try{hook.call(service,method,pkg,133,user);throw new AssertionError();}catch(InvocationTargetException expected){check(expected.getCause() instanceof SecurityException);}service.deny=false;
   service.result=null;check(hook.call(service,method,pkg,133,user)==null);
  }
 }
}'''}
  with tempfile.TemporaryDirectory() as directory:
   root=Path(directory)
   files=[]
   for name,text in sources.items():
    p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8');files.append(str(p))
   subprocess.run([JAVAC,'-d',str(root),*files,str(ROOT/'runtime_overlay/GuestShortcutQuery.java')],check=True)
   subprocess.run([JAVA,'-cp',str(root),'Harness'],check=True)

if __name__=='__main__':unittest.main()
