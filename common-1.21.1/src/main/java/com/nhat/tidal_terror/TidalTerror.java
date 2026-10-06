package com.nhat.tidal_terror;

import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.items.CoralCrusherSpawnEggItem;
import com.nhat.tidal_terror.items.ModFoods;
import com.nhat.tidal_terror.items.ModEquipment;
import com.nhat.tidal_terror.effects.ModEffects;
import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;


public class TidalTerror {
    public static final String MODID = "tidalterror";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, net.minecraft.core.registries.Registries.ITEM);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(MODID, Registries.CREATIVE_MODE_TAB);
    public static final RegistrySupplier<CoralCrusherSpawnEggItem> CORAL_CRUSHER_SPAWN_EGG =
            ITEMS.register("coral_crusher_spawn_egg", CoralCrusherSpawnEggItem::new);
    public static final RegistrySupplier<com.nhat.tidal_terror.items.CathedralRaySpawnEggItem> CATHEDRAL_RAY_SPAWN_EGG =
            ITEMS.register("cathedral_ray_spawn_egg", com.nhat.tidal_terror.items.CathedralRaySpawnEggItem::new);
    public static final RegistrySupplier<com.nhat.tidal_terror.items.VeilglowSpawnEggItem> VEILGLOW_SPAWN_EGG =
            ITEMS.register("veilglow_spawn_egg", com.nhat.tidal_terror.items.VeilglowSpawnEggItem::new);
    public static final RegistrySupplier<com.nhat.tidal_terror.items.ShardbackSpawnEggItem> SHARDBACK_SPAWN_EGG =
            ITEMS.register("shardback_spawn_egg", com.nhat.tidal_terror.items.ShardbackSpawnEggItem::new);
    public static final RegistrySupplier<CreativeModeTab> TIDAL_TERROR_TAB =
            CREATIVE_MODE_TABS.register("tidal_terror", () -> dev.architectury.registry.CreativeTabRegistry.create(builder -> builder
                    .title(Component.translatable("itemGroup.tidalterror"))

                    .icon(() -> CORAL_CRUSHER_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(CORAL_CRUSHER_SPAWN_EGG.get());
                        output.accept(CATHEDRAL_RAY_SPAWN_EGG.get());
                        output.accept(VEILGLOW_SPAWN_EGG.get());
                        output.accept(SHARDBACK_SPAWN_EGG.get());
                        output.accept(ModFoods.RAW_CORAL_CRUSHER_STEAK.get());
                        output.accept(ModFoods.COOKED_CORAL_CRUSHER_STEAK.get());
                        output.accept(ModFoods.RAW_CATHEDRAL_RAY_WING.get());
                        output.accept(ModFoods.COOKED_CATHEDRAL_RAY_WING.get());
                        output.accept(ModFoods.RAW_VEILGLOW_GEL.get());
                        output.accept(ModFoods.COOKED_VEILGLOW_GEL.get());
                        output.accept(ModFoods.RAW_SHARDBACK_CLAW.get());
                        output.accept(ModFoods.COOKED_SHARDBACK_CLAW.get());
                        output.accept(ModEquipment.CRUSHER_TOOTH.get());
                        output.accept(ModEquipment.SHARDBACK_PLATE.get());
                        output.accept(ModEquipment.FANG_ARROW.get());
                        output.accept(ModEquipment.REEF_SPEAR.get());
                        com.nhat.tidal_terror.enchantments.ModEnchantments.addBooks(parameters.holders(),output);
                        output.accept(ModEquipment.REEF_HELMET.get());
                        output.accept(ModEquipment.REEF_CHESTPLATE.get());
                        output.accept(ModEquipment.REEF_LEGGINGS.get());
                        output.accept(ModEquipment.REEF_BOOTS.get());
                    })
                    ));

    public TidalTerror() {
        ModFoods.register();
        ModEquipment.register();
        ModEffects.register();
        com.nhat.tidal_terror.recipes.ModRecipes.register();
        com.nhat.tidal_terror.particles.ModParticles.register();
        ITEMS.register();
        CREATIVE_MODE_TABS.register();
        ModEntities.register();
        ReefWorldgen.register();
    }
}
