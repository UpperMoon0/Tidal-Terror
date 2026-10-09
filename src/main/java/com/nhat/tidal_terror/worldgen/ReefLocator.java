package com.nhat.tidal_terror.worldgen;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/** One chunk-free locator entry point for both world-generation paths. */
public final class ReefLocator {
    public static ReefProvinceLayout.Center find(ServerLevel level,int x,int z,BooleanSupplier cancelled) {
        var source=level.getChunkSource().getGenerator().getBiomeSource();
        var sampler=level.getChunkSource().randomState().sampler();
        if(source instanceof ReefProvinceAccess province)
            return ProvinceLocatorSearch.find(level.getSeed(),x,z,province.placementVersion(),
                    c->province.province(level.getSeed(),c.x(),c.z(),sampler)!=null,cancelled);
        // 32 quart cells = 128 blocks. Native lookup samples climate, never chunks.
        var found=source.findBiomeHorizontal(x,32,z,ProvinceLocatorSearch.RANGE,32,
                biome->{if(cancelled.getAsBoolean())throw new CancellationException();return biome.is(ReefWorldgen.BIOME);},
                RandomSource.create(level.getSeed()),true,sampler);
        return found==null ? null : new ReefProvinceLayout.Center(found.getFirst().getX(),found.getFirst().getZ());
    }
    private ReefLocator() {}
}
