import java.nio.file.*;import java.util.*;
public class Compare{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  byte[] h=narcFile("/a/0/1/2",0);List<String> loc=decode(narcFile("/a/0/0/2",89));
  int rec=48;
  int[] idx={6,137,191,356,28,0};
  for(int i:idx){
   int li=h[i*rec+26]&0xFF;
   System.out.printf("idx %-4d %-22s b0=%-3d b1=%-3d b27=%-3d b28=%-3d b29=%-3d b34=%-3d b35=%-3d%n",
     i, li<loc.size()?loc.get(li).trim():"?",
     h[i*rec]&0xFF,h[i*rec+1]&0xFF,h[i*rec+27]&0xFF,h[i*rec+28]&0xFF,h[i*rec+29]&0xFF,
     h[i*rec+34]&0xFF,h[i*rec+35]&0xFF);
  }
  // distinct counts per byte, to spot small enums
  int n=h.length/rec;
  for(int off=0;off<rec;off++){Set<Integer> d=new TreeSet<>();
   for(int i=0;i<n;i++)d.add(h[i*rec+off]&0xFF);
   if(d.size()<=24&&d.size()>1)System.out.println("byte "+off+" values="+d);
  }
 }
 static byte[] narcFile(String p,int i){int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get(p)*8);
  int q=s+0x10;int[] st=null,en=null;int img=0;int c=0;
  while(true){String m=new String(rom,q,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,q+4);
   if(m.equals("BTAF")){c=u16(rom,q+8);st=new int[c];en=new int[c];
    for(int k=0;k<c;k++){st[k]=u32(rom,q+12+k*8);en[k]=u32(rom,q+12+k*8+4);}}
   else if(m.equals("GMIF")){img=q+8;break;} if(cs<=0)break;q+=cs;}
  return Arrays.copyOfRange(rom,img+st[i],img+en[i]);}
 static List<String> decode(byte[] f){List<String> o=new ArrayList<>();int e2=u16(f,2);int so=u32(f,0x0C);
  for(int e=0;e<e2;e++){int off=so+u32(f,so+4+e*8);int len=u16(f,so+4+e*8+4);
   int key=(0x7C89+e*0x2983)&0xFFFF;StringBuilder sb=new StringBuilder();
   for(int j=0;j<len;j++){int c=u16(f,off+j*2)^key;key=((key<<3)|(key>>>13))&0xFFFF;
    if(c==0xFFFF)break;if(c>=0x20&&c<0xE000)sb.append((char)c);}o.add(sb.toString());}return o;}
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p=sub;
  while(true){int t=rom[p]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p+1+l)&0x0FFF;p+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
