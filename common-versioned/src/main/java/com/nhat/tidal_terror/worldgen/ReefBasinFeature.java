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
   if(!t.province(x,z))continue;int floor=t.floor(x,z),sand=6+(int)Math.floorMod((long)x*31+z*17,3);
   // Accepted province rings can cross native coastal land. Sculpt the whole
   // original surface column so excavation cannot leave floating land above water.
   int top=t.provinceSample(x,z)==null?level.getSeaLevel():Math.min(level.getMaxBuildHeight(),Math.max(floor+1,
       Math.max(level.getSeaLevel(),level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,x,z))));
   for(int y=level.getMinBuildHeight();y<top;y++){
    p.set(x,y,z);var old=level.getBlockState(p);
    boolean deep=c.chunkGenerator().getBiomeSource() instanceof ReefProvinceAccess access && access.deep();
    if(old.is(Blocks.BEDROCK) && !deep)continue;
    var state=y>floor?(y<level.getSeaLevel()?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState()):y>floor-sand?Blocks.SAND.defaultBlockState():Blocks.SANDSTONE.defaultBlockState();
    if(deep && ReefProvinceLayout.bedrock(level.getSeed(),x,y,z,floor))state=Blocks.BEDROCK.defaultBlockState();
    if(!old.equals(state))level.setBlock(p,state,2);
   }
   placed=true;
  }
  if(placed){
   // Excavation exposes former cave biomes. Use Minecraft's native biome
   // container/resolver API so BiomeFilter cannot seed cave decorations in
   // the flooded reef. Preserve the original biome beneath its seabed.
   var chunk=level.getChunk(mx>>4,mz>>4);
   var reef=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(ReefWorldgen.BIOME);
   var source=c.chunkGenerator().getBiomeSource();
   chunk.fillBiomesFromNoise((qx,qy,qz,sampler)->{
    int x=net.minecraft.core.QuartPos.toBlock(qx),y=net.minecraft.core.QuartPos.toBlock(qy),z=net.minecraft.core.QuartPos.toBlock(qz);
    if(y<level.getSeaLevel() && t.province(x,z) && y>t.floor(x,z))
     return t.provinceSample(x,z)!=null?source.getNoiseBiome(qx,8,qz,sampler):reef;
    return source.getNoiseBiome(qx,qy,qz,sampler);
   },level.getLevel().getChunkSource().randomState().sampler());
  }
  return placed;
 }
}
