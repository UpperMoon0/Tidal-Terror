package com.nhat.tidal_terror.entities.coral_crusher;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CoralCrusherRenderer extends MobRenderer<CoralCrusherEntity, CoralCrusherModel<CoralCrusherEntity>> {
    private static final ResourceLocation BLUE = new ResourceLocation(TidalTerror.MODID,
            "textures/entity/coral_crusher/coral_crusher.generated-v1.png");
    private static final ResourceLocation SANDY = new ResourceLocation(TidalTerror.MODID,
            "textures/entity/coral_crusher/coral_crusher.sandy-v1.png");
    public CoralCrusherRenderer(EntityRendererProvider.Context context) {
        super(context, new CoralCrusherModel<>(context.bakeLayer(ModModelLayers.CORAL_CRUSHER_LAYER)), 2f);
    }

    @Override
    public ResourceLocation getTextureLocation(CoralCrusherEntity entity) {
        return entity.isSandy() ? SANDY : BLUE;
    }

    @Override
    public void render(CoralCrusherEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }
}
