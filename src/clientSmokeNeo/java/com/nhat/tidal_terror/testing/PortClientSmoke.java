package com.nhat.tidal_terror.testing;

/** Opt-in title-screen check after the native resource reload and renderer setup. */
@net.neoforged.fml.common.EventBusSubscriber(modid="tidalterror", value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class PortClientSmoke {
    private static boolean done;
    @net.neoforged.bus.api.SubscribeEvent
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        var game=net.minecraft.client.Minecraft.getInstance();
        if(done || game.screen == null || game.getOverlay()!=null) return;
        done=true;
        var models=game.getEntityModels();
        models.bakeLayer(com.nhat.tidal_terror.entities.coral_crusher.ModModelLayers.CORAL_CRUSHER_LAYER);
        models.bakeLayer(com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayModel.LAYER);
        models.bakeLayer(com.nhat.tidal_terror.entities.shardback.ShardbackModel.LAYER);
        models.bakeLayer(com.nhat.tidal_terror.entities.veilglow.VeilglowModel.LAYER);
        models.bakeLayer(com.nhat.tidal_terror.client.ReefSpearModel.LAYER);
        for(var slot:new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET})
            models.bakeLayer(com.nhat.tidal_terror.client.ReefArmorModel.layer(slot));
        org.slf4j.LoggerFactory.getLogger("TidalPortSmoke").info("TIDAL_PORT_CLIENT_READY: native resources and nine model layers loaded");
        game.stop();
    }
}
