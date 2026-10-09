package com.nhat.tidal_terror.worldgen;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import java.util.*;
/** Climate sampling never requests distant chunks during decoration. */
public final class ReefTerrain {
 private record ChunkKey(ChunkGenerator generator,long seed,int x,int z){}
 private static final Map<ChunkKey,int[]> ORIGINAL=Collections.synchronizedMap(new LinkedHashMap<>(2048,.75F,true){
  @Override protected boolean removeEldestEntry(Map.Entry<ChunkKey,int[]> e){return size()>2048;}
 });
 private static final ChunkColumnCache<ChunkKey> NOISE=new ChunkColumnCache<>(2048);
 public static void captureOriginal(WorldGenLevel level,ChunkGenerator generator,int mx,int mz){
  int[] heights=new int[256];
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)heights[x*16+z]=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,mx+x,mz+z)-1;
  ORIGINAL.put(new ChunkKey(generator,level.getSeed(),mx>>4,mz>>4),heights);
 }
 private int originalFloor(int x,int z,boolean anchor){
  if(!anchor){int[] heights=ORIGINAL.get(new ChunkKey(generator,level.getSeed(),x>>4,z>>4));if(heights!=null)return heights[(x&15)*16+(z&15)];}
  ChunkKey key=new ChunkKey(generator,level.getSeed(),x>>4,z>>4);
  if(generator instanceof ReefNativeHeightProvider batch)
   return NOISE.getBatch(key,(x&15)*16+(z&15),()->batch.reefNativeHeights(level,x>>4,z>>4));
  return NOISE.get(key,(x&15)*16+(z&15),()->generator.getBaseHeight(x,z,net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,
      level,level.getLevel().getChunkSource().randomState())-1);
 }
 private final Map<Long,Integer> anchors=new HashMap<>();
 public int anchorFloor(int x,int z){long key=((long)x<<32)^(z&0xffffffffL);return anchors.computeIfAbsent(key,k->calculateFloor(x,z,true));}
 private final WorldGenLevel level;private final ChunkGenerator generator;private final Map<Long,Boolean> samples=new HashMap<>();private final Map<Long,Integer> floors=new HashMap<>();
 public ReefTerrain(WorldGenLevel level,ChunkGenerator generator){this.level=level;this.generator=generator;}
 public ReefProvinceLayout.Sample provinceSample(int x,int z){
  var source=generator.getBiomeSource();
  return source instanceof ReefProvinceAccess access?access.province(level.getSeed(),x,z,level.getLevel().getChunkSource().randomState().sampler()):null;
 }
 public boolean province(int x,int z){return provinceSample(x,z)!=null||reef(x,z);}
 public boolean reef(int x,int z){
  int qx=QuartPos.fromBlock(x),qz=QuartPos.fromBlock(z);long key=((long)qx<<32)^(qz&0xffffffffL);
  return samples.computeIfAbsent(key,k->generator.getBiomeSource().getNoiseBiome(qx,8,qz,level.getLevel().getChunkSource().randomState().sampler()).is(ReefWorldgen.BIOME));
 }
 public int floor(int x,int z){
  long key=((long)x<<32)^(z&0xffffffffL);
  return floors.computeIfAbsent(key,k->calculateFloor(x,z,false));
 }
 private int calculateFloor(int x,int z,boolean anchor){
  var province=provinceSample(x,z);
  if(province!=null){int nativeFloor=province.radius()<ReefProvinceLayout.OUTER-ReefProvinceLayout.EDGE_BLEND?0:originalFloor(x,z,true);
   return generator.getBiomeSource() instanceof ReefProvinceAccess access && access.deep()?ReefProvinceLayout.deepFloor(province,x,z,nativeFloor):ReefProvinceLayout.floor(province,x,z,nativeFloor);}
  double edge=1;
  for(int i=0;i<8;i++){
   double a=i*Math.PI/4;
   for(int distance=32;distance<=192;distance+=32){
    if(reef(x+(int)Math.round(Math.cos(a)*distance),z+(int)Math.round(Math.sin(a)*distance)))continue;
    int lo=distance-32,hi=distance;
    while(hi-lo>1){int mid=(hi+lo)/2;
     if(reef(x+(int)Math.round(Math.cos(a)*mid),z+(int)Math.round(Math.sin(a)*mid)))lo=mid;else hi=mid;
    }
    edge=Math.min(edge,hi/192.0);break;
   }
  }
  double smooth=edge*edge*(3-2*edge),dunes=2*Math.sin(x*.034)+2*Math.cos(z*.027)+Math.sin((x+z)*.069);
  double deep=-49+dunes;
  if(edge>=1)return (int)Math.round(deep);
  // Match the native noise terrain at the actual boundary, instead of a fixed Y=23 shelf.
  int nativeFloor=originalFloor(x,z,anchor);
  return (int)Math.round(nativeFloor+(deep-nativeFloor)*smooth);
 }
 public boolean giant(int x,int z){
  if(!reef(x,z)||anchorFloor(x,z)>-40)return false;
  for(int i=0;i<8;i++)if(!reef(x+(int)(Math.cos(i*Math.PI/4)*38),z+(int)(Math.sin(i*Math.PI/4)*38)))return false;
  return true;
 }
}
