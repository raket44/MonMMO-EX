import java.nio.*;import java.nio.file.*;import java.util.*;

/** Reads a Gen 5 text archive (NARC sub-file) and decrypts its UTF-16 entries. */
public class Gen5Text{
 static byte[] rom;
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}

 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));
  String path=a[1];int wantFile=Integer.parseInt(a[2]);
  int fnt=u32(rom,0x40),fat=u32(rom,0x48);
  Map<String,Integer> paths=new LinkedHashMap<>();walk(fnt,0,"",paths);
  Integer id=paths.get(path.startsWith("/")?path:"/"+path);if(id==null){System.out.println("no "+path);return;}
  int s=u32(rom,fat+id*8);
  byte[] sub=narcFile(s,wantFile);
  if(sub==null){System.out.println("no subfile");return;}
  List<String> out=decode(sub);
  for(int i=0;i<out.size();i++){String t=out.get(i);if(t!=null&&!t.isBlank())System.out.println(i+"\t"+t);}
 }

 /** Pulls one file out of a NARC at romOffset. */
 static byte[] narcFile(int s,int index){
  int p=s+0x10;int count=0;int[] starts=null,ends=null;byte[] img=null;int imgOff=0;
  while(true){
   String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){count=u16(rom,p+8);starts=new int[count];ends=new int[count];
    for(int i=0;i<count;i++){starts[i]=u32(rom,p+12+i*8);ends[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){imgOff=p+8;break;}
   if(cs<=0)break;p+=cs;
  }
  if(starts==null||index>=count)return null;
  return Arrays.copyOfRange(rom,imgOff+starts[index],imgOff+ends[index]);
 }

 /** Gen 5 message archive: sections of encrypted UTF-16LE strings. */
 static List<String> decode(byte[] f){
  List<String> out=new ArrayList<>();
  int sections=u16(f,0),entries=u16(f,2);
  for(int sec=0;sec<sections;sec++){
   int secOff=u32(f,0x0C+sec*4);
   for(int e=0;e<entries;e++){
    int off=secOff+u32(f,secOff+4+e*8);int len=u16(f,secOff+4+e*8+4);
    int key=(0x7C89+e*0x2983)&0xFFFF;
    StringBuilder sb=new StringBuilder();
    for(int j=0;j<len;j++){
     int c=u16(f,off+j*2)^key;
     key=((key<<3)|(key>>>13))&0xFFFF;
     if(c==0xFFFF)break;
     if(c>=0x20&&c<0xE000)sb.append((char)c);
    }
    out.add(sb.toString());
   }
   break;
  }
  return out;
 }

 static void walk(int fnt,int dir,String pre,Map<String,Integer> out){
  int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p=sub;
  while(true){int t=rom[p]&0xFF;if(t==0)break;
   if(t<0x80){out.put(pre+"/"+new String(rom,p+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p+=1+t;}
   else{int l=t&0x7F;String n=new String(rom,p+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p+1+l)&0x0FFF;p+=1+l+2;walk(fnt,sd,pre+"/"+n,out);}}}
}
