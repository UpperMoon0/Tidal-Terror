package com.nhat.tidal_terror.gametest;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** An actual registered third-party-style bow, only present in the opt-in test mod. */
@Mod.EventBusSubscriber(modid="tidalterror", bus=Mod.EventBusSubscriber.Bus.MOD)
public final class TestBowRegistration {
    public static BowItem BOW;
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, new ResourceLocation("tidalterror", "fixture_bow"), () -> {
            BOW = new BowItem(new Item.Properties().durability(384)) {};
            return BOW;
        });
    }
}
