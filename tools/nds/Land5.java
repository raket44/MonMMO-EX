import java.nio.file.*;import java.util.*;
/**
 * Unova (Black/White) tile permissions. World matrix /a/0/0/9 file 0: u16 w,h at +4/+6, then a
 * map-file plane (u32 per cell) and a header plane. Map file (/a/0/0/8): magic "WB"/"GC",
 * u16 sections, u32 section offsets; section 1 = permissions: u16 w, u16 h, then w*h records of
 * 8 bytes (u16 f0, u16 height, u16 type, u16 flags; flags & 0x80 blocks).
 * Modes: probe <rom> <cx> <cy>...   ASCII map + histogram for world cells
 *        dump <rom> <out>            every world cell's tiles as region;bank;map;x;y;type;coll (world coords)
 */
public class Land5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[] a)throws Exception{rom=Files.readAllBytes(Paths.get(a[1]));walk(u32(rom,0x40),0,"");
  byte[] mx=narcFile("/a/0/0/9",0);int w=u16(mx,4),h=u16(mx,6);
  if(a[0].equals("probe")){
   for(int k=2;k+1<a.length;k+=2){int cx=Integer.parseInt(a[k]),cy=Integer.parseInt(a[k+1]);int mapFile=u32(mx,8+(cy*w+cx)*4);int hdr=u32(mx,8+w*h*4+(cy*w+cx)*4);
    byte[] f=narcFile("/a/0/0/8",mapFile);int off1=u32(f,8);int p=off1+4;int tw=u16(f,off1),th=u16(f,off1+2);System.out.println("cell "+cx+","+cy+" mapFile "+mapFile+" header "+hdr+" plane "+tw+"x"+th);
    Map<String,Integer> hist=new TreeMap<>();StringBuilder map=new StringBuilder();
    for(int y=0;y<th;y++){for(int x=0;x<tw;x++){int o=p+(y*tw+x)*8;int f0=u16(f,o),f1=u16(f,o+2),f2=u16(f,o+4),f3=u16(f,o+6);hist.merge(String.format("%04x %04x %04x %04x",f0,f1,f2,f3),1,Integer::sum);
      char c=(f3&1)==0?'#':(f0==0?'.':(f0==0x10?'g':(f0==0x08?'w':(f0==0x18?'t':'?'))));map.append(c);}map.append('\n');}
    System.out.println(map);hist.entrySet().stream().sorted((x,y)->y.getValue()-x.getValue()).limit(16).forEach(en->System.out.println("  "+en.getValue()+"  "+en.getKey()));}
   return;}
  StringBuilder out=new StringBuilder();int cells=0;
  for(int cy=0;cy<h;cy++)for(int cx=0;cx<w;cx++){long mapFile=u32(mx,8+(cy*w+cx)*4)&0xFFFFFFFFL;long hdr=u32(mx,8+w*h*4+(cy*w+cx)*4)&0xFFFFFFFFL;if(mapFile==0xFFFFFFFFL||hdr==0xFFFFFFFFL)continue;
   int[][] idx=narcIndex("/a/0/0/8");if(mapFile>=idx.length)continue;byte[] f=Arrays.copyOfRange(rom,idx[(int)mapFile][0],idx[(int)mapFile][1]);if(f.length<16)continue;int off1=u32(f,8);if(off1+4>f.length)continue;int tw=u16(f,off1),th=u16(f,off1+2);int p=off1+4;if(tw!=32||th!=32)continue;cells++;
   int bank=(int)(hdr&0xFF),map=(int)(hdr>>8);
   for(int y=0;y<th;y++)for(int x=0;x<tw;x++){int o=p+(y*tw+x)*8;if(o+8>f.length)break;int t0=u16(f,o),flags=u16(f,o+6);int type=t0==0x10?2:(t0==0x18?3:(t0==0x08?21:0));int coll=(flags&1)==0?0x80:0;
    out.append("2;").append(bank).append(';').append(map).append(';').append(cx*32+x).append(';').append(cy*32+y).append(';').append(type).append(";").append(coll).append('\n');}}
  Files.write(Paths.get(a[2]),out.toString().getBytes("UTF-8"));System.out.println("cells "+cells);
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
