import java.nio.file.*;import java.util.*;

/** Checks every warp for a correct partner, sane coordinates and a usable direction. */
public class Audit{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();static int[][] ev;
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int s16(byte[] b,int o){int v=u16(b,o);return v>=0x8000?v-0x10000:v;}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 record Warp(int dest,int destWarp,int x,int y,int dir){}

 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[0]));walk(u32(rom,0x40),0,"");
  ev=narcIndex("/a/1/2/5");
  byte[] hdr=narcFile("/a/0/1/2",0);int rec=48,maps=hdr.length/rec;
  int total=0,badDest=0,badIndex=0,asym=0,noDir=0,zero=0,selfLoop=0;
  Map<String,Integer> examples=new LinkedHashMap<>();
  for(int m=0;m<maps;m++){
   List<Warp> ws=warpsOf(m);
   for(int i=0;i<ws.size();i++){
    Warp w=ws.get(i);total++;
    if(w.dest()<0||w.dest()>=maps){badDest++;note(examples,"dest out of range",m,i);continue;}
    List<Warp> dws=warpsOf(w.dest());
    if(w.destWarp()<0||w.destWarp()>=dws.size()){badIndex++;note(examples,"partner index out of range",m,i);continue;}
    Warp p=dws.get(w.destWarp());
    // the partner should point back at this map, ideally at this very warp
    if(p.dest()!=m){asym++;note(examples,"partner points elsewhere",m,i);}
    else if(p.destWarp()!=i){asym++;note(examples,"partner points at a different warp",m,i);}
    if(w.dir()<0){noDir++;note(examples,"no direction",m,i);}
    if(p.x()==0&&p.y()==0){zero++;note(examples,"partner tile is 0,0",m,i);}
    if(w.dest()==m)selfLoop++;
   }
  }
  System.out.println("total warps          "+total);
  System.out.println("destination invalid  "+badDest);
  System.out.println("partner index invalid"+" "+badIndex);
  System.out.println("asymmetric pairing   "+asym);
  System.out.println("no direction         "+noDir);
  System.out.println("partner tile 0,0     "+zero);
  System.out.println("warps within one map "+selfLoop);
  System.out.println("--- first example of each problem (map/warp) ---");
  examples.forEach((k,v)->System.out.println("  "+k+": map "+(v>>8)+" warp "+(v&0xFF)));
 }
 static void note(Map<String,Integer> ex,String k,int m,int i){ex.putIfAbsent(k,(m<<8)|i);}

 static List<Warp> warpsOf(int idx){
  List<Warp> out=new ArrayList<>();
  if(idx<0||idx>=ev.length)return out;
  byte[] f=Arrays.copyOfRange(rom,ev[idx][0],ev[idx][1]);
  if(f.length<8)return out;
  int npc=f[4]&0xFF,second=f[5]&0xFF,warps=f[6]&0xFF;int p=8+npc*20+second*36;
  for(int i=0;i<warps;i++){int o=p+i*20;if(o+20>f.length)break;
   int raw=f[o+4]&0xFF;
   int dir=switch(raw){case 1->0;case 2->1;case 3->2;case 4->3;default->-1;};
   out.add(new Warp(s16(f,o),s16(f,o+2),s16(f,o+8)>>4,s16(f,o+12)>>4,dir));}
  return out;
 }
 static int[][] narcIndex(String path){
  int fat=u32(rom,0x48);int s=u32(rom,fat+paths.get(path)*8);
  int p=s+0x10;int[] st=null,en=null;int img=0;int count=0;
  while(true){String m=new String(rom,p,4,java.nio.charset.StandardCharsets.US_ASCII);int cs=u32(rom,p+4);
   if(m.equals("BTAF")){count=u16(rom,p+8);st=new int[count];en=new int[count];
    for(int i=0;i<count;i++){st[i]=u32(rom,p+12+i*8);en[i]=u32(rom,p+12+i*8+4);}}
   else if(m.equals("GMIF")){img=p+8;break;} if(cs<=0)break;p+=cs;}
  int[][] out=new int[count][2];
  for(int i=0;i<count;i++){out[i][0]=img+st[i];out[i][1]=img+en[i];}
  return out;}
 static byte[] narcFile(String p,int i){int[][] x=narcIndex(p);return Arrays.copyOfRange(rom,x[i][0],x[i][1]);}
 static void walk(int fnt,int dir,String pre){int e=fnt+dir*8;int sub=fnt+u32(rom,e);int fid=u16(rom,e+4);int p=sub;
  while(true){int t=rom[p]&0xFF;if(t==0)break;
   if(t<0x80){paths.put(pre+"/"+new String(rom,p+1,t,java.nio.charset.StandardCharsets.US_ASCII),fid++);p+=1+t;}
   else{int l=t&0x7F;String nm=new String(rom,p+1,l,java.nio.charset.StandardCharsets.US_ASCII);
    int sd=u16(rom,p+1+l)&0x0FFF;p+=1+l+2;walk(fnt,sd,pre+"/"+nm);}}}
}
