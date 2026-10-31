"""Compile real property wrappers: exact values, callbacks, passthrough and coverage."""
from pathlib import Path
import shutil, subprocess, tempfile, unittest
ROOT=Path(__file__).resolve().parents[1]

class DeviceProperties(unittest.TestCase):
 @unittest.skipUnless(shutil.which('g++'), 'host C++ compiler required (CI)')
 def test_wrappers(self):
  with tempfile.TemporaryDirectory() as directory:
   root=Path(directory); (root/'sys').mkdir()
   (root/'sys/system_properties.h').write_text('#pragma once\nstruct prop_info {const char* name;const char* value;};\n',encoding='utf-8')
   source=root/'test.cpp'
   source.write_text('''#include <cstring>
#include <cstdint>
#include <cassert>
#include <string>
#include <sys/system_properties.h>
using Cb=void(*)(void*,const char*,const char*,uint32_t);
static int realGet(const char*,char* v){strcpy(v,"actual");return 6;}
static int realRead(const prop_info* p,char* n,char* v){if(n)strcpy(n,p->name);strcpy(v,p->value);return strlen(v);}
static void realCallback(const prop_info* p,Cb cb,void* cookie){cb(cookie,p->name,p->value,937);}
static bool partial=false;
#define RTLD_NOW 2
static void* dlopen(const char*,int){return (void*)1;}
static int dlclose(void*){return 0;}
static void hook_function(void*,const char* name,void*,void** out){
 if(!strcmp(name,"__system_property_get"))*out=(void*)realGet;
 else if(!strcmp(name,"__system_property_read"))*out=(void*)realRead;
 else if(!partial)*out=(void*)realCallback;
}
'''+ '#include "'+str(ROOT/'runtime_overlay/GuestDeviceProperties.h').replace('\\','/')+'"\n'+'''
struct Seen {void* expected;bool called;const char* value;};
static void receive(void* c,const char* name,const char* value,uint32_t serial){
 Seen* seen=(Seen*)c;assert(c==seen->expected);assert(serial==937);assert(name);seen->called=true;seen->value=value;
}
int main(int argc,char**){
 partial=argc>1;assert(GuestDeviceProperties::install()==(partial?3:7));
 assert(GuestDeviceProperties::install()==(partial?3:7));
 const char* keys[]={"ro.product.model","ro.product.brand","ro.product.name","ro.product.system.name","ro.product.odm.model","ro.product.system.model","ro.product.system.brand","ro.product.system.device","ro.product.vendor.model","ro.product.manufacturer","ro.product.system.manufacturer","ro.product.vendor.marketname","ro.product.system_ext.model","ro.build.product"};
 const char* values[]={"V2408A","iQOO","PD2408","PD2408","V2408A","V2408A","iQOO","PD2408","V2408A","iQOO","iQOO","iQOO 13","V2408A","PD2408"};
 for(int i=0;i<14;i++){
  char out[92],name[128];prop_info p{keys[i],"actual"};
  assert(GuestDeviceProperties::get(keys[i],out)==strlen(values[i]));assert(!strcmp(out,values[i]));
  assert(GuestDeviceProperties::read(&p,name,out)==strlen(values[i]));assert(!strcmp(name,keys[i]));assert(!strcmp(out,values[i]));
  if(!partial){
   GuestDeviceProperties::read(&p,nullptr,out);assert(!strcmp(out,values[i]));
   Seen seen{nullptr,false,nullptr};seen.expected=&seen;
   GuestDeviceProperties::readCallback(&p,receive,&seen);assert(seen.called);assert(!strcmp(seen.value,values[i]));
  }
 }
 char out[92];prop_info unknown{"ro.build.version.sdk","36"};
 assert(GuestDeviceProperties::get("ro.build.fingerprint",out)==6&&!strcmp(out,"actual"));
 GuestDeviceProperties::read(&unknown,nullptr,out);assert(!strcmp(out,"36"));
 if(!partial){Seen seen{nullptr,false,nullptr};seen.expected=&seen;GuestDeviceProperties::readCallback(&unknown,receive,&seen);assert(seen.called&&!strcmp(seen.value,"36"));}
}
''',encoding='utf-8')
   binary=root/'contract'
   subprocess.run(['g++','-std=c++11','-I',str(root),str(source),'-o',str(binary)],check=True)
   subprocess.run([str(binary)],check=True)
   subprocess.run([str(binary),'partial'],check=True)

if __name__=='__main__':unittest.main()
