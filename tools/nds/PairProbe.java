import java.nio.file.*;import java.util.*;
public class PairProbe{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int s16(byte[] b,int o){int v=u16(b,o);return v>=0x8000?v-0x10000:v;}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 static int[][] ev;
 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  ev=narcIndex("/a/1/2/5");
  int src=Integer.parseInt(a[1]);
  byte[] f=file(src);int npc=f[4]&0xFF,second=f[5]&0xFF,warps=f[6]&0xFF;int p=8+npc*20+second*36;
  System.out.println("map "+src+" warps="+warps);
  for(int i=0;i<warps;i++){int o=p+i*20;
   int dest=s16(f,o),at2=s16(f,o+2),b5=f[o+5]&0xFF,tb=s16(f,o+14),d60=s16(f,o+16),last=s16(f,o+18);
   int destWarps=dest>=0&&dest<ev.length?countWarps(dest):-1;
   System.out.printf("  warp %-2d -> map %-4d off2=%-4d b5=%-3d TB=%-3d d60=%-3d off18=%-4d destWarpCount=%d%n",
     i,dest,at2,b5,tb,d60,last,destWarps);
  }
 }
 static int countWarps(int idx){byte[] f=file(idx);if(f.length<8)return 0;return f[6]&0xFF;}
 static byte[] file(int i){return Arrays.copyOfRange(rom,ev[i][0],ev[i][1]);}
 static int[][] narcIndex(String path){
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get(path)*8);
  int p=s+0x10;int[] st=null,en=null;int img=0;int count=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){count=u16(rom,p+8);st=new int[count];en=new int[count];
    for(int i=0;i<count;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  int[][] out=new int[count][2];
  for(int i=0;i<count;i++){out[i][0]=img+st[i];out[i][1]=img+en[i];}
  return out;}
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p=sub;
  while(true){int t=rom[p]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p+1+l)&0x0FFF;p+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
