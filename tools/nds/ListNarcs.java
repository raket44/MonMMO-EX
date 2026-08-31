import java.nio.file.*;import java.util.*;
public class ListNarcs{
 static byte[] rom;
 static int u16(int o){return (rom[o]&0xFF)|((rom[o+1]&0xFF)<<8);}
 static int u32(int o){return (rom[o]&0xFF)|((rom[o+1]&0xFF)<<8)|((rom[o+2]&0xFF)<<16)|((rom[o+3]&0xFF)<<24);}
 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));
  int fnt=u32(0x40),fat=u32(0x48);
  Map<String,Integer> paths=new LinkedHashMap<>();walk(fnt,0,"",paths);
  int target=a.length>1?Integer.parseInt(a[1]):-1;
  for(Map.Entry<String,Integer> e:paths.entrySet()){
   int id=e.getValue();int s=u32(fat+id*8),en=u32(fat+id*8+4);
   if(en-s<16)continue;
   String magic=new String(rom,s,4,java.nio.charset.StandardCharsets.US_ASCII);
   if(!magic.equals("NARC"))continue;
   int n=u16(s+0x18);
   if(target<0||n==target)System.out.printf("%-14s entries=%-6d size=%d%n",e.getKey(),n,en-s);
  }
 }
 static void walk(int fnt,int dir,String pre,Map<String,Integer> out){
  int e=fnt+dir*8;int sub=fnt+u32(e);int fid=u16(e+4);int p=sub;
  while(true){int t=rom[p]&0xFF;if(t==0)break;
   if(t<0x80){out.put(pre+"/"+new String(rom,p+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p+=1+t;}
   else{int l=t&0x7F;String n=new String(rom,p+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(p+1+l)&0x0FFF;p+=1+l+2;walk(fnt,sd,pre+"/"+n,out);}}}
}
