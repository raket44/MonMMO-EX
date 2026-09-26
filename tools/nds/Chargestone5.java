import java.nio.file.*;import java.util.*;
/**
 * Chargestone Cave's electric stones, read from the cave's own code overlay (51 in White):
 * two index tables of (map header, count, RAM pointer) - small pushable stones and big charged
 * stones - pointing at i32 triples (tile x, floor level, tile z). Found 2026-09-25 by scoring every
 * ROM file for the client's big-stone table (13/13 as i32 pairs here). The overlay's code
 * (0x21F3D58) creates each small stone as field object code 253; a stone touching a charged one is
 * charged; a loose stone slides up to 32 tiles toward a charged stone in the push direction and
 * stops beside it (0x21F3BE4); a charged stone never moves.
 * Rows: `cstone;2;bank;map;kind;x;y;level` with kind small|big, then the floors' tiles a stone can
 * slide on and stop against: `ctile;2;bank;map;x;y;floor|rock` - floor is a tile record with flags
 * 0x84 (0x81 is the cave's solid mass, not floor), rock is behaviour 0x32 with flags 0x01 (the big
 * stones' footprints). Everything else is wall. The stone at (15,39) slides LEFT 3 into the rock at
 * x 9-11 - the owner's push - and most stones get exactly one such direction.
 * Usage: java tools/nds/Chargestone5.java <white.nds> server.game/nds-chargestone-2.txt
 */
public class Chargestone5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 static byte[] blz(byte[] in){int n=in.length;int foot=u32(in,n-8);int hdr=foot>>>24;int csize=foot&0xFFFFFF;int extra=u32(in,n-4);if(hdr<8||csize>n)return in;
  byte[] out=new byte[n+extra];int start=n-csize;System.arraycopy(in,0,out,0,start);int src=n-hdr,dst=out.length;
  while(dst>start&&src>start){int fl=in[--src]&0xFF;for(int b=0;b<8&&dst>start&&src>start;b++,fl<<=1){if((fl&0x80)==0)out[--dst]=in[--src];else{int b1=in[--src]&0xFF,b2=in[--src]&0xFF;int len=(b1>>4)+3,disp=((b1&0xF)<<8|b2)+3;for(int k=0;k<len&&dst>start;k++){out[dst-1]=out[dst-1+disp];dst--;}}}}return out;}
 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));int ovt=u32(rom,0x50),fat=u32(rom,0x48);byte[] f=null;int ram=0;
  for(int i=0;i<u32(rom,0x54)/32;i++){int e=ovt+i*32;if(u32(rom,e)!=51)continue;ram=u32(rom,e+4);int fid=u32(rom,e+24),fl=u32(rom,e+28);f=Arrays.copyOfRange(rom,u32(rom,fat+fid*8),u32(rom,fat+fid*8+4));if((fl>>>24&1)!=0)f=blz(f);}
  // The two lookups (0x21F36E0 small, 0x21F3718 big) load their index table address from a literal.
  int smallIdx=u32(f,0x21F36FC-ram)-ram, bigIdx=u32(f,0x21F3734-ram)-ram;
  StringBuilder out=new StringBuilder("# cstone;region;bank;map;kind;x;y;level - Chargestone Cave stones from overlay 51 (Chargestone5)\n");int rows=0;
  for(Object[] t:new Object[][]{{"small",smallIdx},{"big",bigIdx}}){int idx=(Integer)t[1];
   for(int k=0;k<3;k++){int o=idx+k*12;int hdr=u32(f,o),cnt=u32(f,o+4),ptr=u32(f,o+8)-ram;
    for(int j=0;j<cnt;j++){int p=ptr+j*12;out.append("cstone;2;").append(hdr&0xFF).append(';').append(hdr>>8).append(';').append(t[0]).append(';').append(u32(f,p)).append(';').append(u32(f,p+8)).append(';').append(u32(f,p+4)).append('\n');rows++;}}}
  // Floor and rock tiles of the three cave floors, from each header's matrix cells.
  walk(u32(rom,0x40),0,"");byte[] hdrs=narcFile("/a/0/1/2",0);int[][] mx=narcIndex("/a/0/0/9"),mp=narcIndex("/a/0/0/8");int tiles=0;
  for(int h:new int[]{195,196,197}){int m=u16(hdrs,h*48+4);byte[] mf=Arrays.copyOfRange(rom,mx[m][0],mx[m][1]);int w=u16(mf,4),hh=u16(mf,6);
   for(int cy=0;cy<hh;cy++)for(int cx=0;cx<w;cx++){int id=u32(mf,8+(cy*w+cx)*4);if(id<0||id>=mp.length)continue;byte[] mp1=Arrays.copyOfRange(rom,mp[id][0],mp[id][1]);if(mp1.length<12)continue;int off1=u32(mp1,8);int tw=u16(mp1,off1),th=u16(mp1,off1+2);int p=off1+4;
    for(int y=0;y<th;y++)for(int x=0;x<tw;x++){int o=p+(y*tw+x)*8;int beh=u16(mp1,o+4),fl=u16(mp1,o+6);String k=fl==0x84?"floor":(beh==0x32&&fl==0x01)?"rock":null;if(k==null)continue;
     out.append("ctile;2;").append(h&0xFF).append(';').append(h>>8).append(';').append(cx*32+x).append(';').append(cy*32+y).append(';').append(k).append('\n');tiles++;}}}
  Files.write(Paths.get(a[1]),out.toString().getBytes("UTF-8"));System.out.println("rows "+rows); System.out.println("tiles "+tiles);
 }
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
