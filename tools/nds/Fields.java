import java.nio.file.*;import java.util.*;
public class Fields{
 static byte[] rom;
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));
  int fnt=u32(rom,0x40),fat=u32(rom,0x48);
  Map<String,Integer> paths=new LinkedHashMap<>();walk(fnt,0,"",paths);
  int s=u32(rom,fat+paths.get("/a/0/1/2")*8);
  int p=s+0x10;int[] st=null,en=null;int img=0;int count=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){count=u16(rom,p+8);st=new int[count];en=new int[count];
    for(int i=0;i<count;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;}
   if(cs<=0)break;p+=cs;}
  byte[] f=Arrays.copyOfRange(rom,img+st[0],img+en[0]);
  int rec=48,n=f.length/rec;
  System.out.println("headers="+n);
  for(int off=0;off<rec;off++){
   int mx=-1;Set<Integer> d=new HashSet<>();
   for(int i=0;i<n;i++){int v=f[i*rec+off]&0xFF;mx=Math.max(mx,v);d.add(v);}
   if(mx<=116&&d.size()>15)System.out.println("u8@"+off+" max="+mx+" distinct="+d.size());
  }
 }
 static void walk(int fnt,int dir,String pre,Map<String,Integer> out){
  int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p=sub;
  while(true){int t=rom[p]&0xFF;if(t==0)break;
   if(t<0x80){out.put(pre+"/"+new String(rom,p+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p+1+l)&0x0FFF;p+=1+l+2;walk(fnt,sd,pre+"/"+nm,out);}}}
}
