import java.nio.file.*;import java.util.*;
/**
 * Unova permission-record statistics under known-walkable tiles (npc positions). For each npc row of
 * nds-npcs-2.txt the header (map<<8|bank) is resolved to its matrix through the header field at
 * +OFF (tried for 2 and 4), the cell/tile record read, and the (f1 height, f2, f3) histogram printed.
 * Usage: Stat5 <rom> <nds-npcs-2.txt> <headerFieldOffset>
 */
public class Stat5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[] a)throws Exception{rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  int off=Integer.parseInt(a[2]);byte[] hdr=narcFile("/a/0/1/2",0);int[][] mxIdx=narcIndex("/a/0/0/9"),mapIdx=narcIndex("/a/0/0/8");
  Map<String,Integer> hist=new TreeMap<>(),histIn=new TreeMap<>();int inside=0,outside=0,world=0,interior=0;
  for(String l:Files.readAllLines(Paths.get(a[1]))){if(!l.startsWith("obj;"))continue;String[] p=l.split(";");int bank=Integer.parseInt(p[2]),map=Integer.parseInt(p[3]);int x=Integer.parseInt(p[14]),y=Integer.parseInt(p[15]);
   int h=(map<<8)|bank;if(h*48+48>hdr.length){outside++;continue;}int mx=u16(hdr,h*48+off);if(mx>=mxIdx.length){outside++;continue;}
   byte[] m=Arrays.copyOfRange(rom,mxIdx[mx][0],mxIdx[mx][1]);int flag=u16(m,0),w=u16(m,4),hh=u16(m,6);int cx=x/32,cy=y/32;if(x<0||y<0||cx>=w||cy>=hh){outside++;continue;}
   long mf=u32(m,8+(cy*w+cx)*4)&0xFFFFFFFFL;if(mf==0xFFFFFFFFL||mf>=mapIdx.length){outside++;continue;}
   byte[] f=Arrays.copyOfRange(rom,mapIdx[(int)mf][0],mapIdx[(int)mf][1]);if(f.length<16){outside++;continue;}int off1=u32(f,8);int tw=u16(f,off1),th=u16(f,off1+2);int pp=off1+4;int lx=x%32,ly=y%32;if(lx>=tw||ly>=th){outside++;continue;}
   int o=pp+(ly*tw+lx)*8;if(o+8>f.length){outside++;continue;}inside++;if(mx==0)world++;else interior++;
   String key=String.format("h%s f2=%04x f3=%04x",u16(f,o+2)==0?"0":"X",u16(f,o+4),u16(f,o+6));hist.merge(key,1,Integer::sum);if(mx!=0)histIn.merge(key,1,Integer::sum);}
  System.out.println("off "+off+": inside "+inside+" (world "+world+", other matrices "+interior+") outside "+outside);
  System.out.println("-- all"); hist.entrySet().stream().sorted((x,y)->y.getValue()-x.getValue()).limit(20).forEach(en->System.out.println("  "+en.getValue()+"  "+en.getKey()));
  System.out.println("-- non-world"); histIn.entrySet().stream().sorted((x,y)->y.getValue()-x.getValue()).limit(20).forEach(en->System.out.println("  "+en.getValue()+"  "+en.getKey()));}
 static int[][] narcIndex(String path){
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get(path)*8);int p=s+0x10;int[] st=null,en=null;int img=0;int count=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){count=u16(rom,p+8);st=new int[count];en=new int[count];for(int i=0;i<count;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  int[][] out=new int[count][2];for(int i=0;i<count;i++){out[i][0]=img+st[i];out[i][1]=img+en[i];}return out;}
 static byte[] narcFile(String p,int i){int[][] x=narcIndex(p);return Arrays.copyOfRange(rom,x[i][0],x[i][1]);}
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p=sub;
  while(true){int t=rom[p]&0xFF;if(t==0)break;if(t<0x80){paths.put(pre+"/"+new String(rom,p+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p+1,l,java.nio.charset.StandardCharsets.US_ASCII);int sd=u16(rom,p+1+l)&0x0FFF;p+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
