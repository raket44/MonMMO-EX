import java.nio.file.*;import java.util.*;
public class SibProbe{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[]b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[]b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[]a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get("/a/0/1/2")*8);
  int p=s+0x10;int[]st=null,en=null;int img=0;int c=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){c=u16(rom,p+8);st=new int[c];en=new int[c];
    for(int i=0;i<c;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  byte[] hdr=Arrays.copyOfRange(rom,img+st[0],img+en[0]);
  int maps=hdr.length/48;
  // world plane
  s=u32(rom,fat+paths.get("/a/0/0/9")*8);p=s+0x10;st=null;en=null;img=0;c=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){c=u16(rom,p+8);st=new int[c];en=new int[c];
    for(int i=0;i<c;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  byte[] f=Arrays.copyOfRange(rom,img+st[0],img+en[0]);
  int w=u16(f,4),h=u16(f,6);int hp=8+w*h*4;
  Set<Integer> plane=new TreeSet<>();
  for(int i2=0;i2<w*h;i2++){long v=u32(f,hp+i2*4)&0xFFFFFFFFL;if(v!=0xFFFFFFFFL)plane.add((int)v);}
  System.out.println("253 on plane: "+plane.contains(253)+" matrix="+u16(hdr,253*48+4));
  System.out.println("301 on plane: "+plane.contains(301)+" matrix="+u16(hdr,301*48+4));
  System.out.print("on-plane maps with matrix 106: ");
  for(int m2:plane)if(u16(hdr,m2*48+4)==106)System.out.print(m2+" ");
  System.out.println();
  System.out.print("all maps with matrix 106: ");
  for(int m2=0;m2<maps;m2++)if(u16(hdr,m2*48+4)==106)System.out.print(m2+" ");
  System.out.println();
 }
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p2=sub;
  while(true){int t=rom[p2]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p2+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p2+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p2+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p2+1+l)&0x0FFF;p2+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
