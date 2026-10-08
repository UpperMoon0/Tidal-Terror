package com.nhat.tidal_terror.worldgen;

/** Seeded province geometry. No chunks, registries, or TerraBlender are needed. */
public final class ReefProvinceLayout {
    // Match the recorded large legacy patch's ~3.3 km² area; scale all bands together.
    public static final double SCALE = 1024.0 / 180;
    public static final int SPACING = 12288, JITTER = 1024;
    public static final double CORE = 180 * SCALE, RIM = 280 * SCALE,
            INNER = 420 * SCALE, OUTER = 600 * SCALE, EDGE_BLEND = 100 * SCALE;
    public enum Zone { OCEAN, OUTER_WASTES, INNER_WASTES, RIM, CATHEDRAL }
    public static final int DEEP_BOTTOM = -512, DEEP_CORE = -448;
    public record Center(int x, int z) {}
    public record Sample(Center center, double radius, double angle, Zone zone) {
        public double proximity() { return Math.max(0, Math.min(1, 1 - radius / OUTER)); }
    }
    private ReefProvinceLayout() {}
    private static long hash(long seed, int x, int z) {
        long n = seed ^ ((long)x * 341873128712L) ^ ((long)z * 132897987541L);
        n = (n ^ (n >>> 30)) * 0xbf58476d1ce4e5b9L;
        n = (n ^ (n >>> 27)) * 0x94d049bb133111ebL;
        return n ^ (n >>> 31);
    }
    public static Center center(long seed, int cellX, int cellZ) {
        long h = hash(seed, cellX, cellZ);
        return new Center(cellX * SPACING + SPACING / 2 + (int)Math.floorMod(h, 2 * JITTER + 1) - JITTER,
                cellZ * SPACING + SPACING / 2 + (int)Math.floorMod(h >>> 24, 2 * JITTER + 1) - JITTER);
    }
    public static Sample sample(long seed, int x, int z) {
        // Warped outer radius < 3789; centers stay >= 5120 from cell edges.
        Center c = center(seed, Math.floorDiv(x, SPACING), Math.floorDiv(z, SPACING));
        double dx = (double)x - c.x, dz = (double)z - c.z, a = Math.atan2(dz, dx);
        double phase = (hash(seed, c.x, c.z) >>> 11) * 0x1.0p-53 * Math.PI * 2;
        double warp = 1 + .075 * Math.sin(3 * a + phase) + .035 * Math.sin(7 * a - phase);
        double r = Math.hypot(dx, dz) / warp;
        Zone zone = r > OUTER ? Zone.OCEAN : r > INNER ? Zone.OUTER_WASTES
                : r > RIM ? Zone.INNER_WASTES : r > CORE ? Zone.RIM : Zone.CATHEDRAL;
        return new Sample(c, r, a, zone);
    }
    private static double smooth(double a, double b, double r) {
        double t = Math.max(0, Math.min(1, (r - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }
    public static int floor(Sample s, int x, int z, int nativeFloor) {
        double r = s.radius, h;
        if (r <= CORE) h = -49;
        else if (r <= RIM) {
            // Three broad channels cut the raised dead-reef rim.
            double channel = Math.pow(Math.max(0, Math.cos(3 * s.angle)), 14);
            h = -49 + smooth(CORE, RIM - 35 * SCALE, r) * (49 - 25 * channel);
        } else if (r <= INNER) {
            double channel = Math.pow(Math.max(0, Math.cos(3 * s.angle)), 14);
            h = -18 + (18 - 25 * channel) * (1 - smooth(RIM, RIM + 55 * SCALE, r));
        }
        else h = -18 + 36 * smooth(INNER, OUTER - EDGE_BLEND, r);
        double dunes = (r <= CORE ? 1 : 2.2) * (Math.sin(x * .034 + Math.sin(z * .005) * 2)
                + Math.cos(z * .027) + .5 * Math.sin((x + z) * .069));
        h += dunes;
        // Blend both dune amplitude and floor into native terrain at the outer edge.
        double blend = smooth(OUTER - EDGE_BLEND, OUTER, r);
        return (int)Math.round(h * (1 - blend) + nativeFloor * blend);
    }
    /** Versioned deep profile: the shallow preset retains its original floor(). */
    public static int deepFloor(Sample s, int x, int z, int nativeFloor) {
        double r=s.radius, h;
        double channel=Math.pow(Math.max(0,Math.cos(3*s.angle)),14);
        if(r<=CORE) h=DEEP_CORE;
        else if(r<=RIM) h=DEEP_CORE+smooth(CORE,RIM,r)*(192-32*channel);
        else if(r<=INNER) h=-256-32*channel+(112+32*channel)*smooth(RIM,INNER,r);
        else h=-144+162*smooth(INNER,OUTER-EDGE_BLEND,r);
        h+=(r<=CORE?1:2.2)*(Math.sin(x*.034+Math.sin(z*.005)*2)+Math.cos(z*.027)+.5*Math.sin((x+z)*.069));
        double blend=smooth(OUTER-EDGE_BLEND,OUTER,r);
        return (int)Math.round(h*(1-blend)+nativeFloor*blend);
    }
    /** A single sealed sheet below the sediment; normal-height columns keep Y -64. */
    public static int bedrockBase(int floor) { return Math.min(-64, floor-12); }
    public static boolean bedrock(long seed,int x,int y,int z,int floor) {
        int base=bedrockBase(floor);
        int thickness=base< -64?4+(int)Math.floorMod(hash(seed,x,z),3):1+(int)Math.floorMod(hash(seed,x,z),5);
        return y>=base && y<base+thickness;
    }
}
