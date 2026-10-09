package com.nhat.tidal_terror.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/** The same reef decorators can target native generation or private sparse sections. */
public interface ReefBlockAccess {
    LevelReader reader();
    boolean setBlock(BlockPos pos, BlockState state, int flags);
    default BlockState getBlockState(BlockPos pos) { return reader().getBlockState(pos); }
    default FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
    static ReefBlockAccess nativeLevel(WorldGenLevel level) {
        return new ReefBlockAccess() {
            public LevelReader reader() { return level; }
            public boolean setBlock(BlockPos pos, BlockState state, int flags) { return level.setBlock(pos,state,flags); }
        };
    }
}
