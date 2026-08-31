import java.nio.file.*;import java.util.*;

/**
 * Player-visible check over the generated table: walk through every door, then look for a warp on
 * the tile you land on that leads back where you came from. That is the property that matters -
 * "can I get out of the building I just entered".
 */
public class RoundTrip{
 record Key(int bank,int map,int x,int y){}
 record Dest(int bank,int map,int x,int y,int dir){}
 public static void main(String[] a)throws Exception{
  Map<Key,Dest> warps=new LinkedHashMap<>();
  for(String line:Files.readAllLines(Paths.get(a[0]))){
   String[] p=line.split(";");
   if(p.length<10)continue;
   int[] v=new int[10];
   for(int i=0;i<10;i++)v[i]=Integer.parseInt(p[i].trim());
   warps.put(new Key(v[1],v[2],v[3],v[4]),new Dest(v[6],v[7],v[8],v[9],v[5]));
  }
  int total=0,ok=0,noReturn=0,returnsElsewhere=0;
  List<String> broken=new ArrayList<>();
  for(Map.Entry<Key,Dest> e:warps.entrySet()){
   total++;
   Key k=e.getKey();Dest d=e.getValue();
   Dest back=warps.get(new Key(d.bank(),d.map(),d.x(),d.y()));
   if(back==null){noReturn++;
    if(broken.size()<6)broken.add("no way back: "+k.bank()+":"+k.map()+" ("+k.x()+","+k.y()+") -> "
      +d.bank()+":"+d.map()+" ("+d.x()+","+d.y()+")");
    continue;}
   if(back.bank()==k.bank()&&back.map()==k.map())ok++;
   else{returnsElsewhere++;
    if(broken.size()<6)broken.add("returns to a third map: "+k.bank()+":"+k.map()+" -> "
      +d.bank()+":"+d.map()+" -> "+back.bank()+":"+back.map());}
  }
  System.out.println("warps                 "+total);
  System.out.println("round trip works      "+ok);
  System.out.println("landing tile has no warp back "+noReturn);
  System.out.println("comes back to a different map "+returnsElsewhere);
  broken.forEach(b->System.out.println("  "+b));
 }
}
