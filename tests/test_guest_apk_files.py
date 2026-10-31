from pathlib import Path
import os,shutil,subprocess,tempfile,unittest
ROOT=Path(__file__).resolve().parents[1]
JDK=Path(os.environ.get('JAVA_HOME',''))/'bin'

class ApkFiles(unittest.TestCase):
 def test_bytes_readonly_update_and_source_protection(self):
  with tempfile.TemporaryDirectory() as directory:
   root=Path(directory); harness=root/'Harness.java'
   harness.write_text('''import java.io.*;import java.nio.file.*;import java.nio.file.attribute.*;import java.util.*;import com.lody.virtual.helper.utils.GuestApkFiles;
public class Harness {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  File dir=new File(args[0]);File source=new File(dir,"source.apk"),dest=new File(dir,"split_AssetPackConfig.apk"),data=new File(dir,"guest-data");
  boolean posix=FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  try{
   byte[] content=new byte[190003];new Random(123).nextBytes(content);Files.write(source.toPath(),content);Files.write(data.toPath(),new byte[]{42});
   GuestApkFiles.copy(source,dest);check(Arrays.equals(content,Files.readAllBytes(dest.toPath())));
   if(posix){Set<PosixFilePermission> perms=Files.getPosixFilePermissions(dest.toPath());check(!perms.contains(PosixFilePermission.OWNER_WRITE)&&!perms.contains(PosixFilePermission.GROUP_WRITE)&&!perms.contains(PosixFilePermission.OTHERS_WRITE));}
   content[0]=7;Files.write(source.toPath(),content);if(!posix)dest.setWritable(true);
   GuestApkFiles.copy(source,dest);check(Arrays.equals(content,Files.readAllBytes(dest.toPath())));check(Files.readAllBytes(data.toPath())[0]==42);
   try{GuestApkFiles.copy(source,source);throw new AssertionError();}catch(IOException expected){}check(Arrays.equals(content,Files.readAllBytes(source.toPath())));
   try{GuestApkFiles.copy(new File(dir,"missing.apk"),new File(dir,"failed.apk"));throw new AssertionError();}catch(IOException expected){}check(!new File(dir,"failed.apk").exists());
  }finally{dest.setWritable(true);}
 }
}''',encoding='utf-8')
   javac=shutil.which('javac') or str(JDK/'javac.exe');java=shutil.which('java') or str(JDK/'java.exe')
   subprocess.run([javac,'-d',str(root),str(harness),str(ROOT/'runtime_overlay/GuestApkFiles.java')],check=True)
   subprocess.run([java,'-cp',str(root),'Harness',str(root)],check=True)

if __name__=='__main__':unittest.main()
