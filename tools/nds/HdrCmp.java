import java.nio.file.*;import java.util.*;
public class HdrCmp{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get("/a/0/1/2")*8);
  int p=s+0x10;int[] st=null,en=null;int img=0;int c=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){c=u16(rom,p+8);st=new int[c];en=new int[c];
    for(int i=0;i<c;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  byte[] h=Arrays.copyOfRange(rom,img+st[0],img+en[0]);
  int rec=48;
  System.out.println("idx   b0  b1  b2-3  b4-5  b6-7  b8-9  b10-11 b27 b28 b29 b30 b31");
  for(int i=1;i<a.length;i++){
   int x=Integer.parseInt(a[i]);int o=x*rec;
   System.out.printf("%-5d %-3d %-3d %-5d %-5d %-5d %-5d %-6d %-3d %-3d %-3d %-3d %-3d%n",
     x,h[o]&0xFF,h[o+1]&0xFF,u16(h,o+2),u16(h,o+4),u16(h,o+6),u16(h,o+8),u16(h,o+10),
     h[o+27]&0xFF,h[o+28]&0xFF,h[o+29]&0xFF,h[o+30]&0xFF,h[o+31]&0xFF);
  }
 }
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p2=sub;
  while(true){int t=rom[p2]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p2+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p2+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p2+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p2+1+l)&0x0FFF;p2+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
