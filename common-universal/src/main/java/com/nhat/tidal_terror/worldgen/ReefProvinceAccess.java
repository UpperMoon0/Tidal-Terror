package com.nhat.tidal_terror.worldgen;

import net.minecraft.world.level.biome.Climate;

/** Optional biome-source capability; existing sources keep their original terrain. */
public interface ReefProvinceAccess {
    default void prepareDeepSections(net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.chunk.ChunkAccess chunk) {}
    default boolean deep() { return false; }
    ReefProvinceLayout.Sample province(long seed, int x, int z, Climate.Sampler sampler);
}
