package com.nhat.tidal_terror.worldgen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Fresh normal preset and native chunk generation, with TerraBlender absent. */
@Mod.EventBusSubscriber(modid = "tidalterror")
public final class ProvinceNativeAudit {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static int scaled(int distance) { return (int)Math.round(distance * ReefProvinceLayout.SCALE); }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        var server = event.getServer(); var level = server.overworld();
        try {
            require(!ModList.get().isLoaded("terrablender"), "TerraBlender still required");
            var generator = level.getChunkSource().getGenerator();
            require(generator.getBiomeSource() instanceof ReefProvinceBiomeSource, "Prototype preset did not load");
            var source = (ReefProvinceBiomeSource)generator.getBiomeSource();
            var sampler = level.getChunkSource().randomState().sampler();
            require(ProvinceSeeds.get(sampler) != null && ProvinceSeeds.get(sampler) == level.getSeed(), "Wrong sampler seed");
            FeatureSorter.buildFeaturesPerStep(List.copyOf(source.possibleBiomes()), b -> b.value().getGenerationSettings().features(), true);
            // Registry-aware codec serialization must preserve the delegated source and both biomes.
            var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, level.registryAccess());
            var encoded = net.minecraft.world.level.biome.BiomeSource.CODEC.encodeStart(ops, source).getOrThrow(false, m -> {});
            var decoded = net.minecraft.world.level.biome.BiomeSource.CODEC.parse(ops, encoded).getOrThrow(false, m -> {});
            require(decoded instanceof ReefProvinceBiomeSource, "Biome source codec round trip failed");
            int oceanCenters = 0;
            for (int gx=-10; gx<=10; gx++) for(int gz=-10; gz<=10; gz++) {
                var c=ReefProvinceLayout.center(level.getSeed(),gx,gz);
                if(source.delegate().getNoiseBiome(QuartPos.fromBlock(c.x()),8,QuartPos.fromBlock(c.z()),sampler).is(net.minecraft.tags.BiomeTags.IS_OCEAN)) oceanCenters++;
            }
            System.out.println("PROVINCE_AUDIT oceanCenters="+oceanCenters+"/441");
            ReefProvinceLayout.Center center = null;
            outer: for (int r = 0; r <= 30; r++) for (int gx = -r; gx <= r; gx++) for (int gz = -r; gz <= r; gz++) {
                if (Math.max(Math.abs(gx), Math.abs(gz)) != r) continue;
                var c = ReefProvinceLayout.center(level.getSeed(), gx, gz);
                if (source.province(level.getSeed(), c.x(), c.z(), sampler) != null) { center = c; break outer; }
            }
            require(center != null, "No sampled ocean province in bounded search");
            System.out.println("PROVINCE_AUDIT center=" + center);
            int[] offsets = {0, scaled(230), scaled(340), scaled(500), scaled(800)};
            var expected = new ReefProvinceLayout.Zone[]{ReefProvinceLayout.Zone.CATHEDRAL, ReefProvinceLayout.Zone.RIM,
                    ReefProvinceLayout.Zone.INNER_WASTES, ReefProvinceLayout.Zone.OUTER_WASTES, ReefProvinceLayout.Zone.OCEAN};
            var terrain = new ReefTerrain(level, generator);
            for (int i = 0; i < offsets.length; i++) {
                int x = center.x() + offsets[i], z = center.z();
                var s = ReefProvinceLayout.sample(level.getSeed(), x, z);
                require(s.zone() == expected[i], "Unexpected transition at " + offsets[i]);
                var biome = source.getNoiseBiome(QuartPos.fromBlock(x), 8, QuartPos.fromBlock(z), sampler);
                require(biome.equals(decoded.getNoiseBiome(QuartPos.fromBlock(x), 8, QuartPos.fromBlock(z), sampler)), "Reload source disagrees");
                if (i == offsets.length - 1) { require(!biome.is(ReefWorldgen.BIOME) && !biome.is(ReefWorldgen.WASTES), "Province leaked outside ring"); continue; }
                require(biome.is(i == 0 ? ReefWorldgen.BIOME : ReefWorldgen.WASTES), "Zone biome mismatch");
                var chunk = level.getChunk(x >> 4, z >> 4);
                int floor = terrain.floor(x, z);
                require(chunk.getBlockState(new BlockPos(x, floor, z)).is(Blocks.SAND), "Generated floor disagrees with province model");
                require(level.getBiome(new BlockPos(x, floor + 5, z)).is(i == 0 ? ReefWorldgen.BIOME : ReefWorldgen.WASTES), "Stored flooded biome mismatch");
                int air = 0;
                for (int y = floor + 1; y < level.getSeaLevel(); y++)
                    if (chunk.getBlockState(new BlockPos(x, y, z)).isAir()) air++;
                require(air == 0, "Air gap in flooded column");
                System.out.println("PROVINCE_AUDIT " + expected[i] + " floor=" + floor + " biome=" + biome.unwrapKey());
            }
            var point = new BlockPos(center.x(), 58, center.z());
            // Province admission allows coastal land: prove a native above-sea
            // column is removed, rather than left floating over the excavated water.
            boolean coastal = false;
            outer: for (int r=1600; r<3000; r+=256) for(int angle=0; angle<360; angle+=15) {
                int x=center.x()+(int)(Math.cos(Math.toRadians(angle))*r);
                int z=center.z()+(int)(Math.sin(Math.toRadians(angle))*r);
                int nativeTop=generator.getBaseHeight(x,z,net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                        level,level.getChunkSource().randomState());
                if(nativeTop<=level.getSeaLevel()+4 || terrain.floor(x,z)>=level.getSeaLevel())continue;
                var chunk=level.getChunk(x>>4,z>>4);
                for(int y=level.getSeaLevel();y<nativeTop;y++)
                    require(chunk.getBlockState(new BlockPos(x,y,z)).isAir(), "Floating native coastal surface at "+x+","+y+","+z);
                coastal=true;
                System.out.println("PROVINCE_AUDIT COASTAL_SURFACE PASS x="+x+" z="+z+" originalTop="+nativeTop);
                break outer;
            }
            require(coastal,"Fixture did not exercise coastal excavation");
            require(!ProvinceSpawnRules.allowed(level, point, true), "Surface predator allowed");
            require(!ProvinceSpawnRules.allowed(level, new BlockPos(center.x()+scaled(500), 0, center.z()), true), "Outer Wastes predator allowed");
            require(ProvinceSpawnRules.allowed(level, new BlockPos(center.x(), -20, center.z()), true), "Deep core predator rejected");
            Files.writeString(Path.of("passed.txt"), "Preset, no TerraBlender, seed, codec, feature graph, five zones, generated floors/water/biomes, spawn depth passed\n");
            System.out.println("PROVINCE_AUDIT PASSED");
        } catch (Throwable error) {
            com.mojang.logging.LogUtils.getLogger().error("PROVINCE_AUDIT FAILED", error);
            try { Files.writeString(Path.of("failure.txt"), error.toString()); } catch (Exception ignored) {}
        }
        finally { server.halt(false); }
    }
}
