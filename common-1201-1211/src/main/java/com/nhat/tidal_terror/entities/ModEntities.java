package com.nhat.tidal_terror.entities;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;

public class ModEntities {
    // Forge extends the native category enum, so global and local caps count
    // each reef species independently without bypassing natural spawning.
    public static final MobCategory CRUSHER_POOL = com.nhat.tidal_terror.platform.ReefMobCategories.create("TIDALTERROR_CRUSHER", "tidalterror_crusher", 2, true, false, 128);
    public static final MobCategory RAY_POOL = com.nhat.tidal_terror.platform.ReefMobCategories.create("TIDALTERROR_RAY", "tidalterror_ray", 8, true, false, 128);
    public static final MobCategory VEILGLOW_POOL = com.nhat.tidal_terror.platform.ReefMobCategories.create("TIDALTERROR_VEILGLOW", "tidalterror_veilglow", 12, true, false, 128);
    public static final MobCategory SHARDBACK_POOL = com.nhat.tidal_terror.platform.ReefMobCategories.create("TIDALTERROR_SHARDBACK", "tidalterror_shardback", 10, true, false, 128);
    public static java.util.List<MobCategory> reefPools() {
        return java.util.List.of(CRUSHER_POOL, RAY_POOL, VEILGLOW_POOL, SHARDBACK_POOL);
    }
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(TidalTerror.MODID, net.minecraft.core.registries.Registries.ENTITY_TYPE);
    public static final RegistrySupplier<EntityType<com.nhat.tidal_terror.entities.shardback.ShardbackEntity>> SHARDBACK =
            ENTITY_TYPES.register("shardback", () -> EntityType.Builder.of(
                    com.nhat.tidal_terror.entities.shardback.ShardbackEntity::new, SHARDBACK_POOL)
                    .sized(2f, 1.2f).build("shardback"));

    public static final RegistrySupplier<EntityType<CoralCrusherEntity>> CORAL_CRUSHER =
            ENTITY_TYPES.register("coral_crusher", () -> EntityType.Builder.of(CoralCrusherEntity::new ,CRUSHER_POOL)
                    .sized(2f, 1f).build("coral_crusher"));

    public static final RegistrySupplier<EntityType<com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayEntity>> CATHEDRAL_RAY =
            ENTITY_TYPES.register("cathedral_ray", () -> EntityType.Builder.of(
                    com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayEntity::new, RAY_POOL)
                    .sized(4.75f, 0.5f).build("cathedral_ray"));

    public static final RegistrySupplier<EntityType<FangArrowEntity>> FANG_ARROW =
            ENTITY_TYPES.register("fang_arrow", () -> EntityType.Builder.<FangArrowEntity>of(FangArrowEntity::new, MobCategory.MISC)
                    .sized(.5F, .5F).clientTrackingRange(4).updateInterval(20).build("fang_arrow"));

    public static void register() {
        ENTITY_TYPES.register();
    }

    public static final RegistrySupplier<EntityType<com.nhat.tidal_terror.entities.veilglow.VeilglowEntity>> VEILGLOW =
            ENTITY_TYPES.register("veilglow", () -> EntityType.Builder.of(
                    com.nhat.tidal_terror.entities.veilglow.VeilglowEntity::new, VEILGLOW_POOL)
                    .sized(1.5f, 2.4f).build("veilglow"));
}
