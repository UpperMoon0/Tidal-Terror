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
        if(ReefSpawnHabitat.deep(level)) {
            var terrain=new ReefTerrain(level,level.getChunkSource().getGenerator());
            var province=terrain.provinceSample(sampled.getX(),sampled.getZ());
            if(province!=null) {
                int ceiling=ReefSpawnHabitat.ceiling(level,terrain,sampled.getX(),sampled.getZ());
                if(ModEntities.reefPools().contains(category)) {
                    if(province.zone()!=ReefProvinceLayout.Zone.CATHEDRAL)return sampled;
                    if(category!=ModEntities.SHARDBACK_POOL) {
                        int bottom=terrain.floor(sampled.getX(),sampled.getZ())+5;
                        int top=ceiling-4; // Veilglow's column must fit below the canopy.
                        return top<bottom ? sampled : sampled.atY(bottom+level.getRandom().nextInt(top-bottom+1));
                    }
                } else if(category==MobCategory.WATER_CREATURE || category==MobCategory.WATER_AMBIENT
                        || category==MobCategory.UNDERGROUND_WATER_CREATURE || category==MobCategory.MONSTER
                        || category==MobCategory.CREATURE && province.zone()==ReefProvinceLayout.Zone.CATHEDRAL) {
                    int bottom=terrain.floor(sampled.getX(),sampled.getZ())+1;
                    int top=level.getSeaLevel()-1;
                    return top<bottom ? sampled : sampled.atY(bottom+level.getRandom().nextInt(top-bottom+1));
                }
            }
        }
        if(category!=ModEntities.SHARDBACK_POOL) {
            return sampled;
        }
        BlockPos floor = new BlockPos(sampled.getX(),
                ReefSpawnHabitat.deep(level) ? new ReefTerrain(level,level.getChunkSource().getGenerator()).floor(sampled.getX(),sampled.getZ())+1
                    : level.getHeight(Heightmap.Types.OCEAN_FLOOR, sampled.getX(), sampled.getZ()), sampled.getZ());
        var support = level.getBlockState(floor.below());
        if (!ShardbackEntity.isSeabed(support) || !support.isFaceSturdy(level, floor.below(), Direction.UP)
                || !level.getBlockState(floor).is(Blocks.WATER)
                || !level.getBlockState(floor.above()).is(Blocks.WATER)) return sampled;
        return floor;
    }
    private CrabSpawnTarget() {}
}
