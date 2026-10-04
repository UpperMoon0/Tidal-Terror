package com.nhat.tidal_terror.entities;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TidalTerror.MODID);

    public static final RegistryObject<EntityType<CoralCrusherEntity>> CORAL_CRUSHER =
            ENTITY_TYPES.register("coral_crusher", () -> EntityType.Builder.of(CoralCrusherEntity::new ,MobCategory.WATER_CREATURE)
                    .sized(2f, 1f).build("coral_crusher"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
