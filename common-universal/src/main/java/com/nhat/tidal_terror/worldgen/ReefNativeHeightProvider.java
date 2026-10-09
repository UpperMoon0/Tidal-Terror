package com.nhat.tidal_terror.worldgen;
import net.minecraft.world.level.WorldGenLevel;
/** Optional version adapter for exact, batched native noise heights. */
public interface ReefNativeHeightProvider {
 int[] reefNativeHeights(WorldGenLevel level,int chunkX,int chunkZ);
}
