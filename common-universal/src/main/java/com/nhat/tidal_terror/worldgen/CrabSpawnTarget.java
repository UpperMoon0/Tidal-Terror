package com.nhat.tidal_terror.worldgen;

import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.shardback.ShardbackEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Choose a useful crab height; native spawning still decides whether to accept it. */
public final class CrabSpawnTarget {
    public static BlockPos position(MobCategory category, ServerLevel level, BlockPos sampled) {
        if (!level.hasChunkAt(sampled)) return sampled;
        if(category!=ModEntities.SHARDBACK_POOL) {
            if(level.getChunkSource().getGenerator().getBiomeSource() instanceof ReefProvinceAccess access && access.deep()
                    && ModEntities.reefPools().contains(category)) {
                var terrain=new ReefTerrain(level,level.getChunkSource().getGenerator());
                if(terrain.provinceSample(sampled.getX(),sampled.getZ())!=null) {
                    int bottom=terrain.floor(sampled.getX(),sampled.getZ());
                    return new BlockPos(sampled.getX(),bottom+15+level.getRandom().nextInt(60),sampled.getZ());
                }
            }
            return sampled;
        }
        BlockPos floor = new BlockPos(sampled.getX(),
                level.getHeight(Heightmap.Types.OCEAN_FLOOR, sampled.getX(), sampled.getZ()), sampled.getZ());
        var support = level.getBlockState(floor.below());
        if (!ShardbackEntity.isSeabed(support) || !support.isFaceSturdy(level, floor.below(), Direction.UP)
                || !level.getBlockState(floor).is(Blocks.WATER)
                || !level.getBlockState(floor.above()).is(Blocks.WATER)) return sampled;
        return floor;
    }
    private CrabSpawnTarget() {}
}
