import java.nio.file.*;import java.util.*;
/**
 * Gen 5 (Black/White) field script disassembler / validator against the ROM's /a/0/5/7.
 * Script file: u32 offsets (relative to the position after each entry) until 0xFD13, then code:
 * u16 opcode + args. Table file: opcode;name;sizes (B=1,H=2,L=4).
 * Modes: validate <rom> <table>   - decode every entry of every file, report unknown opcodes
 *        dump <rom> <table> <out> - write the disassembly (file;entry;offset;name args) for the corpus
 *        peek <rom> <table> <file> <offset> - raw bytes of a script file at an offset (unknown-opcode work)
 */
public class Dis5{
 static byte[] rom;static Map<String,Integer> paths=new LinkedHashMap<>();
 static int u16(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8);}
 static int u32(byte[] b,int o){return (b[o]&0xFF)|((b[o+1]&0xFF)<<8)|((b[o+2]&0xFF)<<16)|((b[o+3]&0xFF)<<24);}
 static Map<Integer,String[]> cmds=new HashMap<>();
 static final Set<Integer> TERMINAL=new HashSet<>(Arrays.asList(0x02,0x05,0x1D,0x2F0));
 public static void main(String[] a)throws Exception{
  rom=Files.readAllBytes(Paths.get(a[1]));int fnt=u32(rom,0x40);walk(fnt,0,"");
  for(String l:Files.readAllLines(Paths.get(a[2]))){String[] p=l.split(";",-1);if(p.length<2)continue;cmds.put(Integer.parseInt(p[0]),new String[]{p[1],p.length>2?p[2]:""});}
  int[][] idx=narcIndex("/a/0/5/7");
  Map<Integer,Integer> unknown=new TreeMap<>();Map<Integer,String> ctx=new HashMap<>();int okEntries=0,badEntries=0,files=0;
  StringBuilder out=new StringBuilder();
  for(int fi=0;fi<idx.length;fi++){
   byte[] f=Arrays.copyOfRange(rom,idx[fi][0],idx[fi][1]);if(f.length<4)continue;files++;
   List<Integer> entries=new ArrayList<>();int p=0;
   while(p+4<=f.length){if(u16(f,p)==0xFD13)break;int off=u32(f,p);int tgt=p+4+off;if(tgt<0||tgt>f.length){break;}entries.add(tgt);p+=4;if(entries.size()>2048)break;}
   Set<Integer> seen=new HashSet<>();
   for(int e=0;e<entries.size();e++){
    Deque<Integer> work=new ArrayDeque<>();work.add(entries.get(e));boolean bad=false;
    while(!work.isEmpty()){int pc=work.poll();
     while(pc>=0&&pc+2<=f.length&&!seen.contains(pc)){seen.add(pc);int op=u16(f,pc);String[] c=cmds.get(op);
      if(c==null){unknown.merge(op,1,Integer::sum);if(!ctx.containsKey(op)){StringBuilder sb=new StringBuilder();for(int i=pc;i<Math.min(f.length,pc+24);i++)sb.append(String.format("%02x ",f[i]&0xFF));ctx.put(op,"file "+fi+" @"+pc+": "+sb);}bad=true;break;}
      int q=pc+2;StringBuilder args=new StringBuilder();
      for(char s:c[1].toCharArray()){int n=s=='B'?1:s=='H'?2:4;if(q+n>f.length){bad=true;break;}long v=n==1?(f[q]&0xFF):n==2?u16(f,q):(u32(f,q)&0xFFFFFFFFL);
       if(n==4&&(op==0x1E||op==0x1F||op==0x20||op==0x04||op==0x64)){int tgt=(int)(q+4+(int)v);args.append(" @").append(tgt);if(op!=0x64&&tgt>=0&&tgt<f.length)work.add(tgt);}
       else args.append(' ').append(v);q+=n;}
      if(bad)break;
      if(a[0].equals("dump")){out.append(fi).append(';').append(e).append(';').append(pc).append(';').append(c[0]).append(args).append('\n');
       if(op==0x64){int tgt=(int)(pc+8+u32(f,pc+4));if(tgt>=0&&tgt+4<=f.length&&!seen.contains(-tgt-1)){seen.add(-tgt-1);StringBuilder mv=new StringBuilder();int m=tgt;while(m+4<=f.length){int t=u16(f,m),n=u16(f,m+2);if(t==0xFE)break;mv.append(t).append(',').append(n).append(' ');m+=4;if(mv.length()>2000)break;}out.append("mv;").append(fi).append(';').append(tgt).append(';').append(mv).append('\n');}}}
      if(TERMINAL.contains(op)||op==0x1E)break;
      pc=q;}
     if(bad)break;}
    if(bad)badEntries++;else okEntries++;}
  }
  System.out.println("files="+files+" entriesOk="+okEntries+" entriesBad="+badEntries);
  List<Map.Entry<Integer,Integer>> u=new ArrayList<>(unknown.entrySet());u.sort((x,y)->y.getValue()-x.getValue());
  for(int i=0;i<Math.min(40,u.size());i++)System.out.println(String.format("unknown 0x%03X x%d  %s",u.get(i).getKey(),u.get(i).getValue(),ctx.get(u.get(i).getKey())));
  if(a[0].equals("scan")){int[][] sx=narcIndex(a[3]);for(int fi=0;fi<sx.length;fi++){byte[] f=Arrays.copyOfRange(rom,sx[fi][0],sx[fi][1]);int p=0,n=0;while(p+4<=f.length){if(u16(f,p)==0xFD13)break;int off=u32(f,p);int tgt=p+4+off;if(tgt<0||tgt>f.length)break;n++;p+=4;if(n>4096)break;}if(n>0)System.out.println("scan "+a[3]+" file "+fi+" len="+f.length+" entries="+n);}}
  if(a[0].equals("peek")){int[][] px=narcIndex("/a/0/5/7");int fi=Integer.parseInt(a[3]),po=Integer.parseInt(a[4]);byte[] pf=Arrays.copyOfRange(rom,px[fi][0],px[fi][1]);StringBuilder sb=new StringBuilder();for(int i=po;i<Math.min(pf.length,po+40);i++)sb.append(String.format("%02x ",pf[i]&0xFF));System.out.println("file "+fi+" len="+pf.length+" @"+po+": "+sb);}
  if(a[0].equals("dump"))Files.write(Paths.get(a[3]),out.toString().getBytes("UTF-8"));
  if(a[0].equals("infer"))infer(idx,a[3]);
  if(a[0].equals("headers")){byte[] hdr=narcFile("/a/0/1/2",0);StringBuilder sb=new StringBuilder("# hdr;region;bank;map;header;scriptFile;levelScript;textBank;events\n");for(int i=0;i<hdr.length/48;i++){int o=i*48;sb.append("hdr;2;").append(i&0xFF).append(';').append(i>>8).append(';').append(i).append(';').append(u16(hdr,o+6)).append(';').append(u16(hdr,o+8)).append(';').append(u16(hdr,o+10)).append(';').append(u16(hdr,o+22)).append((char)10);}
   // Level scripts (+8): 6-byte typed entries (u16 type, u16 script, u16 0) until a 0 type, then
   // 6-byte var entries (u16 var, u16 value, u16 script) until a 0 var. (Read as 8 bytes until 2026-09-20: only each map's first row was right.)
   int[][] sidx=narcIndex("/a/0/5/7");
   for(int i=0;i<hdr.length/48;i++){int lv=u16(hdr,i*48+8);if(lv>=sidx.length)continue;byte[] f=Arrays.copyOfRange(rom,sidx[lv][0],sidx[lv][1]);int p=0;
    while(p+6<=f.length){int type=u16(f,p);if(type==0){p+=2;break;}sb.append("lvl;2;").append(i).append(';').append(type).append(';').append(u16(f,p+2)).append((char)10);p+=6;}
    while(p+6<=f.length){int var=u16(f,p);if(var==0)break;sb.append("lvlvar;2;").append(i).append(';').append(var).append(';').append(u16(f,p+2)).append(';').append(u16(f,p+4)).append((char)10);p+=6;}}
   Files.write(Paths.get(a[3]),sb.toString().getBytes("UTF-8"));}
 }
 /** Linear decode from pc with known commands only: true when a terminal or jump is reached within 64 steps. */
 static boolean reaches(byte[] f,int pc){for(int step=0;step<64;step++){if(pc<0||pc+2>f.length)return false;int op=u16(f,pc);String[] c=cmds.get(op);if(c==null)return step>=4;if((op&0xC000)!=0)return false;if(TERMINAL.contains(op)||op==0x1E)return true;int q=pc+2;for(char s:c[1].toCharArray())q+=s=='B'?1:s=='H'?2:4;pc=q;}return false;}
 static void infer(int[][] idx,String outTable)throws Exception{
  byte[][] files=new byte[idx.length][];for(int i=0;i<idx.length;i++)files[i]=Arrays.copyOfRange(rom,idx[i][0],idx[i][1]);
  String[] cands={"","H","HH","L","HHH","HL","HHHH","B","BH","BHH","HHHHH","HHHHHH","BB","BBH","BBHH","BL","HHHHHHH","HHHHHHHH","HHHHHHHHH","HHHHHHHHHH","HHHHHHHHHHH","HHL","HHHL"};
  for(int round=0;round<12;round++){
   Map<Integer,List<int[]>> occ=new TreeMap<>();
   for(int fi=0;fi<files.length;fi++){byte[] f=files[fi];if(f.length<4)continue;List<Integer> entries=new ArrayList<>();int p=0;
    while(p+4<=f.length){if(u16(f,p)==0xFD13)break;int off=u32(f,p);int tgt=p+4+off;if(tgt<0||tgt>f.length)break;entries.add(tgt);p+=4;if(entries.size()>2048)break;}
    Set<Integer> seen=new HashSet<>();
    for(int e:entries){Deque<Integer> work=new ArrayDeque<>();work.add(e);
     while(!work.isEmpty()){int pc=work.poll();
      while(pc>=0&&pc+2<=f.length&&!seen.contains(pc)){seen.add(pc);int op=u16(f,pc);String[] c=cmds.get(op);
       if(c==null){occ.computeIfAbsent(op,k->new ArrayList<>()).add(new int[]{fi,pc});break;}
       int q=pc+2;boolean bad=false;for(char s:c[1].toCharArray()){int n=s=='B'?1:s=='H'?2:4;if(q+n>f.length){bad=true;break;}
        if(n==4&&(op==0x1E||op==0x1F||op==0x20||op==0x04)){int tgt=(int)(q+4+u32(f,q));if(tgt>=0&&tgt<f.length)work.add(tgt);}q+=n;}
       if(bad||TERMINAL.contains(op)||op==0x1E)break;pc=q;}}}}
   int added=0;
   for(Map.Entry<Integer,List<int[]>> en:occ.entrySet()){int op=en.getKey();List<int[]> os=en.getValue();if(os.size()<2)continue;
    String best=null;int bestN=0;
    for(String cand:cands){int n=0;int size=0;for(char s:cand.toCharArray())size+=s=='B'?1:s=='H'?2:4;
     for(int[] o:os){if(reaches(files[o[0]],o[1]+2+size))n++;}
     if(n>bestN){bestN=n;best=cand;}}
    if(best!=null&&bestN*10>=os.size()*7){cmds.put(op,new String[]{String.format("CMD_%03X",op),best});added++;System.out.println(String.format("infer 0x%03X = %s (%d/%d)",op,best.isEmpty()?"-":best,bestN,os.size()));}}
   System.out.println("round "+round+" added "+added);if(added==0)break;}
  StringBuilder sb=new StringBuilder();List<Integer> ks=new ArrayList<>(cmds.keySet());Collections.sort(ks);for(int k:ks)sb.append(k).append(';').append(cmds.get(k)[0]).append(';').append(cmds.get(k)[1]).append((char)10);
  Files.write(Paths.get(outTable),sb.toString().getBytes("UTF-8"));
 }
 static byte[] narcFile(String p,int i){int[][] x=narcIndex(p);return Arrays.copyOfRange(rom,x[i][0],x[i][1]);}
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
