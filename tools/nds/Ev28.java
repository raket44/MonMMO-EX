import java.nio.file.*;import java.util.*;
public class Ev28{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[]b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int s16(byte[]b,int o){return (short)u16(b,o);}
 static int u32(byte[]b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[]a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  int fat=u32(rom,0x48);
  // header -> event file id at bytes 6-7
  byte[] hdr=narc("/a/0/1/2",0);
  int mapIdx=Integer.parseInt(a[1]);
  int evId=u16(hdr,mapIdx*48+6);
  System.out.println("map "+mapIdx+" -> event file "+evId);
  byte[] ev=narc("/a/1/2/5",evId);
  int p=4;
  int nNpc=ev[p]&0xFF,nSec=ev[p+1]&0xFF,nWarp=ev[p+2]&0xFF,nTrig=ev[p+3]&0xFF;p+=4;
  System.out.println("npc="+nNpc+" sec="+nSec+" warp="+nWarp+" trig="+nTrig+" size="+ev.length);
  p+=nNpc*20;
  System.out.println("-- second (36B) --");
  for(int i=0;i<nSec;i++){int o=p+i*36;StringBuilder sb=new StringBuilder();
   for(int k=0;k<18;k++)sb.append(s16(ev,o+k*2)).append(k==17?"":",");
   System.out.println(" #"+i+": "+sb);}
  p+=nSec*36;
  p+=nWarp*20;
  System.out.println("-- triggers (raw after warps) --");
  for(int i=0;i<nTrig;i++){int o=p+i*22;if(o+22>ev.length){System.out.println(" trig rec size guess wrong");break;}
   StringBuilder sb=new StringBuilder();
   for(int k=0;k<11;k++)sb.append(s16(ev,o+k*2)).append(k==10?"":",");
   System.out.println(" #"+i+": "+sb);}
  System.out.println("bytes after warps: "+(ev.length-p));
 }
 static byte[] narc(String path,int index)throws Exception{
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get(path)*8);
  int p=s+0x10;int[]st=null,en=null;int img=0;int c=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){c=u16(rom,p+8);st=new int[c];en=new int[c];
    for(int i=0;i<c;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  return Arrays.copyOfRange(rom,img+st[index],img+en[index]);
 }
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p2=sub;
  while(true){int t=rom[p2]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p2+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p2+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p2+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p2+1+l)&0x0FFF;p2+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
