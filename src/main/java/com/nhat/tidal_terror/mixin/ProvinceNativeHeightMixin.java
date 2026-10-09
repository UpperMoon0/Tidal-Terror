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

/** Same column evaluator as native getBaseHeight, sharing its setup per noise cell. */
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
  for(int cx=Math.floorDiv(mx,width)*width;cx<mx+16;cx+=width)
   for(int cz=Math.floorDiv(mz,width)*width;cz<mz+16;cz+=width) {
    var noise=new NoiseChunk(1,random,cx,cz,shape,DensityFunctions.BeardifierMarker.INSTANCE,
        config,globalFluidPicker.get(),Blender.empty());
    noise.initializeForFirstCellX();noise.advanceCellX(0);
    try {
     int x0=Math.max(mx,cx),x1=Math.min(mx+16,cx+width),z0=Math.max(mz,cz),z1=Math.min(mz+16,cz+width);
     boolean[] found=new boolean[256];int remaining=(x1-x0)*(z1-z0);
     // Fill each vertical native cell once, then visit every unresolved column.
     for(int cy=cells-1;cy>=0 && remaining>0;cy--) {
      noise.selectCellYZ(cy,0);
      for(int dy=cellHeight-1;dy>=0 && remaining>0;dy--) {
       int y=(minCell+cy)*cellHeight+dy;noise.updateForY(y,(double)dy/cellHeight);
       for(int x=x0;x<x1;x++) {
         noise.updateForX(x,(double)(x-cx)/width);
        for(int z=z0;z<z1;z++) {
         int index=(x-mx)*16+z-mz;if(found[index])continue;
         noise.updateForZ(z,(double)(z-cz)/width);
         var state=((ProvinceNoiseCellAccess)noise).reefInterpolatedState();
         if(opaque.test(state==null?config.defaultBlock():state)) {
          heights[index]=y;found[index]=true;remaining--;
         }
        }
       }
      }
     }
    } finally { noise.stopInterpolation(); }
   }
  return heights;
 }
}
