package com.nhat.tidal_terror.gametest;

import com.mojang.datafixers.util.Pair;
import com.nhat.tidal_terror.worldgen.ReefDistribution;
import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.worldgen.IExtendedParameterList;

/** Check the actual injected native parameter list, including native fallback. */
public final class ReefDistributionTests {
    @SuppressWarnings("unchecked")
    public static void verify(GameTestHelper h, Holder<Biome> nativeBiome) {
        long seed=7142026L;
        var parameters=new Climate.ParameterList<>(java.util.List.of(Pair.of(
                Climate.parameters(.4F,0F,-.5F,0F,0F,0F,0F),nativeBiome)));
        var extended=(IExtendedParameterList<Holder<Biome>>)(Object)parameters;
        extended.initializeForTerraBlender(h.getLevel().registryAccess(),RegionType.OVERWORLD,seed);
        var gate=ReefDistribution.mask(seed); var reload=ReefDistribution.mask(seed);
        var search=treeSearch();
        int kept=0,suppressed=0,unchanged=0;
        for(int x=-4096;x<=4096;x+=64)for(int z=-4096;z<=4096;z+=64) {
            for(var point:new Climate.TargetPoint[]{Climate.target(.4F,0F,-.5F,0F,0F,0F),Climate.target(-.8F,0F,-.5F,0F,0F,0F)}) {
                var original=(Holder<Biome>)search.apply(extended.getTree(extended.getUniqueness(x,8,z)),point);
                if(original.is(Region.DEFERRED_PLACEHOLDER))original=parameters.findValue(point);
                var actual=extended.findValuePositional(point,x,8,z);
                h.assertTrue(gate.get(x,z)==reload.get(x,z),"Seeded reef gate changed on reconstruction");
                if(original.is(ReefWorldgen.BIOME)) {
                    if(gate.get(x,z)==0){suppressed++;h.assertTrue(actual==nativeBiome,"Excluded reef did not preserve the native/datapack tree");}
                    else{kept++;h.assertTrue(actual==original,"Selected reef lost its biome");}
                    h.assertTrue(actual==extended.findValuePositional(point,x,24,z),"Reef gate varied vertically");
                }else{unchanged++;h.assertTrue(actual==original,"Changed a non-reef biome candidate");}
            }
        }
        h.assertTrue(kept>0&&suppressed>kept&&unchanged>0,"Fixture missed retained reefs, rarity rejection or non-reef candidates");
        System.out.println("TIDAL_REEF_DISTRIBUTION: kept="+kept+" suppressed="+suppressed+" unchanged="+unchanged);
        h.succeed();
    }
    // Native RTree and its distance metric are package-private; reflect only in
    // this opt-in fixture to compare against TerraBlender's unfiltered selection.
    private static java.util.function.BiFunction<Object,Climate.TargetPoint,Object> treeSearch() {
        try {
            var metric=Class.forName("net.minecraft.world.level.biome.Climate$DistanceMetric");
            var node=Class.forName("net.minecraft.world.level.biome.Climate$RTree$Node");
            var distance=node.getDeclaredMethod("distance",long[].class);distance.setAccessible(true);
            var delegate=java.lang.reflect.Proxy.newProxyInstance(metric.getClassLoader(),new Class<?>[]{metric},
                    (proxy,method,args)->distance.invoke(args[0],args[1]));
            var tree=Class.forName("net.minecraft.world.level.biome.Climate$RTree");
            var search=tree.getDeclaredMethod("search",Climate.TargetPoint.class,metric);search.setAccessible(true);
            return (value,point)->{try{return search.invoke(value,point,delegate);}catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}};
        }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
    }
    private ReefDistributionTests() {}
}
