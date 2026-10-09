package com.nhat.tidal_terror.worldgen;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.entities.ModEntities;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
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
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.List;
import java.util.Set;

public final class ReefWorldgen {
    public static final ResourceKey<Biome> WASTES = ResourceKey.create(Registries.BIOME, id("sunken_wastes"));
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, id("coral_cathedral"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE, id("giant_coral"));
    public static final ResourceKey<PlacedFeature> PLACED = ResourceKey.create(Registries.PLACED_FEATURE, id("giant_coral"));
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, TidalTerror.MODID);
    public static final RegistryObject<CoralCathedralFeature> GIANT_CORAL = FEATURES.register("giant_coral", CoralCathedralFeature::new);

    public static final RegistryObject<ReefBasinFeature> BASIN = FEATURES.register("reef_basin", ReefBasinFeature::new);
    public static final RegistryObject<ReefGardenFeature> GARDEN = FEATURES.register("reef_garden", ReefGardenFeature::new);
    public static final ResourceKey<ConfiguredFeature<?, ?>> BASIN_CONFIG = ResourceKey.create(Registries.CONFIGURED_FEATURE, id("reef_basin"));
    public static final ResourceKey<PlacedFeature> BASIN_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, id("reef_basin"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> GARDEN_CONFIG = ResourceKey.create(Registries.CONFIGURED_FEATURE, id("reef_garden"));
    public static final ResourceKey<PlacedFeature> GARDEN_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, id("reef_garden"));
    private static final DeferredRegister<com.mojang.serialization.Codec<? extends BiomeSource>> PROVINCE_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, TidalTerror.MODID);
    static { PROVINCE_SOURCES.register("reef_province", () -> ReefProvinceBiomeSource.CODEC); }

    public static final RegistryObject<SunkenWastesFeature> WASTES_FEATURE = FEATURES.register("wastes_landmarks", SunkenWastesFeature::new);
    public static final ResourceKey<ConfiguredFeature<?, ?>> WASTES_CONFIG = ResourceKey.create(Registries.CONFIGURED_FEATURE, id("wastes_landmarks"));
    public static final ResourceKey<PlacedFeature> WASTES_PLACED = ResourceKey.create(Registries.PLACED_FEATURE, id("wastes_landmarks"));
    private static ResourceLocation id(String path) { return new ResourceLocation(TidalTerror.MODID, path); }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
        PROVINCE_SOURCES.register(bus);
        bus.addListener(ReefWorldgen::data);
        bus.addListener(ReefWorldgen::spawns);
        bus.addListener(ReefAnimalSpawns::register);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            if (!ReefProvinceMode.enabled()) LegacyReefIntegration.register();
        }));
    }

    private static void spawns(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.SHARDBACK.get(), SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.OCEAN_FLOOR, (type, level, reason, pos, random) ->
                    (level.getBiome(pos).is(BIOME) || level.getBiome(pos).is(WASTES)) && pos.getY()<level.getSeaLevel()-4
                    && ReefSpawnHabitat.allowed(level.getLevel(),pos)
                    && level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WATER)
                    && level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.WATER)
                    && com.nhat.tidal_terror.entities.shardback.ShardbackEntity.isSeabed(level.getBlockState(pos.below()))
                    && level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.VEILGLOW.get(), SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> {
                    if(!level.getBiome(pos).is(BIOME) || pos.getY()>=level.getSeaLevel()-8 || !ProvinceSpawnRules.allowed(level,pos,false))return false;
                    // The tall bell and hanging ribbons need an entirely submerged column.
                    for(int y=-1;y<=3;y++)if(!level.getBlockState(pos.above(y)).is(net.minecraft.world.level.block.Blocks.WATER))return false;
                    return true;
                }, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.CATHEDRAL_RAY.get(), SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) ->
                    level.getBiome(pos).is(BIOME) && ProvinceSpawnRules.allowed(level,pos,false) && pos.getY() < level.getSeaLevel()-4
                    && level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WATER)
                    && level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.WATER)
                    && level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.WATER),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.CORAL_CRUSHER.get(), SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> {
                    if (!(level.getBiome(pos).is(BIOME) || level.getBiome(pos).is(WASTES)) || pos.getY() >= level.getSeaLevel()-4) return false;
                    if (!ProvinceSpawnRules.allowed(level, pos, true)) return false;
                    // Unlike WaterAnimal's surface-only rule, reef sharks can use deep water.
                    // NaturalSpawner subsequently checks the entity's actual 2x1 collision box.
                    return level.getFluidState(pos).is(FluidTags.WATER)
                            && level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.WATER)
                            && level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.WATER);
                }, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private static void data(GatherDataEvent event) {
        RegistrySetBuilder builder = new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, context -> {
                    if (ReefProvinceMode.enabled()) context.register(WASTES_CONFIG, new ConfiguredFeature<>(WASTES_FEATURE.get(), NoneFeatureConfiguration.INSTANCE));
                    context.register(CONFIGURED, new ConfiguredFeature<>(GIANT_CORAL.get(), NoneFeatureConfiguration.INSTANCE));
                    context.register(BASIN_CONFIG, new ConfiguredFeature<>(BASIN.get(), NoneFeatureConfiguration.INSTANCE));
                    context.register(GARDEN_CONFIG, new ConfiguredFeature<>(GARDEN.get(), NoneFeatureConfiguration.INSTANCE));
                })
                .add(Registries.PLACED_FEATURE, context -> {
                    // No random offset: each feature owns the decorating chunk, including its edges.
                    var configured=context.lookup(Registries.CONFIGURED_FEATURE);
                    if (ReefProvinceMode.enabled()) context.register(WASTES_PLACED,new PlacedFeature(configured.getOrThrow(WASTES_CONFIG),List.of()));
                    context.register(PLACED,new PlacedFeature(configured.getOrThrow(CONFIGURED),List.of()));
                    context.register(BASIN_PLACED,new PlacedFeature(configured.getOrThrow(BASIN_CONFIG),List.of()));
                    context.register(GARDEN_PLACED,new PlacedFeature(configured.getOrThrow(GARDEN_CONFIG),List.of()));
                })
                .add(Registries.BIOME, ReefWorldgen::biome);
        event.getGenerator().addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(
                event.getGenerator().getPackOutput(), event.getLookupProvider(), builder, Set.of(TidalTerror.MODID)));
    }

    private static void biome(BootstapContext<Biome> context) {
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
        if (ReefProvinceMode.enabled()) {
            BiomeGenerationSettings.Builder wastesGeneration = new BiomeGenerationSettings.Builder(placed, carvers);
            wastesGeneration.addFeature(GenerationStep.Decoration.RAW_GENERATION, BASIN_PLACED);
            wastesGeneration.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, PLACED);
            wastesGeneration.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, WASTES_PLACED);
            MobSpawnSettings.Builder wastesMobs = new MobSpawnSettings.Builder();
            wastesMobs.addSpawn(MobCategory.WATER_AMBIENT, new MobSpawnSettings.SpawnerData(EntityType.COD, 3, 1, 2));
            wastesMobs.addSpawn(ModEntities.CRUSHER_POOL, new MobSpawnSettings.SpawnerData(ModEntities.CORAL_CRUSHER.get(), 1, 1, 1));
            wastesMobs.addSpawn(ModEntities.SHARDBACK_POOL, new MobSpawnSettings.SpawnerData(ModEntities.SHARDBACK.get(), 2, 1, 1));
            context.register(WASTES, new Biome.BiomeBuilder().hasPrecipitation(true).temperature(.8F).downfall(.5F)
                    .specialEffects(new BiomeSpecialEffects.Builder().waterColor(0x648f91).waterFogColor(0x304f60)
                            .fogColor(0xc0d8ff).skyColor(0x78a7ff).build())
                    .mobSpawnSettings(wastesMobs.build()).generationSettings(wastesGeneration.build()).build());
        }
        context.register(BIOME, new Biome.BiomeBuilder().hasPrecipitation(true).temperature(.95F).downfall(.8F)
                .specialEffects(new BiomeSpecialEffects.Builder().waterColor(0x35bdb2).waterFogColor(0x126b82)
                        .fogColor(0xc0d8ff).skyColor(0x78a7ff).build())
                .mobSpawnSettings(mobs.build()).generationSettings(generation.build()).build());
    }
}
