import java.nio.file.*;import java.util.*;
public class Mat0b{
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
  int w=u16(f,4),h=u16(f,6);
  int n=w*h;
  // planar hypothesis: plane1 headers at 8, plane2 at 8+2n, plane3 at 8+4n, plane4 at 8+6n
  int[][] cells={{12,14,406,469},{5,12,187,414},{5,14,164,472},{18,18,592,599},{3,9,103,310},{3,10,107,341},{4,11,135,369},{13,14,436,462}};
  for(int[] cl:cells){
   int idx=cl[1]*w+cl[0];
   System.out.printf("world(%d,%d) cell(%d,%d): p1=%d p2=%d p3=%d p4=%d%n",cl[2],cl[3],cl[0],cl[1],
     u16(f,8+idx*2),u16(f,8+2*n+idx*2),u16(f,8+4*n+idx*2),u16(f,8+6*n+idx*2));
  }
 }
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p2=sub;
  while(true){int t=rom[p2]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p2+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p2+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p2+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p2+1+l)&0x0FFF;p2+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
