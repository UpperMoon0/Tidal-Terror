import com.nhat.tidal_terror.worldgen.CoralGeometry;
import java.nio.file.*;
import java.util.*;
public class ExportCoral {
 public static void main(String[] args)throws Exception{
  Path out=Path.of(args[0]);Files.createDirectories(out);
  for(int style=0;style<3;style++){
   var p=CoralGeometry.build(717+style,108,style);List<String> lines=new ArrayList<>();
   for(var e:p.blocks().entrySet()){var v=e.getKey();lines.add(v.x()+","+v.y()+","+v.z()+","+e.getValue());}
   Collections.sort(lines);Files.write(out.resolve("coral-"+style+".csv"),lines);
   System.out.println("style="+style+" voxels="+lines.size());
  }
 }
}
