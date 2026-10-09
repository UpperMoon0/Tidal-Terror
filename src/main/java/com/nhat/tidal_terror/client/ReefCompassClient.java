package com.nhat.tidal_terror.client;

import com.nhat.tidal_terror.items.ModEquipment;
import net.minecraft.client.renderer.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CompassItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid="tidalterror",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ReefCompassClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(()->ItemProperties.register(ModEquipment.REEF_COMPASS.get(),new ResourceLocation("angle"),
            new CompassItemPropertyFunction((level,stack,entity)->com.nhat.tidal_terror.items.ReefCompassItem.target(stack))));
    }
}
