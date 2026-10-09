package com.nhat.tidal_terror.worldgen;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.entities.ModEntities;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.Set;

public final class ReefWorldgen {
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, id("coral_cathedral"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE, id("giant_coral"));
    public static final ResourceKey<PlacedFeature> PLACED = ResourceKey.create(Registries.PLACED_FEATURE, id("giant_coral"));
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(TidalTerror.MODID, net.minecraft.core.registries.Registries.FEATURE);
    public static final RegistrySupplier<CoralCathedralFeature> GIANT_CORAL = FEATURES.register("giant_coral", CoralCathedralFeature::new);

    public static final RegistrySupplier<ReefBasinFeature> BASIN = FEATURES.register("reef_basin", ReefBasinFeature::new);
    public static final RegistrySupplier<ReefGardenFeature> GARDEN = FEATURES.register("reef_garden", ReefGardenFeature::new);
    public static final ResourceKey<ConfiguredFeature<?, ?>> BASIN_CONFIG = ResourceKey.create(Registries.CONFIGURED_FEATURE, id("reef_basin"));
    public static final ResourceKey<PlacedFeature> BASIN_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, id("reef_basin"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> GARDEN_CONFIG = ResourceKey.create(Registries.CONFIGURED_FEATURE, id("reef_garden"));
    public static final ResourceKey<PlacedFeature> GARDEN_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, id("reef_garden"));
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(TidalTerror.MODID, path); }

    public static final ResourceKey<Biome> WASTES = ResourceKey.create(Registries.BIOME, id("sunken_wastes"));
    public static final RegistrySupplier<SunkenWastesFeature> WASTES_FEATURE = FEATURES.register("wastes_landmarks", SunkenWastesFeature::new);
    private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends BiomeSource>> PROVINCE_SOURCES =
            DeferredRegister.create(TidalTerror.MODID, Registries.BIOME_SOURCE);
    static { PROVINCE_SOURCES.register("reef_province", () -> ReefProvinceBiomeSource.CODEC); }

    public static void register() {
        FEATURES.register();
        PROVINCE_SOURCES.register();
    }


    private static void biome(BootstrapContext<Biome> context) {
        var placed = context.lookup(Registries.PLACED_FEATURE);
        var carvers = context.lookup(Registries.CONFIGURED_CARVER);
        Biome warm = OverworldBiomes.warmOcean(placed, carvers);
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placed, carvers);
        for (GenerationStep.Carving step : GenerationStep.Carving.values())
            warm.getGenerationSettings().getCarvers(step).forEach(holder -> generation.addCarver(step, holder));
        var vanillaFeatures = warm.getGenerationSettings().features();
        for (int step = 0; step < vanillaFeatures.size(); step++)
            for (var holder : vanillaFeatures.get(step)) {
                String feature=holder.unwrapKey().map(key->key.location().getPath()).orElse("");
                // Geodes would hang in the newly excavated water. Seabed colonies are
                // placed by our chunk-confined garden instead of spilling across basins.
                // Native magma creates bubble columns. Waterlogged glow lichen can
                // lose its neighbour-chunk support during excavation and return AIR
                // from MultifaceBlock.updateShape during native chunk postprocessing.
                if (Set.of("amethyst_geode","warm_ocean_vegetation","kelp_warm","underwater_magma","glow_lichen","seagrass_warm","sea_pickle").contains(feature)) continue;
                generation.addFeature(step, holder);
            }
        generation.addFeature(GenerationStep.Decoration.RAW_GENERATION, BASIN_PLACED);
        generation.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, PLACED);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, GARDEN_PLACED);
        MobSpawnSettings.Builder oceanMobs = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.warmOceanSpawns(oceanMobs, 10, 4);
        MobSpawnSettings oceanSpawns = oceanMobs.build();
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        // Keep the warm-ocean roster, but make drowned uncommon in this reef.
        // This changes only our biome table, not vanilla drowned placement rules.
        for (MobCategory category : MobCategory.values()) {
            for (MobSpawnSettings.SpawnerData spawn : oceanSpawns.getMobs(category).unwrap()) {
                mobs.addSpawn(category, spawn.type == EntityType.DROWNED
                        ? new MobSpawnSettings.SpawnerData(EntityType.DROWNED, 1, 1, 1) : spawn);
            }
        }
        mobs.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.TURTLE, 3, 1, 2));
        mobs.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.PUFFERFISH, 15, 1, 3));
        mobs.addSpawn(ModEntities.CRUSHER_POOL, new MobSpawnSettings.SpawnerData(ModEntities.CORAL_CRUSHER.get(), 2, 1, 1));
        mobs.addSpawn(ModEntities.RAY_POOL, new MobSpawnSettings.SpawnerData(ModEntities.CATHEDRAL_RAY.get(), 6, 2, 3));
        mobs.addSpawn(ModEntities.VEILGLOW_POOL, new MobSpawnSettings.SpawnerData(ModEntities.VEILGLOW.get(), 8, 2, 4));
        mobs.addSpawn(ModEntities.SHARDBACK_POOL, new MobSpawnSettings.SpawnerData(ModEntities.SHARDBACK.get(), 10, 1, 3));
        context.register(BIOME, new Biome.BiomeBuilder().hasPrecipitation(true).temperature(.95F).downfall(.8F)
                .specialEffects(new BiomeSpecialEffects.Builder().waterColor(0x35bdb2).waterFogColor(0x126b82)
                        .fogColor(0xc0d8ff).skyColor(0x78a7ff).build())
                .mobSpawnSettings(mobs.build()).generationSettings(generation.build()).build());
    }
}
