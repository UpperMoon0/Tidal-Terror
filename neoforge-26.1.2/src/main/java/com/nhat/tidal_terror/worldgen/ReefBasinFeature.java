package com.nhat.tidal_terror.worldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
/** RAW_GENERATION runs before coral, ores and native seagrass decoration. */
public final class ReefBasinFeature extends Feature<NoneFeatureConfiguration>{
 public ReefBasinFeature(){super(NoneFeatureConfiguration.CODEC);}
 @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
  var level=c.level();var t=new ReefTerrain(level,c.chunkGenerator());int mx=c.origin().getX()&~15,mz=c.origin().getZ()&~15;
  ReefTerrain.captureOriginal(level,c.chunkGenerator(),mx,mz);
  BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();boolean placed=false;
  for(int x=mx;x<mx+16;x++)for(int z=mz;z<mz+16;z++){
   if(!t.reef(x,z))continue;int floor=t.floor(x,z),sand=6+(int)Math.floorMod((long)x*31+z*17,3);
   for(int y=level.getMinY();y<level.getSeaLevel();y++){
    p.set(x,y,z);var old=level.getBlockState(p);if(old.is(Blocks.BEDROCK))continue;
    var state=y>floor?Blocks.WATER.defaultBlockState():y>floor-sand?Blocks.SAND.defaultBlockState():Blocks.SANDSTONE.defaultBlockState();
    if(!old.equals(state))level.setBlock(p,state,2);
   }
   placed=true;
  }
  if(placed){
   // Excavation exposes former cave biomes. Use Minecraft's native biome
   // container/resolver API so BiomeFilter cannot seed cave decorations in
   // the flooded reef. Preserve the original biome beneath its seabed.
   var chunk=level.getChunk(mx>>4,mz>>4);
   var reef=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrThrow(ReefWorldgen.BIOME);
   var source=c.chunkGenerator().getBiomeSource();
   chunk.fillBiomesFromNoise((qx,qy,qz,sampler)->{
    int x=net.minecraft.core.QuartPos.toBlock(qx),y=net.minecraft.core.QuartPos.toBlock(qy),z=net.minecraft.core.QuartPos.toBlock(qz);
    return y<level.getSeaLevel() && t.reef(x,z) && y>t.floor(x,z)?reef:source.getNoiseBiome(qx,qy,qz,sampler);
   },level.getLevel().getChunkSource().randomState().sampler());
  }
  return placed;
 }
}
