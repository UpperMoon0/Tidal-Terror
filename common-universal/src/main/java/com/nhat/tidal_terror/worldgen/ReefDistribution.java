package com.nhat.tidal_terror.worldgen;

import com.nhat.tidal_terror.balance.ReefBalance;
import terrablender.worldgen.noise.*;

/** A separate broad, seeded gate controls reef rarity without shrinking region scale. */
public final class ReefDistribution {
    public static Area mask(long seed) {
        long saltedSeed=seed ^ 0x544944414c524545L;
        java.util.function.LongFunction<AreaContext> context=salt->new AreaContext(25,saltedSeed,salt);
        AreaTransformer0 initial=(random,x,z)->random.nextRandom(ReefBalance.REEF_PATCH_DENOMINATOR)==0?1:0;
        // Public zoom primitives are available on all supported TerraBlender versions.
        AreaFactory area=initial.run(context.apply(1));
        area=ZoomLayer.FUZZY.run(context.apply(2000),area);
        for(int i=0;i<3;i++)area=ZoomLayer.NORMAL.run(context.apply(2001+i),area);
        for(int i=0;i<ReefBalance.REEF_PATCH_ZOOMS;i++)area=ZoomLayer.NORMAL.run(context.apply(1001+i),area);
        return area.make();
    }
    private ReefDistribution() {}
}
