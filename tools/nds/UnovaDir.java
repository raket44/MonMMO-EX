import java.nio.file.*;import java.util.*;

/**
 * Writes every Unova map as `region;bank;map;name` plus the spawn tile the client's own Teleport
 * menu uses. Header record layout (48 bytes) taken from the client's Gen 5 parser f.Hg:
 * byte 0 aZ, 2-3 iU1, 4-5 aX, 6-7 Pa1, 12-13 rA1, 14-15 xv1, 16-17 mm0, 18-19 x6, 22-23 na,
 * 24-25 Te, 26 name index, 28 PJ, 29 UG0, 30-31 nV, 32-33 RO0, 36-39 u10 (x), 40-43 XS0,
 * 44-47 vJ (y).
 */
public class UnovaDir{
 static byte[] rom;
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 static Map<String,Integer> paths=new LinkedHashMap<>();
 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));
  walk(u32(rom,0x40),0,"");
  byte[] hdr=narcFile("/a/0/1/2",0);
  List<String> loc=decode(narcFile("/a/0/0/2",89));
  int rec=48,n=hdr.length/rec;
  StringBuilder dir=new StringBuilder(),tp=new StringBuilder();
  Set<String> seen=new HashSet<>();
  for(int i=0;i<n;i++){
   int bank=i&0xFF,map=i>>8;
   int li=hdr[i*rec+26]&0xFF;
   String name=li<loc.size()?loc.get(li).trim():"?";
   if(name.isEmpty())name="(unnamed)";
   int x=u32(hdr,i*rec+36),y=u32(hdr,i*rec+44);
   dir.append("2;").append(bank).append(';').append(map).append(';').append(name)
      .append(';').append(x).append(';').append(y).append('\n');
   String key=name.toLowerCase(Locale.ROOT);
   if(!name.equals("(unnamed)")&&!name.equals("?")&&seen.add(key))
    tp.append(name.replaceAll("[^A-Za-z0-9 ]","")).append(";2;").append(bank).append(';')
      .append(map).append(';').append(x).append(';').append(y).append('\n');
  }
  Files.writeString(Paths.get(a[1]),dir.toString());
  if(a.length>2)Files.writeString(Paths.get(a[2]),tp.toString());
  System.out.println("maps="+n+" named="+seen.size());
 }
 static byte[] narcFile(String path,int index){
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get(path)*8);
  int p=s+0x10;int[] st=null,en=null;int img=0;int count=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){count=u16(rom,p+8);st=new int[count];en=new int[count];
    for(int i=0;i<count;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;}
   if(cs<=0)break;p+=cs;}
  return Arrays.copyOfRange(rom,img+st[index],img+en[index]);
 }
 static List<String> decode(byte[] f){
  List<String> out=new ArrayList<>();int entries=u16(f,2);int secOff=u32(f,0x0C);
  for(int e=0;e<entries;e++){
   int off=secOff+u32(f,secOff+4+e*8);int len=u16(f,secOff+4+e*8+4);
   int key=(0x7C89+e*0x2983)&0xFFFF;StringBuilder sb=new StringBuilder();
   for(int j=0;j<len;j++){int c=u16(f,off+j*2)^key;key=((key<<3)|(key>>>13))&0xFFFF;
    if(c==0xFFFF)break;if(c>=0x20&&c<0xE000)sb.append((char)c);}
   out.add(sb.toString());}
  return out;
 }
 static void walk(int fnt,int dir,String pre){
  int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p=sub;
  while(true){int t=rom[p]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p+1+l)&0x0FFF;p+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
