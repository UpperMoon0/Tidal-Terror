import com.nhat.tidal_terror.worldgen.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProvinceLocatorSearchCheck {
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) {
        for(int version:new int[]{1,2})for(long seed:new long[]{0,1,-1,Long.MIN_VALUE,Long.MAX_VALUE}) {
            var near=ReefProvinceLayout.center(seed,-1,0,version);
            var far=ReefProvinceLayout.center(seed,-2,0,version);
            var found=ProvinceLocatorSearch.find(seed,0,0,version,c->c.equals(far)||c.equals(near),()->false);
            require(near.equals(found),"Did not select nearest accepted center");
            require(ProvinceLocatorSearch.find(seed,0,0,version,c->false,()->false)==null,"Empty search invented target");
            AtomicInteger calls=new AtomicInteger();
            require(ProvinceLocatorSearch.find(seed,0,0,version,c->{calls.incrementAndGet();return true;},()->true)==null&&calls.get()==0,"Cancelled search evaluated native probes");
            int spacing=ReefProvinceLayout.spacing(version);
            var negative=ReefProvinceLayout.center(seed,-5,-6,version);
            require(negative.equals(ProvinceLocatorSearch.find(seed,negative.x(),negative.z(),version,c->c.equals(negative),()->false)),"Negative coordinate lookup failed");
            calls.set(0);ProvinceLocatorSearch.find(seed,0,0,version,c->{calls.incrementAndGet();return false;},()->false);
            require(calls.get()<=400,"Unbounded candidate search");
            require(spacing==(version==1?12288:8192),"Versioned spacing regressed");
        }
        require(ReefProvinceLayout.center(0,-5,3,1).equals(new ReefProvinceLayout.Center(-55689,43535)),"Legacy seeded center moved");
        System.out.println("PROVINCE_LOCATOR_SEARCH_PASS nearest,empty,cancelled,negative,bounded,legacy");
    }
}
