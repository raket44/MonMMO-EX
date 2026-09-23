import java.nio.file.*;import java.util.*;

/**
 * Unova background events (signs, bookshelves, hidden items - the things the action button hits
 * on a TILE rather than an npc) from White's per-map event files (/a/1/2/5) into the server's npc
 * table, as `bg;2;bank;map;idx;script;type;dir;x;y;z` rows. Only the bg rows of the target file
 * are replaced.
 *
 * Event file: u32 size, four u8 counts (bg 20 B, npc 36 B, warp 20 B, trigger 22 B), the arrays
 * in that order (see Triggers5). Bg record (read 2026-09-23 off the Nacrene gym's 26 bookshelves,
 * the museum's signs and Route 3's hidden items):
 *   +0 u16 script (the map header's script file entry, 1-based; 2000+ shared chunks, 8000+ hidden
 *      items)   +2 u16 type (0 bookshelf/plain, 1 sign, 2 hidden item)   +4 u16 facing (0, 4, 6)
 *   +6 u16 unused   +8 s32 x   +12 s32 y   +16 s32 z (0 / 8 / 16)
 *
 * Usage: java tools/nds/Bg5.java <white.nds> server.game/nds-npcs-2.txt
 */
public class Bg5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}

 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  String code=new String(rom,0x0C,4,java.nio.charset.StandardCharsets.US_ASCII);
  if(!code.equals("IRAO"))throw new IllegalStateException("expected Pokemon White (IRAO), got "+code);
  int[][] ev=narcIndex("/a/1/2/5");
  byte[] hdr=narcFile("/a/0/1/2",0);int rec=48,maps=hdr.length/rec;
  List<String> rows=new ArrayList<>();
  int written=0;
  for(int header=0;header<maps;header++){
   int file=u16(hdr,header*rec+22);
   if(file>=ev.length)continue;
   byte[] f=Arrays.copyOfRange(rom,ev[file][0],ev[file][1]);
   if(f.length<8)continue;
   int n0=f[4]&0xFF;
   for(int i=0;i<n0&&8+i*20+20<=f.length;i++){
    int o=8+i*20;
    int script=u16(f,o),type=u16(f,o+2),dir=u16(f,o+4);
    int x=u32(f,o+8),y=u32(f,o+12),z=u32(f,o+16);
    rows.add("bg;2;"+(header&0xFF)+";"+(header>>8)+";"+i+";"+script+";"+type+";"+dir+";"+x+";"+y+";"+z);
    written++;
   }
  }
  Path out=Paths.get(a[1]);
  List<String> kept=new ArrayList<>();
  for(String l:Files.readAllLines(out))if(!l.startsWith("bg;"))kept.add(l);
  kept.addAll(rows);
  Files.write(out,kept);
  System.out.println("bg rows written="+written);
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
