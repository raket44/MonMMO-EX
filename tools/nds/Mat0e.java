import java.nio.file.*;import java.util.*;
public class Mat0e{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[]b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[]b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[]a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get("/a/0/0/9")*8);
  int p=s+0x10;int[]st=null,en=null;int img=0;int c=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){c=u16(rom,p+8);st=new int[c];en=new int[c];
    for(int i=0;i<c;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  byte[] f=Arrays.copyOfRange(rom,img+st[0],img+en[0]);
  int flag=u32(f,0),w=u16(f,4),h=u16(f,6);
  int land=8, hdr=8+w*h*4;
  System.out.println("world matrix flag="+flag+" "+w+"x"+h+"  (client layout: eT0[x][y] column-major reads: land then header planes)");
  // The parser fills eT0[x][y] with y outer, x inner -> stream order is x-fastest, row-major.
  System.out.println("HEADER plane (row-major, ---=0xFFFF/empty):");
  for(int y=0;y<h;y++){StringBuilder sb=new StringBuilder();
   for(int x=0;x<w;x++){long v=u32(f,hdr+(y*w+x)*4)&0xFFFFFFFFL;
    sb.append(v==0xFFFFFFFFL||v==0xFFFFL?" ---":String.format("%4d",v));}
   System.out.println(String.format("%2d|",y)+sb);}
 }
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p2=sub;
  while(true){int t=rom[p2]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p2+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p2+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p2+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p2+1+l)&0x0FFF;p2+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
