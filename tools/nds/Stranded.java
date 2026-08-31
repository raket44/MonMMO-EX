import java.nio.file.*;import java.util.*;

/** Can a player who walks into a map always walk back out of it somewhere? */
public class Stranded{
 public static void main(String[] a)throws Exception{
  Map<Integer,List<int[]>> byMap=new HashMap<>();      // map key -> warps out
  Set<Integer> reached=new TreeSet<>();
  List<int[]> all=new ArrayList<>();
  for(String line:Files.readAllLines(Paths.get(a[0]))){
   String[] p=line.split(";");if(p.length<10)continue;
   int[] v=new int[10];for(int i=0;i<10;i++)v[i]=Integer.parseInt(p[i].trim());
   int src=(v[1]<<8)|v[2], dst=(v[6]<<8)|v[7];
   byMap.computeIfAbsent(src,k->new ArrayList<>()).add(new int[]{dst,v[8],v[9]});
   reached.add(dst);all.add(new int[]{src,dst});
  }
  int stranded=0;List<String> names=new ArrayList<>();
  for(int dst:reached){
   if(!byMap.containsKey(dst)){stranded++;
    if(names.size()<10)names.add("bank "+(dst>>8)+" map "+(dst&0xFF));}
  }
  System.out.println("maps you can walk into      "+reached.size());
  System.out.println("of those, with no way out   "+stranded);
  names.forEach(n->System.out.println("   "+n));
  // one-way pairs: A->B exists but no warp anywhere in B returns to A
  int oneWay=0;
  for(int[] e:all){
   List<int[]> outs=byMap.get(e[1]);
   boolean back=outs!=null&&outs.stream().anyMatch(o->o[0]==e[0]);
   if(!back)oneWay++;
  }
  System.out.println("one-way links (no warp back to the source map) "+oneWay+" of "+all.size());
 }
}
