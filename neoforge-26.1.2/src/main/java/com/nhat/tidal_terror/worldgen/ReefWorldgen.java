package com.nhat.tidal_terror.worldgen;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.entities.ModEntities;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(TidalTerror.MODID, path); }

    public static final ResourceKey<Biome> WASTES = ResourceKey.create(Registries.BIOME, id("sunken_wastes"));
    public static final RegistrySupplier<SunkenWastesFeature> WASTES_FEATURE = FEATURES.register("wastes_landmarks", SunkenWastesFeature::new);
    private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends BiomeSource>> PROVINCE_SOURCES =
            DeferredRegister.create(TidalTerror.MODID, Registries.BIOME_SOURCE);
    static { PROVINCE_SOURCES.register("reef_province", () -> ReefProvinceBiomeSource.CODEC); }

    public static void register() {
        FEATURES.register();
        PROVINCE_SOURCES.register();
    }


}
