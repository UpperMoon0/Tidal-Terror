package com.nhat.tidal_terror.gametest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import terrablender.worldgen.IExtendedParameterList;
import terrablender.api.RegionType;
import java.util.*;
public final class ReefSurvey {
 private static Climate.ParameterList<net.minecraft.core.Holder<Biome>> parameters(MultiNoiseBiomeSource source) {
  try {var method=MultiNoiseBiomeSource.class.getDeclaredMethod("parameters");method.setAccessible(true);return (Climate.ParameterList<net.minecraft.core.Holder<Biome>>)method.invoke(source);}
  catch(ReflectiveOperationException e){throw new RuntimeException(e);}
 }
 public static void sample(GameTestHelper h) {
  var access=h.getLevel().registryAccess();
  var preset=access.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).value();
  var generator=(NoiseBasedChunkGenerator)preset.overworld().orElseThrow().generator();
  var source=(MultiNoiseBiomeSource)generator.getBiomeSource();
  var baseline=new Climate.ParameterList<>(parameters(source).values());
  for(long seed:new long[]{0L,123456789L,-987654321L,42L,20261006L}) {
   var parameters=((IExtendedParameterList<net.minecraft.core.Holder<Biome>>)parameters(source)).clone();
   var extended=(IExtendedParameterList<net.minecraft.core.Holder<Biome>>)parameters;
   extended.initializeForTerraBlender(access,RegionType.OVERWORLD,seed);
   var sampler=RandomState.create(access,NoiseGeneratorSettings.OVERWORLD,seed).sampler();
   var reefGrid=new boolean[256][256];
   var actual=new TreeMap<String,Integer>();var vanilla=new TreeMap<String,Integer>();var regions=new TreeMap<String,Integer>();
   for(int x=-16384;x<16384;x+=128)for(int z=-16384;z<16384;z+=128) {
    int qx=Math.floorDiv(x,4),qz=Math.floorDiv(z,4);
    var climate=sampler.sample(qx,8,qz);
    var biome=extended.findValuePositional(climate,qx,8,qz);
    reefGrid[(x+16384)/128][(z+16384)/128]=biome.is(com.nhat.tidal_terror.worldgen.ReefWorldgen.BIOME);
    actual.merge(biome.unwrapKey().orElseThrow().identifier().toString(),1,Integer::sum);
    vanilla.merge(baseline.findValue(climate).unwrapKey().orElseThrow().identifier().toString(),1,Integer::sum);
    regions.merge(extended.getRegion(extended.getUniqueness(qx,8,qz)).getName().toString(),1,Integer::sum);
   }
   var spans=new ArrayList<Integer>(); var areas=new ArrayList<Integer>();
   for(int gx=0;gx<256;gx++)for(int gz=0;gz<256;gz++)if(reefGrid[gx][gz]) {
    var queue=new ArrayDeque<int[]>();queue.add(new int[]{gx,gz});reefGrid[gx][gz]=false;
    int minx=gx,maxx=gx,minz=gz,maxz=gz,area=0;boolean edge=false;
    while(!queue.isEmpty()) {var p=queue.removeFirst();int x=p[0],z=p[1];area++;minx=Math.min(minx,x);maxx=Math.max(maxx,x);minz=Math.min(minz,z);maxz=Math.max(maxz,z);edge|=x==0||z==0||x==255||z==255;
     for(var d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {int xx=x+d[0],zz=z+d[1];if(xx>=0&&xx<256&&zz>=0&&zz<256&&reefGrid[xx][zz]){reefGrid[xx][zz]=false;queue.add(new int[]{xx,zz});}}
    }
    if(!edge){spans.add(Math.max(maxx-minx+1,maxz-minz+1)*128);areas.add(area);}
   }
   Collections.sort(spans);Collections.sort(areas);
   System.out.println("REEF_PATCHES seed="+seed+" complete="+spans.size()+" spans="+spans+" cells="+areas);
   System.out.println("REEF_SURVEY seed="+seed+" samples=65536 y=32 spacing=128 square=32768 actual="+actual+" vanilla="+vanilla+" regions="+regions);
  }
  h.succeed();
 }
}
