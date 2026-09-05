import java.nio.file.*;import java.util.*;
/** Lists narc archives under a ROM path prefix: file count and the first sizes. Usage: Ls5 <rom> <prefix> [<path> <file> hexdump-bytes] */
public class Ls5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[] a)throws Exception{rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  if(a[1].equals("find")){int[][] x=narcIndex(a[2]);int want=Integer.parseInt(a[3]);for(int i=0;i<x.length;i++){byte[] f=Arrays.copyOfRange(rom,x[i][0],x[i][1]);if(f.length<16)continue;int lines=0;int sec=u16(f,0);for(int s=0;s<sec;s++){int so=u32(f,0x10+s*4);lines+=u16(f,so);}if(lines!=want&&u16(f,2)!=want)continue;System.out.println("file "+i+" sections "+sec+" lines "+lines+" u16 "+u16(f,2));List<String> d=Raw5.decode(f);for(int k=0;k<Math.min(8,d.size());k++)System.out.println("  "+k+": "+d.get(k));}return;}
  if(a.length>=4){int[][] x=narcIndex(a[2]);int i=Integer.parseInt(a[3]);int n=a.length>4?Integer.parseInt(a[4]):256;int s=x[i][0];StringBuilder sb=new StringBuilder();for(int k=0;k<Math.min(n,x[i][1]-s);k++){sb.append(String.format("%02x",rom[s+k]&0xFF));if(k%2==1)sb.append(' ');if(k%32==31)sb.append('\n');}System.out.println("len "+(x[i][1]-s)+"\n"+sb);return;}
  for(String p:paths.keySet()){if(!p.startsWith(a[1]))continue;try{int[][] x=narcIndex(p);StringBuilder sb=new StringBuilder(p+" files "+x.length+":");for(int i=0;i<Math.min(6,x.length);i++)sb.append(' ').append(x[i][1]-x[i][0]);System.out.println(sb);}catch(Exception e){System.out.println(p+" not narc");}}}
 static int[][] narcIndex(String path){
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get(path)*8);int p=s+0x10;int[] st=null,en=null;int img=0;int count=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){count=u16(rom,p+8);st=new int[count];en=new int[count];for(int i=0;i<count;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  int[][] out=new int[count][2];for(int i=0;i<count;i++){out[i][0]=img+st[i];out[i][1]=img+en[i];}return out;}
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p=sub;
  while(true){int t=rom[p]&0xFF;if(t==0)break;if(t<0x80){paths.put(pre+"/"+new String(rom,p+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p+1,l,java.nio.charset.StandardCharsets.US_ASCII);int sd=u16(rom,p+1+l)&0x0FFF;p+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
