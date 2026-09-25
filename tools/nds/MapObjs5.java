import java.nio.file.*;import java.util.*;
/**
 * White map-file object placements: the last section of a map file (/a/0/0/8) is
 * u32 count then count records of four fx32 words (x, height, z, kind<<24). Chargestone Cave
 * keeps its electric stones there (kinds 0x6E big, 0x6F attached, 0x70 loose - the ones the
 * player pushes), which no event file lists and no overlay names: found 2026-09-25 after the
 * owner insisted the cartridge carries them. Tile = cell*32 + 16 + round(x/16) east,
 * cell*32 + 15 - round(z/16) south (23 of 24 loose stones land on open floor; the big ones on
 * blocked tiles). Rows: `mobj;2;bank;map;kind;x;y;height` for every header whose cells carry
 * records of those kinds.
 * Usage: java tools/nds/MapObjs5.java <white.nds> server.game/nds-objects-2.txt
 */
public class MapObjs5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 static int rnd(double v){return (int)(v+(v<0?-0.5:0.5));}
 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  byte[] hdr=narcFile("/a/0/1/2",0);int[][] mx=narcIndex("/a/0/0/9"),mp=narcIndex("/a/0/0/8");
  StringBuilder out=new StringBuilder("# mobj;region;bank;map;kind;x;y;height - White map-file placements (MapObjs5)\n");int rows=0;Set<Integer> seenMatrix=new HashSet<>();
  for(int h=0;h<hdr.length/48;h++){int m=u16(hdr,h*48+4);if(m>=mx.length||!seenMatrix.add(m))continue;
   byte[] mf=Arrays.copyOfRange(rom,mx[m][0],mx[m][1]);int w=u16(mf,4),hh=u16(mf,6);if(w<1||hh<1||w*hh>64)continue;StringBuilder mrows=new StringBuilder();int mcount=0;
   for(int cy=0;cy<hh;cy++)for(int cx=0;cx<w;cx++){int id=u32(mf,8+(cy*w+cx)*4);if(id<0||id>=mp.length)continue;byte[] f=Arrays.copyOfRange(rom,mp[id][0],mp[id][1]);if(f.length<8)continue;int n=u16(f,2);if(n<3)continue;int off=u32(f,4+(n-1)*4);if(off<0||off+4>f.length)continue;int cnt=u32(f,off);if(cnt<1||cnt>200||off+4+cnt*16>f.length)continue;
    for(int i=0;i<cnt;i++){int o=off+4+i*16;int kind=(u32(f,o+12)>>>24);if(kind<0x6E||kind>0x70)continue;double x=u32(f,o)/4096.0,y=u32(f,o+4)/4096.0,z=u32(f,o+8)/4096.0;
     int tx=cx*32+16+rnd(x/16),ty=cy*32+15-rnd(z/16);mrows.append("mobj;2;").append(h&0xFF).append(';').append(h>>8).append(';').append(kind).append(';').append(tx).append(';').append(ty).append(';').append(rnd(y)).append('\n');mcount++;}}
   // A lone record with one of these kind bytes elsewhere is another table's data; the cave floors carry 26-57.
   if(mcount>=5){out.append(mrows);rows+=mcount;}}
  Files.write(Paths.get(a[1]),out.toString().getBytes("UTF-8"));System.out.println("rows "+rows);
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
