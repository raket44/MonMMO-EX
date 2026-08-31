import java.nio.file.*;import java.util.*;
public class Mat0c{
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
  System.out.println("world matrix "+w+"x"+h+" (interleaved 8-byte cells: a,b,c,d)");
  int[][] cells={{12,14,406,469,-1},{5,12,187,414,-1},{5,14,164,472,-1},{18,18,592,599,-1},{3,9,103,310,-1},{3,10,107,341,-1},{4,11,135,369,-1},{18,17,584,589,-1}};
  for(int[] cl:cells){
   int idx=cl[1]*w+cl[0];int o=8+idx*8;
   System.out.printf("cell(%d,%d) world~(%d,%d): a=%d b=%d c=%d d=%d%n",cl[0],cl[1],cl[2],cl[3],
     u16(f,o),u16(f,o+2),u16(f,o+4),u16(f,o+6));
  }
  // Where do 191 / 296 / 107 / 291 / 154 appear, and in which field?
  int[] wanted={191,296,107,291,154,297,195,192,155};
  for(int t:wanted){
   StringBuilder sb=new StringBuilder("header "+t+": ");
   for(int i=0;i<w*h;i++){int o=8+i*8;
    for(int k=0;k<4;k++) if(u16(f,o+k*2)==t) sb.append("cell(").append(i%w).append(',').append(i/w).append(")f").append(k).append(' ');
   }
   System.out.println(sb);
  }
 }
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p2=sub;
  while(true){int t=rom[p2]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p2+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p2+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p2+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p2+1+l)&0x0FFF;p2+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
