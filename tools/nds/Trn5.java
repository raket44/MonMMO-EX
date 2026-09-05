import java.nio.file.*;import java.util.*;
/** Probe for the Unova script archive: counts, header fields, and a hex dump of one script file. */
public class Trn5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[] a)throws Exception{rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  int[][] td=narcIndex("/a/0/9/2"),tp=narcIndex("/a/0/9/3");StringBuilder sb=new StringBuilder("# trn;region;id;class;type;battleType;count | mon;region;id;slot;species;level;iv;item;m1;m2;m3;m4\n");
  for(int i=1;i<td.length;i++){byte[] d=Arrays.copyOfRange(rom,td[i][0],td[i][1]);if(d.length<20)continue;int type=d[0]&0xFF,cls=d[1]&0xFF,bt=d[2]&0xFF,n=d[3]&0xFF;
   sb.append("trn;2;").append(i).append(';').append(cls).append(';').append(type).append(';').append(bt).append(';').append(n).append((char)10);
   byte[] p=Arrays.copyOfRange(rom,tp[i][0],tp[i][1]);int sz=8+((type&2)!=0?2:0)+((type&1)!=0?8:0);
   for(int k=0;k<n&&k*sz+8<=p.length;k++){int o=k*sz;int iv=p[o]&0xFF,lvl=u16(p,o+2),sp=u16(p,o+4);int q=o+8;int item=0;if((type&2)!=0){item=u16(p,q);q+=2;}
    int[] mv=new int[4];if((type&1)!=0)for(int m=0;m<4;m++)mv[m]=u16(p,q+m*2);
    sb.append("mon;2;").append(i).append(';').append(k).append(';').append(sp).append(';').append(lvl).append(';').append(iv).append(';').append(item).append(';').append(mv[0]).append(';').append(mv[1]).append(';').append(mv[2]).append(';').append(mv[3]).append((char)10);}}
  Files.write(Paths.get(a[1]),sb.toString().getBytes("UTF-8"));System.out.println("trainers "+(td.length-1));}
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
