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
    public static void registerParticles(net.minecraftforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(com.nhat.tidal_terror.particles.ModParticles.BLOOD.get(), com.nhat.tidal_terror.client.BloodParticle.Provider::new);
    }
    @SubscribeEvent
    public static void registerReload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                manager -> com.nhat.tidal_terror.client.ReefEquipmentClient.resetModels());
    }
    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHARDBACK.get(), com.nhat.tidal_terror.entities.shardback.ShardbackRenderer::new);
        event.registerEntityRenderer(ModEntities.CORAL_CRUSHER.get(), CoralCrusherRenderer::new);
        event.registerEntityRenderer(ModEntities.CATHEDRAL_RAY.get(), com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayRenderer::new);
        event.registerEntityRenderer(ModEntities.VEILGLOW.get(), com.nhat.tidal_terror.entities.veilglow.VeilglowRenderer::new);
    }
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.nhat.tidal_terror.client.ReefSpearModel.LAYER,
                com.nhat.tidal_terror.client.ReefSpearModel::createLayer);
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            event.registerLayerDefinition(com.nhat.tidal_terror.client.ReefArmorModel.layer(slot),
                    () -> com.nhat.tidal_terror.client.ReefArmorModel.createLayer(slot));
        }
        event.registerLayerDefinition(com.nhat.tidal_terror.entities.shardback.ShardbackModel.LAYER,
                com.nhat.tidal_terror.entities.shardback.ShardbackModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.CORAL_CRUSHER_LAYER, CoralCrusherModel::createBodyLayer);
        event.registerLayerDefinition(com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayModel.LAYER,
                com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayModel::createBodyLayer);
        event.registerLayerDefinition(com.nhat.tidal_terror.entities.veilglow.VeilglowModel.LAYER,
                com.nhat.tidal_terror.entities.veilglow.VeilglowModel::createBodyLayer);
    }
}
