import java.nio.file.*;import java.util.*;
public class Mat0{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[]b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[]b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[]a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get("/a/0/0/9")*8);
  int p=s+0x10;int[]st=null,en=null;int img=0;int c=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){c=u16(rom,p+8);st=new int[c];en=new int[c];
    for(int i=0;i<c;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  byte[] f=Arrays.copyOfRange(rom,img+st[0],img+en[0]);
  int flag=u16(f,0),z=u16(f,2),w=u16(f,4),h=u16(f,6);
  System.out.println("flag="+flag+" ?="+z+" w="+w+" h="+h+" len="+f.length+" per-cell="+((f.length-8.0)/(w*h)));
  // try: two planes of u16 pairs? per-cell 8 bytes: print plane structure for a known cell.
  // Nimbasa world (406..469): cell (406/32=12, 469/32=14) -> idx 14*29+12=418
  int[] probe={14*29+12, (414/32)*0+ (414/32), (472/32)*29+(164/32), (599/32)*29+(592/32), (310/32)*29+(103/32), (341/32)*29+(107/32), (369/32)*29+(135/32)};
  String[] names={"Nimbasa(406,469)","dummy","ColdSt(164,472)","Pinwheel(592,599)","Mistr(103,310)","Charge(107,341)","PinIn(135,369)"};
  for(int k=0;k<probe.length;k++){
   int idx=probe[k];int o=8+idx*8;
   if(o+8>f.length)continue;
   System.out.println(names[k]+" cell#"+idx+": "+u16(f,o)+" "+u16(f,o+2)+" "+u16(f,o+4)+" "+u16(f,o+6));
  }
  // full header-plane occupancy: assume first u16 of each 8-byte cell = header id
  Set<Integer> hdrs=new TreeSet<>();
  for(int i=0;i<w*h;i++){int v=u16(f,8+i*8);if(v!=0xFFFF)hdrs.add(v);}
  System.out.println("distinct first-u16 values: "+hdrs.size());
  System.out.println(hdrs);
 }
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p2=sub;
  while(true){int t=rom[p2]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p2+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p2+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p2+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p2+1+l)&0x0FFF;p2+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
