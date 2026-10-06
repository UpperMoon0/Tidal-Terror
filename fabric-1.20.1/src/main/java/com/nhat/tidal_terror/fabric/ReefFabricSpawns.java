package com.nhat.tidal_terror.fabric;

import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;

final class ReefFabricSpawns {
    static void register() {
        SpawnPlacements.register(ModEntities.SHARDBACK.get(), SpawnPlacements.Type.IN_WATER,
            Heightmap.Types.OCEAN_FLOOR, (type,level,reason,pos,random) ->
                level.getBiome(pos).is(ReefWorldgen.BIOME) && pos.getY()<level.getSeaLevel()-4
                && level.getBlockState(pos).is(Blocks.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER)
                && com.nhat.tidal_terror.entities.shardback.ShardbackEntity.isSeabed(level.getBlockState(pos.below()))
                && level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP));
        SpawnPlacements.register(ModEntities.VEILGLOW.get(), SpawnPlacements.Type.IN_WATER,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type,level,reason,pos,random) -> {
                if(!level.getBiome(pos).is(ReefWorldgen.BIOME) || pos.getY()>=level.getSeaLevel()-8) return false;
                for(int y=-1;y<=3;y++) if(!level.getBlockState(pos.above(y)).is(Blocks.WATER))return false;
                return true;
            });
        SpawnPlacements.register(ModEntities.CATHEDRAL_RAY.get(), SpawnPlacements.Type.IN_WATER,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type,level,reason,pos,random) ->
                level.getBiome(pos).is(ReefWorldgen.BIOME) && pos.getY()<level.getSeaLevel()-4
                && level.getBlockState(pos).is(Blocks.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER)
                && level.getBlockState(pos.below()).is(Blocks.WATER));
        SpawnPlacements.register(ModEntities.CORAL_CRUSHER.get(), SpawnPlacements.Type.IN_WATER,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type,level,reason,pos,random) ->
                level.getBiome(pos).is(ReefWorldgen.BIOME) && pos.getY()<level.getSeaLevel()-4
                && level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                && level.getBlockState(pos.above()).is(Blocks.WATER) && level.getBlockState(pos.below()).is(Blocks.WATER));
    }
}
