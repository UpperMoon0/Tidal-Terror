import com.nhat.tidal_terror.worldgen.ReefProvinceLayout;
import java.nio.file.Files;
import java.nio.file.Path;

/** Independent layout invariants and a preview sampled from the production model. */
public final class ProvinceLayoutCheck {
    private static void require(boolean c, String m) { if (!c) throw new AssertionError(m); }
    public static void main(String[] args) throws Exception {
        int checks = 0;
        require(ReefProvinceLayout.CORE == 1024, "Large Cathedral footprint regressed");
        require(ReefProvinceLayout.OUTER / ReefProvinceLayout.CORE == 600.0 / 180, "Band proportions changed");
        require(ReefProvinceLayout.OUTER * 1.11 < ReefProvinceLayout.SPACING / 2.0 - ReefProvinceLayout.JITTER, "Province crosses its placement cell");
        for (long seed : new long[]{0, 1, -1, 7142026, Long.MIN_VALUE, Long.MAX_VALUE}) {
            for (int gx = -2; gx <= 2; gx++) for (int gz = -2; gz <= 2; gz++) {
                var center = ReefProvinceLayout.center(seed, gx, gz);
                require(ReefProvinceLayout.sample(seed, center.x(), center.z()).zone() == ReefProvinceLayout.Zone.CATHEDRAL, "Center lost");
                for (int angle = 0; angle < 360; angle += 3) {
                    int previous = 4, lastFloor = -49;
                    double a = Math.toRadians(angle);
                    for (int radius = 0; radius < 4500; radius += 4) {
                        int x = center.x() + (int)Math.round(Math.cos(a) * radius);
                        int z = center.z() + (int)Math.round(Math.sin(a) * radius);
                        var s = ReefProvinceLayout.sample(seed, x, z);
                        require(s.equals(ReefProvinceLayout.sample(seed,x,z)), "Non-deterministic sample");
                        int zone = s.zone().ordinal();
                        require(zone <= previous && previous-zone <= 1, "Missing or reversed ring");
                        int floor = ReefProvinceLayout.floor(s,x,z,25);
                        require(Math.abs(floor-lastFloor) <= 9, "Discontinuous terrain");
                        require(floor >= -56 && floor <= 30, "World-height budget exceeded");
                        require(s.proximity() >= 0 && s.proximity() <= 1, "Unbounded proximity");
                        previous = zone; lastFloor = floor; checks++;
                    }
                    require(previous == 0 && lastFloor == 25, "Outer edge does not match native terrain");
                }
            }
        }
        require(!ReefProvinceLayout.center(0,0,0).equals(ReefProvinceLayout.center(1,0,0)), "World seed ignored");
        String[] colors = {"#1f3b50", "#dbcda7", "#b6ac91", "#787d81", "#41bdae"};
        var c = ReefProvinceLayout.center(0,0,0);
        StringBuilder svg = new StringBuilder("<svg xmlns='http://www.w3.org/2000/svg' width='1000' height='800' viewBox='0 0 1000 800'><rect width='1000' height='800' fill='#101d2b'/><g font-family='sans-serif' fill='#e5eced'><text x='24' y='34' font-size='23'>Sunken Wastes / Reef Province prototype</text><text x='24' y='58' font-size='14'>Seed 0 layout model. Ocean admission and in-world appearance are separate checks.</text></g>");
        for(int x=-4400;x<4400;x+=32) for(int z=-4400;z<4400;z+=32) {
            var s=ReefProvinceLayout.sample(0,c.x()+x,c.z()+z);
            svg.append("<rect x='").append(24+(x+4400)*.07).append("' y='").append(82+(z+4400)*.07)
                .append("' width='2.3' height='2.3' fill='").append(colors[s.zone().ordinal()]).append("'/>");
        }
        for(int i=0;i<colors.length;i++) svg.append("<rect x='675' y='").append(105+i*35).append("' width='20' height='20' fill='").append(colors[i]).append("'/><text x='707' y='").append(121+i*35).append("' fill='#e5eced' font-family='sans-serif' font-size='14'>").append(ReefProvinceLayout.Zone.values()[i].name().replace('_',' ')).append("</text>");
        svg.append("<text x='675' y='320' fill='#e5eced' font-family='sans-serif' font-size='14'>Radial terrain profile, Y</text><path d='M675 460 H965 M675 345 V465' stroke='#8195a1' fill='none'/><polyline points='");
        for(int r=0;r<=4400;r+=16) {
            int floor=ReefProvinceLayout.floor(ReefProvinceLayout.sample(0,c.x()+r,c.z()),c.x()+r,c.z(),25);
            svg.append(675+r*.065).append(',').append(385-floor*1.25).append(' ');
        }
        svg.append("' fill='none' stroke='#dbcda7' stroke-width='2'/><g fill='#bdcbd1' font-family='sans-serif' font-size='13'><text x='675' y='487'>Core</text><text x='890' y='487'>Ocean</text><text x='675' y='540'>Core radius: 1,024 blocks</text><text x='675' y='563'>Rim: 1,024-1,593</text><text x='675' y='586'>Inner Wastes: 1,593-2,389</text><text x='675' y='609'>Outer Wastes: 2,389-3,413</text><text x='675' y='651'>Existing giant coral height retained.</text></g></svg>");
        Files.createDirectories(Path.of("build/province-model"));
        Files.writeString(Path.of("build/province-model/layout.svg"),svg);
        System.out.println("PROVINCE_LAYOUT PASSED: " + checks + " sampled invariants; six seeds, negative cells, 120 directions per province");
    }
}
