package com.nhat.tidal_terror.fabric;

import com.nhat.tidal_terror.client.*;
import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.items.ModEquipment;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;

public final class TidalTerrorFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.CORAL_CRUSHER.get(),com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherRenderer::new);
        EntityRendererRegistry.register(ModEntities.CATHEDRAL_RAY.get(),com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayRenderer::new);
        EntityRendererRegistry.register(ModEntities.VEILGLOW.get(),com.nhat.tidal_terror.entities.veilglow.VeilglowRenderer::new);
        EntityRendererRegistry.register(ModEntities.SHARDBACK.get(),com.nhat.tidal_terror.entities.shardback.ShardbackRenderer::new);
        EntityRendererRegistry.register(ModEntities.FANG_ARROW.get(),FangArrowRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(com.nhat.tidal_terror.entities.coral_crusher.ModModelLayers.CORAL_CRUSHER_LAYER,com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayModel.LAYER,com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(com.nhat.tidal_terror.entities.veilglow.VeilglowModel.LAYER,com.nhat.tidal_terror.entities.veilglow.VeilglowModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(com.nhat.tidal_terror.entities.shardback.ShardbackModel.LAYER,com.nhat.tidal_terror.entities.shardback.ShardbackModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(ReefSpearModel.LAYER,ReefSpearModel::createLayer);
        for(var slot:new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET})
            EntityModelLayerRegistry.registerModelLayer(ReefArmorModel.layer(slot),()->ReefArmorModel.createLayer(slot));
        ParticleFactoryRegistry.getInstance().register(com.nhat.tidal_terror.particles.ModParticles.BLOOD.get(),BloodParticle.Provider::new);
        BuiltinItemRendererRegistry.INSTANCE.register(ModEquipment.REEF_SPEAR.get(),(stack,context,poses,buffers,light,overlay)->{
            var model=ReefModelCache.layer(ReefSpearModel.LAYER);
            poses.pushPose();poses.translate(.5,.5,.5);poses.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
            model.render(poses,ItemRenderer.getFoilBufferDirect(buffers,RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("tidalterror","textures/item/reef_spear_model.png")),false,stack.hasFoil()),light,overlay);poses.popPose();
        });
        ArmorRenderer.register((poses,buffers,stack,entity,slot,light,context)->
            ReefArmorTrimRenderer.renderArmor(poses,buffers,light,stack,entity.level().registryAccess(),slot,context),
            ModEquipment.REEF_HELMET.get(),ModEquipment.REEF_CHESTPLATE.get(),ModEquipment.REEF_LEGGINGS.get(),ModEquipment.REEF_BOOTS.get());
        ItemTooltipCallback.EVENT.register((stack,context,flags,lines)->{
            if(!stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK))return;

            int power=com.nhat.tidal_terror.enchantments.ModEnchantments.level(stack,com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION),duration=com.nhat.tidal_terror.enchantments.ModEnchantments.level(stack,com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE);
            if(power>0)lines.add(net.minecraft.network.chat.Component.translatable("tooltip.tidalterror.serration_book",.5F*power).withStyle(net.minecraft.ChatFormatting.AQUA));
            if(duration>0)lines.add(net.minecraft.network.chat.Component.translatable("tooltip.tidalterror.hemorrhage_book",2*duration).withStyle(net.minecraft.ChatFormatting.AQUA));
        });
    }
}
