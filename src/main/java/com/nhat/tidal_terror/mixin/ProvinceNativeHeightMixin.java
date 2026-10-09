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
     for(int x=Math.max(mx,cx);x<Math.min(mx+16,cx+width);x++)
      for(int z=Math.max(mz,cz);z<Math.min(mz+16,cz+width);z++) {
       boolean found=false;
       for(int cy=cells-1;cy>=0 && !found;cy--) {
        noise.selectCellYZ(cy,0);
        for(int dy=cellHeight-1;dy>=0;dy--) {
         int y=(minCell+cy)*cellHeight+dy;
         noise.updateForY(y,(double)dy/cellHeight);
         noise.updateForX(x,(double)(x-cx)/width);
         noise.updateForZ(z,(double)(z-cz)/width);
         var state=((ProvinceNoiseCellAccess)noise).reefInterpolatedState();
         if(opaque.test(state==null?config.defaultBlock():state)) {
          heights[(x-mx)*16+z-mz]=y;found=true;break;
         }
        }
       }
      }
    } finally { noise.stopInterpolation(); }
   }
  return heights;
 }
}
