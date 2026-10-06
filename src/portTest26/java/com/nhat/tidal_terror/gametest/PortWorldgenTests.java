package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

public final class PortWorldgenTests {
    public static void normalOverworldFeatureOrder(GameTestHelper helper) {
        // GameTest's own flat generator cannot exercise the normal biome graph.
        var preset = helper.getLevel().registryAccess().lookupOrThrow(Registries.WORLD_PRESET)
                .getOrThrow(WorldPresets.NORMAL).value();
        var generator = preset.overworld().orElseThrow().generator();
        helper.assertTrue(generator instanceof NoiseBasedChunkGenerator, "Expected normal noise generation");
        // A normal preset outside an active normal world has not received TerraBlender's
        // runtime region injection. Include the loaded reef explicitly so flat test
        // worlds cannot silently omit the biome that caused the client crash.
        var biomes = new java.util.ArrayList<>(generator.getBiomeSource().possibleBiomes());
        var reef = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME)
                .getOrThrow(ReefWorldgen.BIOME);
        if (biomes.stream().noneMatch(b -> b.is(ReefWorldgen.BIOME))) biomes.add(reef);
        net.minecraft.world.level.biome.FeatureSorter.buildFeaturesPerStep(
                biomes, biome -> biome.value().getGenerationSettings().features(), true);
        // Also exercise the normal generator's own validation entry point.
        generator.validate();
        helper.succeed();
    }

    private PortWorldgenTests() {}
}
