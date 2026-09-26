import java.nio.file.*;import java.util.*;
/**
 * Chargestone Cave's electric stones, read from the cave's own code overlay (51 in White):
 * two index tables of (map header, count, RAM pointer) - small pushable stones and big charged
 * stones - pointing at i32 triples (tile x, floor level, tile z). Found 2026-09-25 by scoring every
 * ROM file for the client's big-stone table (13/13 as i32 pairs here). The overlay's code
 * (0x21F3D58) creates each small stone as field object code 253; a stone touching a charged one is
 * charged; a loose stone slides up to 32 tiles toward a charged stone in the push direction and
 * stops beside it (0x21F3BE4); a charged stone never moves.
 * Rows: `cstone;2;bank;map;kind;x;y;level` with kind small|big.
 * Usage: java tools/nds/Chargestone5.java <white.nds> server.game/nds-chargestone-2.txt
 */
public class Chargestone5{
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 static byte[] blz(byte[] in){int n=in.length;int foot=u32(in,n-8);int hdr=foot>>>24;int csize=foot&0xFFFFFF;int extra=u32(in,n-4);if(hdr<8||csize>n)return in;
  byte[] out=new byte[n+extra];int start=n-csize;System.arraycopy(in,0,out,0,start);int src=n-hdr,dst=out.length;
  while(dst>start&&src>start){int fl=in[--src]&0xFF;for(int b=0;b<8&&dst>start&&src>start;b++,fl<<=1){if((fl&0x80)==0)out[--dst]=in[--src];else{int b1=in[--src]&0xFF,b2=in[--src]&0xFF;int len=(b1>>4)+3,disp=((b1&0xF)<<8|b2)+3;for(int k=0;k<len&&dst>start;k++){out[dst-1]=out[dst-1+disp];dst--;}}}}return out;}
 public static void main(String[] a)throws Exception{
  byte[] rom=Files.readAllBytes(Paths.get(a[0]));int ovt=u32(rom,0x50),fat=u32(rom,0x48);byte[] f=null;int ram=0;
  for(int i=0;i<u32(rom,0x54)/32;i++){int e=ovt+i*32;if(u32(rom,e)!=51)continue;ram=u32(rom,e+4);int fid=u32(rom,e+24),fl=u32(rom,e+28);f=Arrays.copyOfRange(rom,u32(rom,fat+fid*8),u32(rom,fat+fid*8+4));if((fl>>>24&1)!=0)f=blz(f);}
  // The two lookups (0x21F36E0 small, 0x21F3718 big) load their index table address from a literal.
  int smallIdx=u32(f,0x21F36FC-ram)-ram, bigIdx=u32(f,0x21F3734-ram)-ram;
  StringBuilder out=new StringBuilder("# cstone;region;bank;map;kind;x;y;level - Chargestone Cave stones from overlay 51 (Chargestone5)\n");int rows=0;
  for(Object[] t:new Object[][]{{"small",smallIdx},{"big",bigIdx}}){int idx=(Integer)t[1];
   for(int k=0;k<3;k++){int o=idx+k*12;int hdr=u32(f,o),cnt=u32(f,o+4),ptr=u32(f,o+8)-ram;
    for(int j=0;j<cnt;j++){int p=ptr+j*12;out.append("cstone;2;").append(hdr&0xFF).append(';').append(hdr>>8).append(';').append(t[0]).append(';').append(u32(f,p)).append(';').append(u32(f,p+8)).append(';').append(u32(f,p+4)).append('\n');rows++;}}}
  Files.write(Paths.get(a[1]),out.toString().getBytes("UTF-8"));System.out.println("rows "+rows);
 }
}
