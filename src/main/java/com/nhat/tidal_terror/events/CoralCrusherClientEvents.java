package com.nhat.tidal_terror.events;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherModel;
import com.nhat.tidal_terror.entities.coral_crusher.ModModelLayers;
import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TidalTerror.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CoralCrusherClientEvents {
    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CORAL_CRUSHER.get(), CoralCrusherRenderer::new);
    }
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.CORAL_CRUSHER_LAYER, CoralCrusherModel::createBodyLayer);
    }
}
