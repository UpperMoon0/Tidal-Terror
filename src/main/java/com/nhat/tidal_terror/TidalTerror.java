package com.nhat.tidal_terror;

import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.items.CoralCrusherSpawnEggItem;
import com.nhat.tidal_terror.items.ModFoods;
import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(TidalTerror.MODID)
public class TidalTerror {
    public static final String MODID = "tidalterror";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final RegistryObject<CoralCrusherSpawnEggItem> CORAL_CRUSHER_SPAWN_EGG =
            ITEMS.register("coral_crusher_spawn_egg", CoralCrusherSpawnEggItem::new);
    public static final RegistryObject<com.nhat.tidal_terror.items.CathedralRaySpawnEggItem> CATHEDRAL_RAY_SPAWN_EGG =
            ITEMS.register("cathedral_ray_spawn_egg", com.nhat.tidal_terror.items.CathedralRaySpawnEggItem::new);
    public static final RegistryObject<com.nhat.tidal_terror.items.VeilglowSpawnEggItem> VEILGLOW_SPAWN_EGG =
            ITEMS.register("veilglow_spawn_egg", com.nhat.tidal_terror.items.VeilglowSpawnEggItem::new);
    public static final RegistryObject<com.nhat.tidal_terror.items.ShardbackSpawnEggItem> SHARDBACK_SPAWN_EGG =
            ITEMS.register("shardback_spawn_egg", com.nhat.tidal_terror.items.ShardbackSpawnEggItem::new);
    public static final RegistryObject<CreativeModeTab> TIDAL_TERROR_TAB =
            CREATIVE_MODE_TABS.register("tidal_terror", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tidalterror"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
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
                    })
                    .build());

    public TidalTerror() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModFoods.register();
        ITEMS.register(bus);
        CREATIVE_MODE_TABS.register(bus);
        ModEntities.register(bus);
        ReefWorldgen.register(bus);
    }
}
