package com.nhat.tidal_terror.worldgen;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.biome.*;
/** Native registry and serialization contract used by the opt-in cross-loader suite. */
public final class ProvincePresetAudit {
    public static void verify(GameTestHelper h) {
        var access=h.getLevel().registryAccess();
        var preset=access.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).value();
        var generator=preset.overworld().orElseThrow().generator();
        h.assertTrue(generator.getBiomeSource() instanceof ReefProvinceAccess province&&!province.deep(),"Normal world must use shallow provinces");
        var source=generator.getBiomeSource();
        h.assertTrue(source.possibleBiomes().stream().anyMatch(b->b.is(ReefWorldgen.WASTES)),"Normal source omitted Sunken Wastes");
        FeatureSorter.buildFeaturesPerStep(java.util.List.copyOf(source.possibleBiomes()),b->b.value().getGenerationSettings().features(),true);
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,access);
        var encoded=BiomeSource.CODEC.encodeStart(ops,source).result().orElseThrow();
        var decoded=BiomeSource.CODEC.parse(ops,encoded).result().orElseThrow();
        h.assertTrue(decoded instanceof ReefProvinceAccess province&&!province.deep(),"Default saved source lost province profile");
        h.assertTrue(encoded.getAsJsonObject().has("delegate")&&!encoded.getAsJsonObject().has("value"),"Default source dispatch shape changed");
        System.out.println("TIDAL_DEFAULT_PROVINCE: normal,preset,codec,full-biome-graph");
    }
    private ProvincePresetAudit(){}
}
