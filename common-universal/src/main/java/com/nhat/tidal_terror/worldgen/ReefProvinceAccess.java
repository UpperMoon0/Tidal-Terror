package com.nhat.tidal_terror.worldgen;

import net.minecraft.world.level.biome.Climate;

/** Optional biome-source capability; existing sources keep their original terrain. */
public interface ReefProvinceAccess {
    default boolean deep() { return false; }
    default int placementVersion(){return 2;}
    ReefProvinceLayout.Sample province(long seed, int x, int z, Climate.Sampler sampler);
}
