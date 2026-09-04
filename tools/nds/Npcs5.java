import java.nio.file.*;import java.util.*;

/**
 * Decodes the per-map event files (/a/1/2/5) into the server's warp tables.
 *
 * Event file: u32 payload size, four u8 counts (npc, second, warp, trigger), then the arrays in
 * that order - npc records 20 bytes, second array 36, warp records 20.
 *
 * Warp record, field order from the client's own Gen 5 parser f.IV1:
 *   +0  s16 destination map index
 *   +2  s16 destination WARP index within that map (the client ignores it; warps are paired, so
 *          this is what makes a building exit come out of its own door)
 *   +4  u8  exit direction, raw 1-4 mapped by f.NN.Ai1 to 0-3 = DOWN, UP, LEFT, RIGHT
 *   +5  u8  mode byte (f.NN.Mi1)
 *   +6  s16 flag: 0 = tile warp, 1 = RAIL warp (Castelia main city, Skyarrow Bridge, the League
 *          lobby, Gear Station... - the 3D camera-rail areas)
 *   tile warp:  +8/+10/+12 s16 x, z, y - fixed point, 16 units per tile (client's QR0() >> 4)
 *   rail warp:  +8 s16 rail line id, +10 s16 x, +12 s16 y - raw rail coordinates, no shift.
 *          The client's hit test (f.UN.y81) compares the player's position to these RAW, so on
 *          rail maps the movement packets carry rail coordinates directly.
 *   +14 s16 box width, +16 s16 box height - the warp covers [x, x+w) x [y, y+h) (street mouths
 *          in Castelia are 14 tiles wide; a one-tile door is w=h=1)
 *
 * Modes:
 *   dump <index>   print one map's warps
 *   spawns <out>   fallback entry tile per map (its first warp, else the header world position)
 *   <out>          every warp TILE as region;bank;map;x;y;dir;destBank;destMap;destX;destY
 */
public class Npcs5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int[][] ev;
 /** Per map: event file id (header bytes 22-23, field na in the client's f.Hg - NOT always the
  *  map index) and matrix id (header bytes 4-5, field aX; 0 = the seamless world matrix). */
 static int[] evId,matrixId,locId;
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int s16(byte[] b,int o){int v=u16(b,o);return v>=0x8000?v-0x10000:v;}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}

 /** mode = the record's byte at +5 (client field f.NN.Mi1): the transition KIND. The client's
  *  door-walk routine G41 selects the entry animation and scripted movement from it - doors,
  *  stairs, edge crossings all carry their choreography in the ROM itself. */
 record Warp(int dest,int destWarp,int x,int y,int dir,boolean rail,int line,int w,int h,int mode){}
 static long tile(int map,int x,int y){return ((long)map<<40)|((long)(x&0xFFFFF)<<20)|(y&0xFFFFF);}

 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  ev=narcIndex("/a/1/2/5");
  byte[] hdr=narcFile("/a/0/1/2",0);
  List<String> loc=decode(narcFile("/a/0/0/2",89));
  int rec=48,maps=hdr.length/rec;
  evId=new int[maps];matrixId=new int[maps];locId=new int[maps];
  int evMismatch=0;
  for(int i=0;i<maps;i++){
   evId[i]=u16(hdr,i*rec+22);
   matrixId[i]=u16(hdr,i*rec+4);
   locId[i]=hdr[i*rec+26]&0xFF;
   if(evId[i]!=i)evMismatch++;
  }
  System.out.println("maps whose event file differs from their index: "+evMismatch);
  loadWorldPlane();
  buildPlaneByMatrix(maps);

  // Every map's npc (20 B) and "second" (36 B) records from /a/1/2/5, raw as u16 columns plus
  // the two fixed-point coordinate candidates >> 4, so the field roles can be pinned against the
  // known warp/land frame before the server reads them.
  java.io.PrintWriter out=new java.io.PrintWriter(a[1],"UTF-8");
  int npcs=0,seconds=0;
  for(int i=0;i<maps;i++){
   if(i<0||i>=evId.length)continue;
   int file=evId[i];
   if(file<0||file>=ev.length)continue;
   byte[] f=Arrays.copyOfRange(rom,ev[file][0],ev[file][1]);
   if(f.length<8)continue;
   int npc=f[4]&0xFF,second=f[5]&0xFF;
   int p=8;
   for(int k=0;k<npc&&p+20<=f.length;k++,p+=20){
    StringBuilder sb=new StringBuilder("npc;2;"+(i&0xFF)+";"+(i>>8)+";"+k);
    for(int o=0;o<20;o+=2)sb.append(';').append(s16(f,p+o));
    out.println(sb);npcs++;
   }
   for(int k=0;k<second&&p+36<=f.length;k++,p+=36){
    StringBuilder sb=new StringBuilder("second;2;"+(i&0xFF)+";"+(i>>8)+";"+k);
    for(int o=0;o<36;o+=2)sb.append(';').append(s16(f,p+o));
    out.println(sb);seconds++;
   }
  }
  out.close();
  System.err.println("# npcs="+npcs+" second="+seconds);
 }

 static int wpW,wpH;static int[][] worldHeader;static Set<Integer> onWorldPlane=new HashSet<>();
 static void loadWorldPlane(){
  byte[] f=narcFile("/a/0/0/9",0);
  wpW=u16(f,4);wpH=u16(f,6);
  worldHeader=new int[wpW][wpH];
  int hdrPlane=8+wpW*wpH*4;
  for(int y=0;y<wpH;y++)for(int x=0;x<wpW;x++){
   long v=u32(f,hdrPlane+(y*wpW+x)*4)&0xFFFFFFFFL;
   worldHeader[x][y]=v==0xFFFFFFFFL?-1:(int)v;
   if(worldHeader[x][y]>=0)onWorldPlane.add(worldHeader[x][y]);
  }
 }

 /** The canonical header for a map at world-scale coordinates; variants map to the plane owner. */
 static int canonical(int map,int x,int y){
  if(onWorldPlane.contains(map))return map;
  int cx=x>>5,cy=y>>5;
  if(cx>=0&&cy>=0&&cx<wpW&&cy<wpH){
   int owner=worldHeader[cx][cy];
   if(owner>=0)return owner;
  }
  // Local-frame variants: an off-plane map whose matrix also hosts exactly one ON-plane map is
  // that map's story twin. Otherwise fall back to the twin-cluster canonical (lowest index).
  Integer sibling=planeByMatrix.get(matrixId[map]);
  if(sibling!=null)return sibling;
  return twinCanonical.getOrDefault(map,map);
 }

 static Map<Integer,Integer> planeByMatrix=new HashMap<>();
 /** For each OFF-plane matrix hosting several maps (story-twin clusters like drawbridge 253/301,
  *  Marvelous Bridge 263/303), the canonical member: the lowest header index - Unova's variant
  *  headers are appended after the originals. Every ROM warp record exists regardless of the
  *  story flags that gate it in the real game, so doors into the non-canonical twin sit as live
  *  phantom warps on open ground unless redirected here. */
 static Map<Integer,Integer> twinCanonical=new HashMap<>();
 static void buildPlaneByMatrix(int maps){
  Map<Integer,Integer> counts=new HashMap<>();
  for(int m=0;m<maps;m++)if(onWorldPlane.contains(m)){
   counts.merge(matrixId[m],1,Integer::sum);
   planeByMatrix.putIfAbsent(matrixId[m],m);
  }
  counts.forEach((mx,c)->{if(c>1)planeByMatrix.remove(mx);});
  // NO cluster-merging of off-plane maps beyond the exact plane-sibling rule above: any broader
  // heuristic (same matrix, same matrix+name) fuses DISTINCT template interiors - all Castelia
  // buildings share one floor plan AND the location name "Castelia City", and merging them sent
  // players through the wrong building to the wrong street. Residual flag-gated twin doors on
  // open ground (Driftveil's second drawbridge entrance to 301) now round-trip through their own
  // twin's frame harmlessly since the seam fallback is world-plane only.
 }

 /** A tile a player can actually stand on. Script-only warps use (0,-1) or (0,0). */
 static boolean walkable(int x,int y){return x>=0&&y>=0&&!(x==0&&y==0);}

 /**
  * The Elite Four rooms. Their exit doors exist as warp records but the game seals them by
  * script - the only ways out are the champion or a whiteout - so their outgoing warps are
  * dropped on purpose. Warps INTO them stay.
  */
 static final Set<Integer> TRAP_MAPS=Set.of(140,141,142,143);

 /**
  * Where this warp puts the player: the centre of the destination map's paired warp box. Rail
  * destinations are always accepted (rail coordinates may legitimately be 0 or negative). When
  * the paired tile is unusable the first walkable warp on that map stands in, and if the map has
  * none the warp is dropped rather than teleporting someone to (0, -1).
  */
 static int[] destTile(Warp w,int maps){
  if(w.dest()<0||w.dest()>=maps)return new int[]{0,0,0,-1};
  List<Warp> dws=warpsOf(w.dest());
  if(w.destWarp()>=0&&w.destWarp()<dws.size()){
   Warp d=dws.get(w.destWarp());
   int cx=d.x()+d.w()/2,cy=d.y()+d.h()/2;
   if(d.rail()||walkable(cx,cy))return new int[]{cx,cy,1,d.line()};}
  for(Warp d:dws){
   int cx=d.x()+d.w()/2,cy=d.y()+d.h()/2;
   if(d.rail()||walkable(cx,cy))return new int[]{cx,cy,2,d.line()};}
  return new int[]{0,0,0,-1};
 }

 static String name(byte[] hdr,List<String> loc,int rec,int i){
  int li=hdr[i*rec+26]&0xFF;return li<loc.size()?loc.get(li).trim():"?";}

 static List<Warp> warpsOf(int idx){
  List<Warp> out=new ArrayList<>();
  if(idx<0||idx>=evId.length)return out;
  int file=evId[idx];
  if(file<0||file>=ev.length)return out;
  byte[] f=Arrays.copyOfRange(rom,ev[file][0],ev[file][1]);
  if(f.length<8)return out;
  int npc=f[4]&0xFF,second=f[5]&0xFF,warps=f[6]&0xFF;
  int p=8+npc*20+second*36;
  for(int i=0;i<warps;i++){
   int o=p+i*20;
   if(o+20>f.length)break;
   int raw=f[o+4]&0xFF;
   // Raw 1-4. Down and up match our Direction ordinals, but the ROM stores left/right in the
   // opposite order (verified against geometry: Nimbasa west gate carries raw 4, east gate raw 3).
   int dir=switch(raw){case 1->0;case 2->1;case 3->3;case 4->2;default->-1;};
   boolean rail=s16(f,o+6)==1;
   int x,y,line;
   if(rail){line=s16(f,o+8);x=s16(f,o+10);y=s16(f,o+12);}
   else{line=-1;x=s16(f,o+8)>>4;y=s16(f,o+12)>>4;}
   int w=s16(f,o+14),h=s16(f,o+16);
   if(w<1||w>64)w=1;
   if(h<1||h>64)h=1;
   out.add(new Warp(s16(f,o),s16(f,o+2),x,y,dir,rail,line,w,h,f[o+5]&0xFF));
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
 static List<String> decode(byte[] f){List<String> o=new ArrayList<>();int e2=u16(f,2);int so=u32(f,0x0C);
  for(int e=0;e<e2;e++){int off=so+u32(f,so+4+e*8);int len=u16(f,so+4+e*8+4);
   int key=(0x7C89+e*0x2983)&0xFFFF;StringBuilder sb=new StringBuilder();
   for(int j=0;j<len;j++){int c=u16(f,off+j*2)^key;key=((key<<3)|(key>>>13))&0xFFFF;
    if(c==0xFFFF)break;if(c>=0x20&&c<0xE000)sb.append((char)c);}o.add(sb.toString());}return o;}
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p=sub;
  while(true){int t=rom[p]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p+1+l)&0x0FFF;p+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
