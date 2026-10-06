package com.nhat.tidal_terror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;

public final class ReefArmorTrimRenderer {
    public static final net.minecraft.resources.ResourceLocation BASE_TEXTURE=new net.minecraft.resources.ResourceLocation("tidalterror","textures/models/armor/reef.png");
    public static void renderArmor(PoseStack poses,MultiBufferSource buffers,int light,ItemStack stack,RegistryAccess registry,
            EquipmentSlot slot,HumanoidModel<LivingEntity> context) {
        var model=new ReefArmorModel(ReefModelCache.layer(ReefArmorModel.layer(slot)));
        context.copyPropertiesTo(model);
        net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.renderPart(poses,buffers,light,stack,model,BASE_TEXTURE);
        render(poses,buffers,light,stack,registry,slot,context);
    }

    public static void render(PoseStack poses,MultiBufferSource buffers,int light,ItemStack stack,RegistryAccess registry,
            EquipmentSlot slot,HumanoidModel<LivingEntity> context) {
        ArmorTrim trim=ArmorTrim.getTrim(registry,stack).orElse(null);
        if(trim==null || !(stack.getItem() instanceof ArmorItem armor)) return;
        var model=ReefTrimModel.get(ReefModelCache.layer(ReefArmorModel.layer(slot)));
        context.copyPropertiesTo(model);
        var texture=slot==EquipmentSlot.LEGS ? trim.innerTexture(armor.getMaterial()) : trim.outerTexture(armor.getMaterial());
        var sprite=Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET).getSprite(texture);
        model.renderToBuffer(poses,sprite.wrap(buffers.getBuffer(Sheets.armorTrimsSheet())),light,OverlayTexture.NO_OVERLAY,1,1,1,1);
    }
    private ReefArmorTrimRenderer() {}
}
