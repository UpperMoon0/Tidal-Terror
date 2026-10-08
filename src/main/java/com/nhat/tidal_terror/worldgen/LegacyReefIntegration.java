package com.nhat.tidal_terror.worldgen;
import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.SurfaceRules;
import terrablender.api.*;
/** Loaded only by the released TerraBlender generation path. */
final class LegacyReefIntegration {
    private static ResourceLocation id(String path) { return new ResourceLocation(TidalTerror.MODID, path); }
    private static final ResourceKey<Biome> BIOME = ReefWorldgen.BIOME;
    static void register() {
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, TidalTerror.MODID,
                    SurfaceRules.ifTrue(SurfaceRules.isBiome(BIOME), SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(net.minecraft.world.level.block.Blocks.SAND.defaultBlockState())),
                            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.state(net.minecraft.world.level.block.Blocks.SAND.defaultBlockState())))));
            Regions.register(new Region(id("reefs"), RegionType.OVERWORLD, com.nhat.tidal_terror.balance.ReefBalance.REEF_REGION_WEIGHT) {
                @Override public void addBiomes(net.minecraft.core.Registry<Biome> registry,
                        java.util.function.Consumer<com.mojang.datafixers.util.Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
                    addModifiedVanillaOverworldBiomes(mapper, builder -> {
                        builder.replaceBiome(Biomes.WARM_OCEAN, BIOME);
                        builder.replaceBiome(Biomes.LUKEWARM_OCEAN, BIOME);
                        builder.replaceBiome(Biomes.OCEAN, BIOME);
                        builder.replaceBiome(Biomes.DEEP_OCEAN, BIOME);
                        builder.replaceBiome(Biomes.DEEP_LUKEWARM_OCEAN, BIOME);
                    });
                }
            });
    }
}
