package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Retain a discarded waterlogged plant's fluid in native reef postprocessing. */
@Mixin(LevelChunk.class)
public abstract class ReefWaterPostprocessMixin {
    @Redirect(method="postProcessGeneration",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;updateFromNeighbourShapes(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState tidalterror$retainReefWater(BlockState original,LevelAccessor access,BlockPos pos){
        BlockState updated=Block.updateFromNeighbourShapes(original,access,pos);
        if(updated.isAir() && original.getFluidState().is(FluidTags.WATER)
                && access instanceof ServerLevel level && pos.getY()<level.getSeaLevel()
                && level.getBiome(pos).is(ReefWorldgen.BIOME)){
            if(Boolean.getBoolean("tidalterror.reefAudit"))
                System.out.println("REEF_POSTPROCESS RETAIN_WATER "+pos+" original="+original);
            return Blocks.WATER.defaultBlockState();
        }
        return updated;
    }
}
