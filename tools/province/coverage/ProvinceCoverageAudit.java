package com.nhat.tidal_terror.worldgen;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in biome-area survey; never requests survey chunks or changes player worlds. */
@Mod.EventBusSubscriber(modid="tidalterror")
public final class ProvinceCoverageAudit {
    @SubscribeEvent public static void start(ServerStartedEvent event) {
        if(!Boolean.getBoolean("tidalterror.coverageSurvey"))return;
        var server=event.getServer();
        new Thread(() -> {
            try {
                var generator=(NoiseBasedChunkGenerator)server.overworld().getChunkSource().getGenerator();
                var source=(ReefProvinceBiomeSource)generator.getBiomeSource();
                var noises=server.registryAccess().lookupOrThrow(Registries.NOISE);
                List<Object> seeds=new ArrayList<>();
                int extent=Integer.getInteger("tidalterror.coverageExtent",15);
                int side=16, points=side*side;
                for(long seed:new long[]{0,1,-1,7142026,Long.MIN_VALUE,Long.MAX_VALUE}) {
                    var sampler=RandomState.create(generator.generatorSettings().value(),noises,seed).sampler();
                    List<Object> cells=new ArrayList<>();
                    for(int cx=-extent;cx<=extent;cx++) for(int cz=-extent;cz<=extent;cz++) {
                        var c=ReefProvinceLayout.center(seed,cx,cz);
                        boolean accepted=source.province(seed,c.x(),c.z(),sampler)!=null;
                        long salt=seed^((long)cx*341873128712L)^((long)cz*132897987541L)^0x53a9b947e621L;
                        var rng=new SplittableRandom(salt);
                        int ocean=0, covered=0, province=0, land=0, core=0, coreOcean=0;
                        // One randomly jittered point in each equally sized stratum.
                        for(int sx=0;sx<side;sx++) for(int sz=0;sz<side;sz++) {
                            int unit=ReefProvinceLayout.SPACING/side;
                            int x=cx*ReefProvinceLayout.SPACING+sx*unit+rng.nextInt(unit);
                            int z=cz*ReefProvinceLayout.SPACING+sz*unit+rng.nextInt(unit);
                            boolean wet=source.delegate().getNoiseBiome(QuartPos.fromBlock(x),8,QuartPos.fromBlock(z),sampler).is(BiomeTags.IS_OCEAN);
                            var sample=ReefProvinceLayout.sample(seed,x,z);
                            boolean inside=accepted&&sample.zone()!=ReefProvinceLayout.Zone.OCEAN;
                            if(wet)ocean++;
                            if(inside) {
                                province++;
                                if(wet)covered++;else land++;
                                if(sample.zone()==ReefProvinceLayout.Zone.CATHEDRAL) {core++;if(wet)coreOcean++;}
                            }
                        }
                        Map<String,Object> row=new LinkedHashMap<>();
                        row.put("cell_x",cx);row.put("cell_z",cz);row.put("accepted",accepted);
                        row.put("samples",points);row.put("vanilla_ocean",ocean);row.put("province_ocean",covered);
                        row.put("province",province);row.put("converted_non_ocean",land);
                        row.put("cathedral",core);row.put("cathedral_ocean",coreOcean);
                        int fineInside=0,fineOcean=0,fineCore=0,fineCoreOcean=0;
                        int fineSide=128,fineUnit=ReefProvinceLayout.SPACING/fineSide;
                        if(accepted) {
                            var fineRng=new SplittableRandom(salt^0x146f9282730L);
                            for(int sx=0;sx<fineSide;sx++) for(int sz=0;sz<fineSide;sz++) {
                                int x=cx*ReefProvinceLayout.SPACING+sx*fineUnit+fineRng.nextInt(fineUnit);
                                int z=cz*ReefProvinceLayout.SPACING+sz*fineUnit+fineRng.nextInt(fineUnit);
                                var sample=ReefProvinceLayout.sample(seed,x,z);
                                if(sample.zone()==ReefProvinceLayout.Zone.OCEAN)continue;
                                fineInside++;
                                boolean wet=source.delegate().getNoiseBiome(QuartPos.fromBlock(x),8,QuartPos.fromBlock(z),sampler).is(BiomeTags.IS_OCEAN);
                                if(wet)fineOcean++;
                                if(sample.zone()==ReefProvinceLayout.Zone.CATHEDRAL) {fineCore++;if(wet)fineCoreOcean++;}
                            }
                        }
                        row.put("fine_strata",fineSide*fineSide);row.put("fine_province",fineInside);
                        row.put("fine_province_ocean",fineOcean);row.put("fine_cathedral",fineCore);
                        row.put("fine_cathedral_ocean",fineCoreOcean);cells.add(row);
                    }
                    Map<String,Object> result=new LinkedHashMap<>();result.put("seed",Long.toString(seed));
                    var spawn=sampler.findSpawnPosition();
                    result.put("spawn_hint_x",spawn.getX());result.put("spawn_hint_z",spawn.getZ());
                    result.put("spacing",ReefProvinceLayout.SPACING);result.put("jitter",ReefProvinceLayout.JITTER);
                    result.put("cells",cells);seeds.add(result);
                    Files.writeString(Path.of("coverage.json"),new GsonBuilder().setPrettyPrinting().create().toJson(seeds));
                    System.out.println("COVERAGE_SEED_COMPLETE seed="+seed+" cells="+cells.size());
                }
                Files.writeString(Path.of("passed.txt"),"Native placement and ocean-area survey complete\n");
                System.out.println("COVERAGE_COMPLETE");
            } catch(Throwable t) {
                t.printStackTrace();
                try {Files.writeString(Path.of("failure.txt"),t.toString());}catch(Exception ignored){}
            } finally {server.execute(()->server.halt(false));}
        },"Province ocean coverage survey").start();
    }
}
