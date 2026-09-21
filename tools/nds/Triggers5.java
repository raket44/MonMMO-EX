import java.nio.file.*;import java.util.*;

/**
 * Unova step triggers (coord events) from White's per-map event files (/a/1/2/5) into the server's
 * npc table, as `coord;2;bank;map;idx;script;x;y;w;h;height;value;var` rows - the same columns the
 * Platinum/HeartGold tables carry. Only the coord rows of the target file are replaced.
 *
 * Event file: u32 size, four u8 counts (bg 20 B, npc 36 B, warp 20 B, trigger 22 B), the arrays in that
 * order, then a variable-length table (the leftover after triggers*22 matches the trigger-free files).
 * Trigger record (verified 2026-09-14 against all 428 files of IRAO):
 *   +0 u16 script (the map header's script file entry, 1-based)   +2 u16 value   +4 u16 var
 *   +6 u16 type (0 plain; 1-4 and 6 only in two special maps, meaning unknown)
 *   +8 u16 rail flag (1 = rail coordinates, like the warp records)
 *   +10 s16 x  +12 s16 y (tiles; world tiles on the world matrix)  +14 s16 width  +16 s16 height
 *   +18 s16 height/z   +20 u16 unused
 * Written: plain triggers only - type 0, not rail, a story var (0x4000-0x43FF). Skipped and counted:
 * rail triggers, var-less triggers (var 0 / below 0x4000, shared scripts like 10332 / 2000), typed ones.
 *
 * Usage: java tools/nds/Triggers5.java <white.nds> server.game/nds-npcs-2.txt
 */
public class Triggers5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int s16(byte[] b,int o){int v=u16(b,o);return v>=0x8000?v-0x10000:v;}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}

 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  String code=new String(rom,0x0C,4,java.nio.charset.StandardCharsets.US_ASCII);
  if(!code.equals("IRAO"))throw new IllegalStateException("expected Pokemon White (IRAO), got "+code);
  int[][] ev=narcIndex("/a/1/2/5");
  byte[] hdr=narcFile("/a/0/1/2",0);int rec=48,maps=hdr.length/rec;
  List<String> rows=new ArrayList<>();
  int written=0,rail=0,varless=0,typed=0;
  for(int header=0;header<maps;header++){
   int file=u16(hdr,header*rec+22);
   if(file>=ev.length)continue;
   byte[] f=Arrays.copyOfRange(rom,ev[file][0],ev[file][1]);
   if(f.length<8)continue;
   int n0=f[4]&0xFF,n1=f[5]&0xFF,n2=f[6]&0xFF,n3=f[7]&0xFF;
   int p=8+n0*20+n1*36+n2*20;
   for(int i=0;i<n3&&p+i*22+22<=f.length;i++){
    int o=p+i*22;
    int script=u16(f,o),value=u16(f,o+2),var=u16(f,o+4),type=u16(f,o+6),railFlag=u16(f,o+8);
    int x=s16(f,o+10),y=s16(f,o+12),w=s16(f,o+14),h=s16(f,o+16),z=s16(f,o+18);
    if(railFlag!=0){rail++;continue;}
    if(var<0x4000||var>=0x4400){varless++;continue;}
    if(type!=0){typed++;continue;}
    rows.add("coord;2;"+(header&0xFF)+";"+(header>>8)+";"+i+";"+script+";"+x+";"+y+";"+Math.max(1,w)+";"+Math.max(1,h)+";"+z+";"+value+";"+var);
    written++;
   }
  }
  Path out=Paths.get(a[1]);
  List<String> kept=new ArrayList<>();
  for(String l:Files.readAllLines(out))if(!l.startsWith("coord;"))kept.add(l);
  kept.addAll(rows);
  Files.write(out,kept);
  System.out.println("coord rows written="+written+" (skipped: rail="+rail+" varless="+varless+" typed="+typed+")");
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
