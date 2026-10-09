package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.ReefNativeHeightProvider;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.Arrays;
import java.util.function.Supplier;

/** Exact native height evaluation with one noise graph and shared interpolation slices per chunk. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class ProvinceNativeHeightMixin implements ReefNativeHeightProvider {
 @Shadow @Final private Holder<NoiseGeneratorSettings> settings;
 @Shadow @Final private Supplier<Aquifer.FluidPicker> globalFluidPicker;

 @Override public int[] reefNativeHeights(WorldGenLevel level,int chunkX,int chunkZ) {
  var config=settings.value();var shape=config.noiseSettings().clampToHeightAccessor(level);
  int cellHeight=shape.getCellHeight(),width=shape.getCellWidth();
  int minCell=Math.floorDiv(shape.minY(),cellHeight),cells=Math.floorDiv(shape.height(),cellHeight);
  int[] heights=new int[256];Arrays.fill(heights,level.getMinBuildHeight()-1);
  if(cells<=0)return heights;
  int mx=chunkX*16,mz=chunkZ*16;
  var random=level.getLevel().getChunkSource().randomState();
  var opaque=Heightmap.Types.OCEAN_FLOOR_WG.isOpaque();
  // Nonstandard horizontal settings retain the native scalar fallback.
  if(width<=0 || width>16 || 16%width!=0) {
   var generator=(NoiseBasedChunkGenerator)(Object)this;
   for(int x=0;x<16;x++)for(int z=0;z<16;z++)
    heights[x*16+z]=generator.getBaseHeight(mx+x,mz+z,Heightmap.Types.OCEAN_FLOOR_WG,level,random)-1;
   return heights;
  }
  int horizontal=16/width;boolean[] found=new boolean[256];
  var noise=new NoiseChunk(horizontal,random,mx,mz,shape,DensityFunctions.BeardifierMarker.INSTANCE,
      config,globalFluidPicker.get(),Blender.empty());
  noise.initializeForFirstCellX();
  try {
   for(int cx=0;cx<horizontal;cx++) {
    noise.advanceCellX(cx);
    for(int cz=0;cz<horizontal;cz++) {
     int remaining=width*width;
     for(int cy=cells-1;cy>=0 && remaining>0;cy--) {
      noise.selectCellYZ(cy,cz);
      for(int dy=cellHeight-1;dy>=0 && remaining>0;dy--) {
       int y=(minCell+cy)*cellHeight+dy;noise.updateForY(y,(double)dy/cellHeight);
       for(int x=0;x<width;x++) {
        noise.updateForX(mx+cx*width+x,(double)x/width);
        for(int z=0;z<width;z++) {
         int index=(cx*width+x)*16+cz*width+z;if(found[index])continue;
         noise.updateForZ(mz+cz*width+z,(double)z/width);
         var state=((ProvinceNoiseCellAccess)noise).reefInterpolatedState();
         if(opaque.test(state==null?config.defaultBlock():state)) {
          heights[index]=y;found[index]=true;remaining--;
         }
        }
       }
      }
     }
    }
    noise.swapSlices();
   }
  } finally { noise.stopInterpolation(); }
  return heights;
 }
}
