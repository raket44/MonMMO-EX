import java.nio.file.*;import java.util.*;
public class LandCmp{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[]b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[]b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 public static void main(String[]a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  for(String path:new String[]{"/a/0/0/8","/a/0/1/0"}){
   Integer fid=paths.get(path);
   if(fid==null){System.out.println(path+": missing");continue;}
   int fat=u32(rom,0x48);int s=u32(rom,fat+fid*8);
   int p=s+0x10;int[]st=null,en=null;int img=0;int c=0;
   while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
    if(m.equals("BTAF")){c=u16(rom,p+8);st=new int[c];en=new int[c];
     for(int i=0;i<c;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
    else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
   System.out.println(path+": "+c+" files");
   for(int k=1;k<a.length;k++){
    int i=Integer.parseInt(a[k]);
    if(i>=c){System.out.println("  file "+i+": out of range");continue;}
    byte[] f=Arrays.copyOfRange(rom,img+st[i],img+en[i]);
    StringBuilder sb=new StringBuilder();
    for(int b=0;b<Math.min(f.length,16);b++)sb.append(String.format("%02x ",f[b]));
    System.out.println("  file "+i+": len="+f.length+" head: "+sb+" magic:"+new String(f,0,Math.min(4,f.length)).replaceAll("[^ -~]","."));
   }
  }
 }
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p2=sub;
  while(true){int t=rom[p2]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p2+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p2+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p2+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p2+1+l)&0x0FFF;p2+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
