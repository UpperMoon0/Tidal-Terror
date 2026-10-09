package com.nhat.tidal_terror.gametest;
import com.nhat.tidal_terror.worldgen.*;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.biome.*;
/** Shared native regression for all loaders, without any biome injection dependency. */
public final class ReefDistributionTests {
    public static void verify(GameTestHelper h,Holder<Biome> ocean) {
        h.assertTrue(ocean.is(net.minecraft.tags.BiomeTags.IS_OCEAN),"Fixture needs tagged vanilla ocean");
        long seed=7142026L;
        var sampler=h.getLevel().getChunkSource().randomState().sampler();
        var source=new ReefProvinceBiomeSource(new FixedBiomeSource(ocean),ocean,ocean,false);
        var reload=new ReefProvinceBiomeSource(new FixedBiomeSource(ocean),ocean,ocean,false);
        var center=ReefProvinceLayout.center(seed,-1,1);
        int admitted=0,outside=0;
        for(int x=-4096;x<=4096;x+=64)for(int z=-4096;z<=4096;z+=64) {
            var first=source.province(seed,center.x()+x,center.z()+z,sampler);
            h.assertTrue(java.util.Objects.equals(first,reload.province(seed,center.x()+x,center.z()+z,sampler)),"Province moved on reconstruction");
            if(first==null)outside++;else admitted++;
        }
        h.assertTrue(admitted>0&&outside>0,"Fixture missed full province or vanilla background");
        h.assertTrue(source.province(seed,center.x(),center.z(),sampler).zone()==ReefProvinceLayout.Zone.CATHEDRAL,"Large core missing");
        ProvinceSeeds.bind(sampler,seed);
        h.assertTrue(source.getNoiseBiome(0,24,0,sampler)==ocean,"Above-sea delegate changed");
        // Direct holders carry no ocean tag: a land-only delegate must reject the entire footprint.
        var land=new ReefProvinceBiomeSource(new FixedBiomeSource(Holder.direct(ocean.value())),ocean,ocean,false);
        h.assertTrue(land.province(seed,center.x(),center.z(),sampler)==null,"Land-only climate admitted a province");
        System.out.println("TIDAL_REEF_DISTRIBUTION: province="+admitted+" background="+outside+" stable,land-rejection");
        ProvincePresetAudit.verify(h);
        h.succeed();
    }
    private ReefDistributionTests(){}
}
