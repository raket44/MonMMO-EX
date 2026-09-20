import java.nio.file.*;import java.util.*;
/** Gen 5 item pockets from the ROM's item table /a/0/2/4 (36-byte records): (u16 @ 8 >> 7) & 0xF =
 *  0 Items, 1 Medicine, 2 TMs & HMs, 3 Berries, 4 Key Items. Checked on Poke Ball/Repel (0), Potion/
 *  Rare Candy (1), TM01/HM01 (2), Cheri/Oran (3), Bicycle/Town Map/Xtransceiver (4).
 *  Usage: ItemPockets5 <rom> <out> - writes one digit per item index on a single line. */
public class ItemPockets5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[]b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[]b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[]a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get("/a/0/2/4")*8);
  int p=s+0x10;int[]st=null,en=null;int img=0;int c=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){c=u16(rom,p+8);st=new int[c];en=new int[c];for(int i=0;i<c;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  StringBuilder sb=new StringBuilder();
  for(int i=0;i<c;i++){int o=img+st[i];int len=en[i]-st[i];sb.append(len>=10?Integer.toHexString((u16(rom,o+8)>>7)&0xF):"0");}
  Files.writeString(Paths.get(a[1]),"# White item pockets by Gen 5 item index (tools/nds/ItemPockets5): 0 Items, 1 Medicine, 2 TMs & HMs, 3 Berries, 4 Key Items\n"+sb+"\n");
  System.out.println("items="+c);
 }
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p2=sub;
  while(true){int t=rom[p2]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p2+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p2+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p2+1,l,java.nio.charset.StandardCharsets.US_ASCII);int sd=u16(rom,p2+1+l)&0x0FFF;p2+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
