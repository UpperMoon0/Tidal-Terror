import com.nhat.tidal_terror.worldgen.ReefProvinceLayout;
import java.nio.file.*;

/** Distances from origin to already native-validated accepted provinces. */
public final class ProvinceTravelMeasure {
    public static void main(String[] args) throws Exception {
        int version=args.length>1?Integer.parseInt(args[1]):2;
        System.out.println("seed,cell_x,cell_z,origin_x,origin_z,center_x,center_z,center_distance,province_edge_distance,cathedral_edge_distance");
        for(String line:Files.readAllLines(Path.of(args[0]))) {
            String[] p=line.split(",");long seed=Long.parseLong(p[0]);
            int cx=Integer.parseInt(p[1]),cz=Integer.parseInt(p[2]);
            int ox=p.length>3?Integer.parseInt(p[3]):0,oz=p.length>4?Integer.parseInt(p[4]):0;
            var c=ReefProvinceLayout.center(seed,cx,cz,version);
            double edge=Double.POSITIVE_INFINITY,core=edge;
            for(int i=0;i<7200;i++) {
                double a=i*Math.PI*2/7200;
                int dx=(int)Math.round(2048*Math.cos(a)),dz=(int)Math.round(2048*Math.sin(a));
                var s=ReefProvinceLayout.sample(seed,c.x()+dx,c.z()+dz,version);
                if(!s.center().equals(c))throw new AssertionError("Angular probe left cell");
                double f=ReefProvinceLayout.OUTER/s.radius();
                edge=Math.min(edge,Math.hypot(c.x()+dx*f-ox,c.z()+dz*f-oz));
                f=ReefProvinceLayout.CORE/s.radius();
                core=Math.min(core,Math.hypot(c.x()+dx*f-ox,c.z()+dz*f-oz));
            }
            System.out.printf(java.util.Locale.ROOT,"%d,%d,%d,%d,%d,%d,%d,%.3f,%.3f,%.3f%n",seed,cx,cz,ox,oz,c.x(),c.z(),Math.hypot((double)c.x()-ox,(double)c.z()-oz),edge,core);
        }
    }
}
