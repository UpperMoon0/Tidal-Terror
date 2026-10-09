import com.nhat.tidal_terror.worldgen.ReefProvinceLayout;

/** Geometric sealing, vertical budget and shallow-preset preservation. */
public final class DeepLayoutCheck {
    private static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
    public static void main(String[] args) {
        long checks=0;
        for(long seed:new long[]{0,1,-1,7142026,Long.MIN_VALUE,Long.MAX_VALUE})
            for(int gx=-1;gx<=1;gx++)for(int gz=-1;gz<=1;gz++) {
                var c=ReefProvinceLayout.center(seed,gx,gz);
                for(int angle=0;angle<360;angle+=3)for(int r=0;r<4500;r+=4) {
                    double a=Math.toRadians(angle);
                    int x=c.x()+(int)Math.round(Math.cos(a)*r),z=c.z()+(int)Math.round(Math.sin(a)*r);
                    var sample=ReefProvinceLayout.sample(seed,x,z);
                    int floor=ReefProvinceLayout.deepFloor(sample,x,z,25),bed=ReefProvinceLayout.bedrockBase(floor);
                    require(floor>=-455 && floor<=30,"Deep world-height budget exceeded");
                    require(bed>=ReefProvinceLayout.DEEP_BOTTOM && bed<floor-5,"Bedrock outside foundation");
                    require(ReefProvinceLayout.bedrock(seed,x,bed,z,floor),"Unsealed base column");
                    for(int[] d:new int[][]{{1,0},{0,1}}) {
                        int nx=x+d[0],nz=z+d[1];
                        int next=ReefProvinceLayout.deepFloor(ReefProvinceLayout.sample(seed,nx,nz),nx,nz,25);
                        require(Math.abs(next-floor)<=3,"Excessive adjacent-column step");
                        int nb=ReefProvinceLayout.bedrockBase(next);
                        require(ReefProvinceLayout.bedrock(seed,x,Math.max(bed,nb),z,floor)
                            && ReefProvinceLayout.bedrock(seed,nx,Math.max(bed,nb),nz,next),"Disconnected bedrock sheet");
                    }
                    if(sample.zone()==ReefProvinceLayout.Zone.OCEAN)require(floor==25,"Outside deep terrain changed");
                    if(sample.zone()==ReefProvinceLayout.Zone.CATHEDRAL) {
                        require(floor < -400,"Core never enters sparse space");
                        require(ReefProvinceLayout.floor(sample,x,z,25)>-64,"Shallow profile changed");
                    }
                    checks++;
                }
            }
        System.out.println("DEEP_LAYOUT PASSED: "+checks+" columns; six seeds, shallow preservation and sealed cardinal bedrock joins");
    }
}
