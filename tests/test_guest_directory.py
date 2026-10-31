from pathlib import Path
import shutil,subprocess,tempfile,unittest
ROOT=Path(__file__).resolve().parents[1]

class GuestDirectory(unittest.TestCase):
 @unittest.skipUnless(shutil.which('g++'),'host C++ compiler required (CI)')
 def test_real_listing_and_policy_failure(self):
  with tempfile.TemporaryDirectory() as directory:
   root=Path(directory);(root/'one').mkdir();(root/'two').mkdir()
   (root/'one/owned-a').write_text('a');(root/'two/owned-b').write_text('b')
   source=root/'contract.cpp'
   source.write_text(r'''#include <cstdlib>
#include <cstring>
#include <string>
#include <cassert>
#include <dirent.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#define FORBID 1
#define FREE(p,o) {if(p&&p!=o){free((void*)p);errno=EIO;}}
#define ALOGI(...) ((void)0)
static std::string root;static int calls;
static const char* relocate_path(const char* p,int* policy){
 *policy=0;if(!strncmp(p,"/deny/",6)){*policy=FORBID;return nullptr;}
 if(!strncmp(p,"/guest.alpha/",13))return strdup((root+"/one/"+(p+13)).c_str());
 if(!strncmp(p,"/renamed.beta/",14))return strdup((root+"/two/"+(p+14)).c_str());
 return p;
}
static DIR* original(const char* p){calls++;return ::opendir(p);}
static void hook_function(void*,const char* name,void*,void** out){assert(!strcmp(name,"opendir"));*out=(void*)original;}
'''.replace('\\"','"')+'#include "'+str(ROOT/'runtime_overlay/GuestDirectoryHooks.h').replace('\\','/')+'"\n'+r'''
static void listing(const char* path,const char* own,const char* other){
 DIR* d=phantom_opendir(path);assert(d);assert(fcntl(dirfd(d),F_GETFD)&FD_CLOEXEC);bool found=false;dirent* e;
 while((e=readdir(d))){if(!strcmp(e->d_name,own))found=true;assert(strcmp(e->d_name,other));}assert(found);assert(closedir(d)==0);
}
int main(int argc,char** argv){root=argv[1];hook_guest_directory_symbols((void*)1);
 listing("/guest.alpha/","owned-a","owned-b");listing("/renamed.beta/","owned-b","owned-a");
 int before=calls;errno=0;assert(phantom_opendir("/deny/private")==nullptr&&errno==EACCES&&calls==before);
 errno=0;assert(phantom_opendir("/guest.alpha/missing")==nullptr&&errno==ENOENT);
 assert(chdir((root+"/one").c_str())==0);listing(".","owned-a","owned-b");
}
'''.replace('\\"','"'),encoding='utf-8')
   binary=root/'contract';subprocess.run(['g++','-std=c++11',str(source),'-o',str(binary)],check=True)
   subprocess.run([str(binary),str(root)],check=True)

if __name__=='__main__':unittest.main()
