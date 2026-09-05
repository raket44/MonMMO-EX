import java.nio.file.*;import java.util.*;
/** Probe for the Unova script archive: counts, header fields, and a hex dump of one script file. */
public class Scr5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));int fnt=u32(rom,0x40);walk(fnt,0,"");
  for(String p:new String[]{"/a/0/5/6","/a/0/5/7","/a/0/5/8","/a/0/0/2","/a/0/9/1","/a/0/9/2","/a/0/9/3","/a/0/1/2"}){
   Integer id=paths.get(p);System.out.println(p+" -> "+(id==null?"absent":narcIndex(p).length+" files"));}
  byte[] hdr=narcFile("/a/0/1/2",0);int map=Integer.parseInt(a[1]);int o=map*48;
  System.out.println("header "+map+": +0="+(hdr[o]&0xFF)+" +2="+u16(hdr,o+2)+" +4="+u16(hdr,o+4)+" +6="+u16(hdr,o+6)+" +8="+u16(hdr,o+8)+" +10="+u16(hdr,o+10)+" +12="+u16(hdr,o+12)+" +14="+u16(hdr,o+14)+" +16="+u16(hdr,o+16)+" +18="+u16(hdr,o+18)+" +20="+u16(hdr,o+20)+" +22="+u16(hdr,o+22)+" +24="+u16(hdr,o+24)+" +26="+(hdr[o+26]&0xFF));
  int scr=Integer.parseInt(a[2]);byte[] f=narcFile("/a/0/5/7",scr);System.out.println("script file "+scr+" len "+f.length);
  StringBuilder sb=new StringBuilder();for(int i=0;i<Math.min(f.length,Integer.parseInt(a[3]));i++){sb.append(String.format("%02x ",f[i]&0xFF));if(i%32==31)sb.append("\n");}System.out.println(sb);
  int tb=u16(hdr,o+10);List<String> t=decode(narcFile("/a/0/0/2",tb));System.out.println("text bank "+tb+" entries "+t.size());for(int i=0;i<Math.min(4,t.size());i++)System.out.println("  "+i+": "+t.get(i));
 }
 static int[][] narcIndex(String path){
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get(path)*8);int p=s+0x10;int[] st=null,en=null;int img=0;int count=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){count=u16(rom,p+8);st=new int[count];en=new int[count];for(int i=0;i<count;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  int[][] out=new int[count][2];for(int i=0;i<count;i++){out[i][0]=img+st[i];out[i][1]=img+en[i];}return out;}
 static byte[] narcFile(String p,int i){int[][] x=narcIndex(p);return Arrays.copyOfRange(rom,x[i][0],x[i][1]);}
 static List<String> decode(byte[] f){List<String> o=new ArrayList<>();int e2=u16(f,2);int so=u32(f,0x0C);
  for(int e=0;e<e2;e++){int off=so+u32(f,so+4+e*8);int len=u16(f,so+4+e*8+4);int key=(0x7C89+e*0x2983)&0xFFFF;StringBuilder sb=new StringBuilder();
   for(int j=0;j<len;j++){int c=u16(f,off+j*2)^key;key=((key<<3)|(key>>>13))&0xFFFF;if(c==0xFFFF)break;if(c>=0x20&&c<0xE000)sb.append((char)c);}o.add(sb.toString());}return o;}
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p=sub;
  while(true){int t=rom[p]&0xFF;if(t==0)break;if(t<0x80){paths.put(pre+"/"+new String(rom,p+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p+1,l,java.nio.charset.StandardCharsets.US_ASCII);int sd=u16(rom,p+1+l)&0x0FFF;p+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
