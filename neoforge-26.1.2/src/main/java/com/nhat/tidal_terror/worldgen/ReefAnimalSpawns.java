package com.nhat.tidal_terror.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
/** Extend native spawning only in the submerged Coral Cathedral. */

public final class ReefAnimalSpawns {
    public static boolean deepWater(LevelReader level, BlockPos pos) {
        return pos.getY() < level.getSeaLevel() - 13
                && level.getBiome(pos).is(ReefWorldgen.BIOME)
                && level.getWorldBorder().isWithinBounds(pos)
                && level.getBlockState(pos).is(Blocks.WATER)
                && level.getBlockState(pos.above()).is(Blocks.WATER)
                && level.getBlockState(pos.below()).is(Blocks.WATER);
    }

}
