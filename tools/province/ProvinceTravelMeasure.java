import com.nhat.tidal_terror.worldgen.ReefProvinceLayout;
import java.nio.file.*;

/** Distances from origin to already native-validated accepted provinces. */
public final class ProvinceTravelMeasure {
    public static void main(String[] args) throws Exception {
        System.out.println("seed,cell_x,cell_z,center_x,center_z,center_distance,province_edge_distance,cathedral_edge_distance");
        for(String line:Files.readAllLines(Path.of(args[0]))) {
            String[] p=line.split(",");long seed=Long.parseLong(p[0]);
            int cx=Integer.parseInt(p[1]),cz=Integer.parseInt(p[2]);
            var c=ReefProvinceLayout.center(seed,cx,cz);
            double edge=Double.POSITIVE_INFINITY,core=edge;
            for(int i=0;i<7200;i++) {
                double a=i*Math.PI*2/7200;
                int dx=(int)Math.round(4096*Math.cos(a)),dz=(int)Math.round(4096*Math.sin(a));
                var s=ReefProvinceLayout.sample(seed,c.x()+dx,c.z()+dz);
                if(!s.center().equals(c))throw new AssertionError("Angular probe left cell");
                double f=ReefProvinceLayout.OUTER/s.radius();
                edge=Math.min(edge,Math.hypot(c.x()+dx*f,c.z()+dz*f));
                f=ReefProvinceLayout.CORE/s.radius();
                core=Math.min(core,Math.hypot(c.x()+dx*f,c.z()+dz*f));
            }
            System.out.printf(java.util.Locale.ROOT,"%d,%d,%d,%d,%d,%.3f,%.3f,%.3f%n",seed,cx,cz,c.x(),c.z(),Math.hypot(c.x(),c.z()),edge,core);
        }
    }
}
