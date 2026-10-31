"""Compile production opt-in profile and verify isolation, rollback and real fields."""
from pathlib import Path
import os, shutil, subprocess, tempfile, unittest
ROOT = Path(__file__).resolve().parents[1]
JDK = Path(os.environ.get('JAVA_HOME', '')) / 'bin'
JAVAC = shutil.which('javac') or str(JDK / 'javac.exe')
JAVA = shutil.which('java') or str(JDK / 'java.exe')

class DeviceProfile(unittest.TestCase):
 def test_production_profile(self):
  with tempfile.TemporaryDirectory() as directory:
   root=Path(directory)
   build=root/'android/os/Build.java'; build.parent.mkdir(parents=True)
   build.write_text('package android.os; public class Build {public static String MODEL="real",BRAND="real",MANUFACTURER="real",PRODUCT="real",DEVICE="real",FINGERPRINT="untouched"; public static int SDK_INT=36;}',encoding='utf-8')
   harness=root/'Harness.java'
   harness.write_text('''import java.io.*;import com.lody.virtual.helper.utils.GuestDeviceProfile;
public class Harness {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args)throws Exception {
  File root=new File(args[0]);
  for(String pkg:new String[]{"arbitrary.game.renamed","different.app.client"}) {
   for(int user:new int[]{0,7}) {
    check(GuestDeviceProfile.get(root,pkg,user).equals("default"));
    GuestDeviceProfile.apply(root,pkg,user);check(android.os.Build.MODEL.equals("real"));
    check(!GuestDeviceProfile.set(root,pkg,user,"unknown"));
    check(GuestDeviceProfile.set(root,pkg,user,"iqoo13"));
    check(GuestDeviceProfile.get(root,pkg,user).equals("iqoo13"));
    check(GuestDeviceProfile.get(root,pkg,user+1).equals("default"));
    check(GuestDeviceProfile.get(root,"untouched.app",user).equals("default"));
    GuestDeviceProfile.apply(root,pkg,user);
    check(android.os.Build.MODEL.equals("V2408A")&&android.os.Build.BRAND.equals("iQOO")&&android.os.Build.DEVICE.equals("PD2408"));
    check(android.os.Build.FINGERPRINT.equals("untouched")&&android.os.Build.SDK_INT==36);
    check(GuestDeviceProfile.set(root,pkg,user,"default"));
    check(GuestDeviceProfile.get(root,pkg,user).equals("default"));
    android.os.Build.MODEL="real";
   }
  }
  check(!GuestDeviceProfile.set(root,"../escape",0,"iqoo13"));
  check(!GuestDeviceProfile.set(root,"valid.pkg",-1,"iqoo13"));
  File corrupt=new File(root,"phantom_device_profiles/corrupt.app_u0.properties");
  try(FileWriter out=new FileWriter(corrupt)){out.write("profile=unknown\\n");}
  check(GuestDeviceProfile.get(root,"corrupt.app",0).equals("default"));
 }
}''',encoding='utf-8')
   subprocess.run([JAVAC,'-d',str(root),str(build),str(harness),str(ROOT/'runtime_overlay/GuestDeviceProfile.java')],check=True)
   subprocess.run([JAVA,'-cp',str(root),'Harness',str(root/'policies')],check=True)

if __name__=='__main__':unittest.main()
