package com.nhat.tidal_terror.worldgen;

/** Seeded world-coordinate fields: no chunk quota or chunk-centre anchors. */
public final class ReefGardenLayout {
    public record Colony(int x, int z, long seed, boolean rock, int height, int color, int form, double yaw) {}
    private static double random(long seed, int x, int z) {
        return (CoralCathedralFeature.seed(seed, x, z) >>> 11) * 0x1.0p-53;
    }
    private static double noise(long seed, int x, int z, int size) {
        int gx = Math.floorDiv(x, size), gz = Math.floorDiv(z, size);
        double tx = Math.floorMod(x, size) / (double) size, tz = Math.floorMod(z, size) / (double) size;
        tx = tx * tx * (3 - 2 * tx); tz = tz * tz * (3 - 2 * tz);
        double a = random(seed, gx, gz), b = random(seed, gx + 1, gz);
        double c = random(seed, gx, gz + 1), d = random(seed, gx + 1, gz + 1);
        return (a + (b-a)*tx)*(1-tz) + (c + (d-c)*tx)*tz;
    }
    public static double density(long seed, int x, int z) {
        return .7 * noise(seed ^ 0x7afad123L, x, z, 96) + .3 * noise(seed ^ 0x321ab76L, x, z, 27);
    }
    private static boolean candidate(long seed, int x, int z) {
        return random(seed ^ 0x501fa1L, x, z) < .024 * Math.pow(density(seed,x,z), 1.7);
    }
    public static Colony colony(long seed, int x, int z) {
        if (!candidate(seed,x,z)) return null;
        double priority = random(seed ^ 0x42a37L,x,z);
        // Thin close neighbours using a symmetric priority rule. The same point
        // is accepted regardless of which neighbouring chunk asks for its slice.
        for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++) {
            if(dx*dx+dz*dz==0 || dx*dx+dz*dz>25)continue;
            if(candidate(seed,x+dx,z+dz) && random(seed ^ 0x42a37L,x+dx,z+dz)<priority)return null;
        }
        long s=CoralCathedralFeature.seed(seed ^ 0x53af61L,x,z);
        var r=new java.util.Random(s);
        boolean rock=r.nextInt(5)==0;
        return new Colony(x,z,s,rock,rock?2+r.nextInt(4):2+r.nextInt(10),r.nextInt(5),r.nextInt(3),r.nextDouble()*Math.PI*2);
    }
    public static double decorationChance(long seed,int x,int z) {
        return .12 + .62*Math.pow(density(seed ^ 0x591cL,x,z),1.4);
    }
}
