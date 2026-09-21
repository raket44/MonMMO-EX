import java.nio.file.*;import java.util.*;
/**
 * Unova (Black/White) tile permissions.
 *
 * Every map header (/a/0/1/2, 48-byte records) names its MATRIX at +4 - /a/0/0/9 holds 255 of them,
 * not one: matrix 0 is the 29x27 overworld and the rest are the interiors and caves, mostly 1x1 or
 * 1x2. Verified 2026-09-21 against White (IRAO): +4 is in range for all 427 headers and reaches
 * 427/427 headers and 595/649 map files, where walking matrix 0 alone reached 40 headers and 109
 * files, which is why Unova had no cave or building land data at all. (+0 is NOT the matrix id: it
 * takes only 34 distinct values and is a map-type/flags field.)
 *
 * A matrix is u16 w,h at +4/+6, then w*h u32 map-file ids, then - only on the big overworld ones,
 * so test the length - a w*h u32 header plane giving the owning header per cell. Where there is no
 * header plane the cells belong to the header that named the matrix.
 *
 * Map file (/a/0/0/8): magic, u16 sections, u32 section offsets; section 1 (off at +8) = permissions:
 * u16 w, u16 h, then w*h records of 8 bytes (u16 behaviour, u16 height, u16 f2, u16 flags; flags
 * bit 0 set = walkable).
 *
 * Modes: probe <rom> <cx> <cy>...   ASCII map + histogram for overworld cells
 *        map   <rom> <mapFile>...   ASCII map + histogram for a map file
 *        dump  <rom> <out>          every map's tiles as region;bank;map;x;y;type;coll
 */
public class Land5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 /**
  * Gen 5 tile record -> the Gen 4 metatile number the server reads (NdsLand).
  *
  * The record is four u16: [0] MOVEMENT PERMISSION (a directional mask - 0x04/0x08/0x10/0x20 are
  * the four edges, combined on corners, which is why building outlines show up in it), [1] an
  * index, [2] the TERRAIN, [3] flags whose BIT 0 means BLOCKED.
  *
  * Established 2026-09-21 against ground truth, not by eye - the previous reading took [0] as the
  * terrain and had bit 0 backwards, which is why Unova never gave a wild encounter:
  *  - every npc stands on a walkable tile. Of 600 that resolve on the overworld matrix, 487 sit on
  *    f3 == 0x0080 (bit 0 clear) though that is only 9.5% of the world, while f3 == 0x0081 is 82%
  *    of the world and holds just 10 of them. Bit 0 set = blocked.
  *  - by terrain value, of the headers that carry it: 0x04 -> 18 headers, ALL with a retail Grass
  *    table; 0x06 -> 16 headers, all with Grass and all 16 with Dark Grass; 0x17, 0x19 and 0x3f ->
  *    every header carrying them has a Water table.
  * Cave floor is not resolvable from the overworld matrix (caves are interiors on other matrices),
  * so it is deliberately left unmapped rather than guessed.
  */
 static int type(int terrain){
  switch(terrain){
   case 0x04: return 2;   // TALL_GRASS
   case 0x06: return 3;   // VERY_TALL_GRASS, which is Unova's dark grass
   case 0x17: case 0x19: case 0x3f: return 21;  // WATER_SEA
   default: return 0;
  }
 }
 public static void main(String[] a)throws Exception{rom=Files.readAllBytes(Paths.get(a[1]));walk(u32(rom,0x40),0,"");
  byte[] mx=narcFile("/a/0/0/9",0);int w=u16(mx,4),h=u16(mx,6);
  if(a[0].equals("map")){for(int k=2;k<a.length;k++){int mf=Integer.parseInt(a[k]);byte[] f=narcFile("/a/0/0/8",mf);int off1=u32(f,8);int tw=u16(f,off1),th=u16(f,off1+2);int p=off1+4;System.out.println("mapFile "+mf+" plane "+tw+"x"+th);StringBuilder map=new StringBuilder();for(int y=0;y<th;y++){for(int x=0;x<tw;x++){int o=p+(y*tw+x)*8;int f0=u16(f,o),f3=u16(f,o+6);map.append((f3&1)==0?'#':(f0==0?'.':(f0==0x10?'g':(f0==0x08?'w':'?'))));}map.append((char)10);}Map<String,Integer> hist=new TreeMap<>();for(int y=0;y<th;y++)for(int x=0;x<tw;x++){int o=p+(y*tw+x)*8;hist.merge(String.format("%04x %04x %04x %04x",u16(f,o),u16(f,o+2),u16(f,o+4),u16(f,o+6)),1,Integer::sum);}System.out.println(map);hist.entrySet().stream().sorted((x,y)->y.getValue()-x.getValue()).limit(14).forEach(en->System.out.println("  "+en.getValue()+"  "+en.getKey()));}return;}
  if(a[0].equals("probe")){
   for(int k=2;k+1<a.length;k+=2){int cx=Integer.parseInt(a[k]),cy=Integer.parseInt(a[k+1]);int mapFile=u32(mx,8+(cy*w+cx)*4);int hdr=u32(mx,8+w*h*4+(cy*w+cx)*4);
    byte[] f=narcFile("/a/0/0/8",mapFile);int off1=u32(f,8);int p=off1+4;int tw=u16(f,off1),th=u16(f,off1+2);System.out.println("cell "+cx+","+cy+" mapFile "+mapFile+" header "+hdr+" plane "+tw+"x"+th);
    Map<String,Integer> hist=new TreeMap<>();StringBuilder map=new StringBuilder();
    for(int y=0;y<th;y++){for(int x=0;x<tw;x++){int o=p+(y*tw+x)*8;int f0=u16(f,o),f1=u16(f,o+2),f2=u16(f,o+4),f3=u16(f,o+6);hist.merge(String.format("%04x %04x %04x %04x",f0,f1,f2,f3),1,Integer::sum);
      char c=(f3&1)==0?'#':(f0==0?'.':(f0==0x10?'g':(f0==0x08?'w':(f0==0x18?'t':'?'))));map.append(c);}map.append('\n');}
    System.out.println(map);hist.entrySet().stream().sorted((x,y)->y.getValue()-x.getValue()).limit(16).forEach(en->System.out.println("  "+en.getValue()+"  "+en.getKey()));}
   return;}
  int[][] mxIdx=narcIndex("/a/0/0/9");int[][] mapIdx=narcIndex("/a/0/0/8");
  byte[] hdrs=narcFile("/a/0/1/2",0);int rec=48,headers=hdrs.length/rec;
  StringBuilder out=new StringBuilder();int cells=0;Set<Integer> planeDone=new HashSet<>();Set<Integer> seenMaps=new TreeSet<>();
  for(int hi=0;hi<headers;hi++){
   int m=u16(hdrs,hi*rec+4);if(m>=mxIdx.length)continue;
   byte[] mf=Arrays.copyOfRange(rom,mxIdx[m][0],mxIdx[m][1]);if(mf.length<8)continue;
   int mw=u16(mf,4),mh=u16(mf,6);if(mw<=0||mh<=0||8+mw*mh*4>mf.length)continue;
   boolean plane=mf.length>=8+mw*mh*8;
   // A matrix with a header plane owns its own cells, so walk it once however many headers name it.
   if(plane&&!planeDone.add(m))continue;
   for(int c=0;c<mw*mh;c++){
    long file=u32(mf,8+c*4)&0xFFFFFFFFL;if(file==0xFFFFFFFFL||file>=mapIdx.length)continue;
    long owner=plane?(u32(mf,8+mw*mh*4+c*4)&0xFFFFFFFFL):hi;if(owner>=headers)continue;
    byte[] f=Arrays.copyOfRange(rom,mapIdx[(int)file][0],mapIdx[(int)file][1]);if(f.length<16)continue;
    int off1=u32(f,8);if(off1+4>f.length)continue;
    int tw=u16(f,off1),th=u16(f,off1+2);int p=off1+4;if(tw<=0||th<=0||p+tw*th*8>f.length)continue;
    cells++;seenMaps.add((int)file);
    int bank=(int)(owner&0xFF),map=(int)(owner>>8);int cx=c%mw,cy=c/mw;
    for(int y=0;y<th;y++)for(int x=0;x<tw;x++){int o=p+(y*tw+x)*8;int terrain=u16(f,o+4),flags=u16(f,o+6);
     out.append("2;").append(bank).append(';').append(map).append(';').append(cx*32+x).append(';').append(cy*32+y)
        .append(';').append(type(terrain)).append(';').append((flags&1)!=0?0x80:0).append('\n');}}}
  Files.write(Paths.get(a[2]),out.toString().getBytes("UTF-8"));
  System.out.println("cells "+cells+"  distinct map files "+seenMaps.size()+" / "+mapIdx.length);
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
