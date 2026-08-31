import java.nio.file.*;import java.util.*;

/**
 * Gen 4 (Platinum, region 3) warp/spawn table extractor - the Johto/Sinnoh counterpart of
 * Warps.java, mirroring the CLIENT's own parsers exactly:
 *
 * - Header table: the client (f.QO1) SCANS decompressed arm9 for two consecutive u32 markers
 *   (1573448, 1573449 for Platinum, me1==3) and reads 24-byte records right after them
 *   (f.rY.K80, me1==3 branch): u8 areaData(iU1), u8 -, u16 MATRIX(aX), u16 scripts, u16 -,
 *   u16 -, u16 m00, u16 XW0, u16 -, u16 EVENTS(na), u8, u8, u8, u8, u16. Header count =
 *   size(/fielddata/maptable/mapname.bin) / 16.
 *
 * - Zone events (f.CoM2.lL0 -> f.HS0): /fielddata/eventdata/zone_event.narc file[events id]:
 *   u32 n, n x 20-byte furniture (f.qL, ten u16) ; u32 n, n x 32-byte NPCs (f.Qi, sixteen u16) ;
 *   u32 n, n x 12-byte WARPS (f.ph0): s16 x, s16 y, s16 destHeader, u16 anchor, u32 height ;
 *   u32 n, n x 16-byte triggers (f.U31). Gen 4 warps are direction-less single tiles: no dir,
 *   no mode, no box - contact warps (dir column = -1).
 *
 * - World placement: /fielddata/mapmatrix/map_matrix.narc file 0, cells are 32x32 tiles; a
 *   header on matrix 0 gets global coords = cellOrigin*32 + local warp coords, matching the
 *   global coordinates the client reports for outdoor movement.
 *
 * Usage: java Warps4.java <rom.nds> <3|4> [dump <header>] - default emits nds-warps rows on
 * stdout and spawns rows on stderr, same column formats as Warps.java.
 */
public class Warps4{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int s16(byte[] b,int o){int v=u16(b,o);return v>=0x8000?v-0x10000:v;}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}

 record W(int x,int y,int dest,int anchor){}
 record Hdr(int matrix,int events){}

 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  int region=Integer.parseInt(a[1]);
  // Per-region constants, all read out of the CLIENT: Platinum loader f.CoM2 (me1==3),
  // HeartGold loader f.kl (me1==4); markers from f.QO1, header offsets from f.rY.K80.
  int mA=region==3?1573448:33489410, mB=region==3?1573449:11076050;
  int skip=region==3?0:8, offMatrix=region==3?2:4, offEvents=16;
  String evPath=region==3?"/fielddata/eventdata/zone_event.narc":"/a/0/3/2";
  String mxPath=region==3?"/fielddata/mapmatrix/map_matrix.narc":"/a/0/5/6";

  byte[] arm9=arm9();
  int tab=scanPair(arm9,mA,mB);
  if(tab<0){arm9=blz(arm9);tab=scanPair(arm9,mA,mB);}
  if(tab<0)throw new RuntimeException("header-table marker not found in arm9 (raw or BLZ)");
  tab+=skip;

  int fat=u32(rom,0x48);
  int nameIdx=paths.get("/fielddata/maptable/mapname.bin");
  int nameSize=u32(rom,fat+nameIdx*8+4)-u32(rom,fat+nameIdx*8);
  int maps=nameSize/16;
  System.err.println("# headers="+maps+" tableAt=arm9+0x"+Integer.toHexString(tab));

  Hdr[] hdr=new Hdr[maps];
  for(int i=0;i<maps;i++){int o=tab+i*24;hdr[i]=new Hdr(u16(arm9,o+offMatrix),u16(arm9,o+offEvents));}

  // Matrix 0: per-header world origin (min cell * 32).
  byte[] mx=narcFile(mxPath,0);
  int mw=mx[0]&0xFF,mh=mx[1]&0xFF,fH=mx[2]&0xFF,fA=mx[3]&0xFF,nl=mx[4]&0xFF;
  int p=5+nl;
  int[] originX=new int[maps],originY=new int[maps];Arrays.fill(originX,-1);
  if(fH!=0){
   for(int r=0;r<mh;r++)for(int c=0;c<mw;c++){
    int h=u16(mx,p+(r*mw+c)*2);
    if(h<maps&&originX[h]<0){originX[h]=c*32;originY[h]=r*32;}
   }
  }
  System.err.println("# matrix0 "+mw+"x"+mh+" headersLayer="+fH+" altLayer="+fA);

  List<List<W>> events=new ArrayList<>();
  int[][] evIdx=narcIndex(evPath);
  for(int i=0;i<evIdx.length;i++)events.add(parseWarps(Arrays.copyOfRange(rom,evIdx[i][0],evIdx[i][1])));

  if(a.length>3&&a[2].equals("dump")){
   int i=Integer.parseInt(a[3]);
   System.out.println("header "+i+" matrix="+hdr[i].matrix()+" events="+hdr[i].events()
     +" origin=("+originX[i]+","+originY[i]+")");
   for(W w:events.get(hdr[i].events()))System.out.println("  "+w);
   return;
  }

  // Warp rows. Gen 4 warp records store their coordinates NATIVELY in the frame the client
  // reports: GLOBAL world coordinates for matrix-0 zones (verified: Hearthome's warp (303,756)
  // IS the GM hotspot tile), local coordinates for interiors. Nothing to convert.
  int rows=0,dropped=0;
  StringBuilder warps=new StringBuilder(),spawns=new StringBuilder();
  for(int i=0;i<maps;i++){
   List<W> ws=hdr[i].events()<events.size()?events.get(hdr[i].events()):List.of();
   int m=hdr[i].matrix();
   // Spawn: first warp tile, else the cell centre for placed world maps.
   if(!ws.isEmpty()){
    W f=ws.get(0);
    spawns.append(region+";"+(i&0xFF)+";"+(i>>8)+";"+f.x()+";"+f.y()+";"+m+"\n");
   }else if(m==0&&originX[i]>=0){
    spawns.append(region+";"+(i&0xFF)+";"+(i>>8)+";"+(originX[i]+16)+";"+(originY[i]+16)+";"+m+"\n");
   }
   for(W w:ws){
    int d=w.dest();
    if(d<0||d>=maps){dropped++;continue;}
    List<W> dws=hdr[d].events()<events.size()?events.get(hdr[d].events()):List.of();
    if(w.anchor()>=dws.size()){dropped++;continue;}
    W t=dws.get(w.anchor());
    warps.append(region+";"+(i&0xFF)+";"+(i>>8)+";"+w.x()+";"+w.y()+";-1;"
      +(d&0xFF)+";"+(d>>8)+";"+t.x()+";"+t.y()+";-1;-1;"+m+";0\n");
    rows++;
   }
  }
  System.out.print(warps);
  System.err.print(spawns);
  System.err.println("# warp rows="+rows+" dropped="+dropped);
 }

 static List<W> parseWarps(byte[] f){
  List<W> out=new ArrayList<>();
  int p=0;
  int n1=u32(f,p);p+=4+n1*20;
  int n2=u32(f,p);p+=4+n2*32;
  int nw=u32(f,p);p+=4;
  for(int i=0;i<nw;i++){int o=p+i*12;
   out.add(new W(s16(f,o),s16(f,o+2),s16(f,o+4),u16(f,o+6)));}
  return out;
 }

 /** Raw arm9 image from the ROM header (offset 0x20, size 0x2C). */
 static byte[] arm9(){int off=u32(rom,0x20),size=u32(rom,0x2C);return Arrays.copyOfRange(rom,off,off+size);}

 /** 4-byte-stride scan for two consecutive u32s, returning the offset AFTER them (f.bW0.BZ). */
 static int scanPair(byte[] b,int x,int y){
  for(int o=0;o+8<=b.length;o+=4)if(u32(b,o)==x&&u32(b,o+4)==y)return o+8;
  return -1;
 }

 /** BLZ (backwards LZ) decompression of an arm9 image; returns input unchanged if no footer. */
 static byte[] blz(byte[] in){
  int n=in.length;
  int extra=u32(in,n-4);
  int info=u32(in,n-8);
  int headerLen=(info>>>24)&0xFF,compLen=info&0xFFFFFF;
  if(headerLen<8||headerLen>32||compLen<=headerLen||compLen>n)return in;
  byte[] out=new byte[n+extra];
  System.arraycopy(in,0,out,0,n-compLen);
  int src=n-headerLen,dst=n+extra,end=n-compLen;
  while(src>end&&dst>end){
   int flags=in[--src]&0xFF;
   for(int bit=0;bit<8&&src>end&&dst>end;bit++){
    if((flags&0x80)!=0){
     int b1=in[--src]&0xFF,b2=in[--src]&0xFF;
     int len=(b1>>4)+3,disp=((b1&0x0F)<<8|b2)+3;
     for(int j=0;j<len;j++){out[dst-1]=out[dst-1+disp];dst--;}
    }else out[--dst]=in[--src];
    flags<<=1;
   }
  }
  return out;
 }

 static int[][] narcIndex(String path){
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get(path)*8);
  int p=s+0x10;int[] st=null,en=null;int img=0;int count=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){count=u16(rom,p+8);st=new int[count];en=new int[count];
    for(int i=0;i<count;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  int[][] out=new int[count][2];
  for(int i=0;i<count;i++){out[i][0]=img+st[i];out[i][1]=img+en[i];}
  return out;
 }
 static byte[] narcFile(String p,int i){int[][] x=narcIndex(p);return Arrays.copyOfRange(rom,x[i][0],x[i][1]);}
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p=sub;
  while(true){int t=rom[p]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p+1+l)&0x0FFF;p+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
