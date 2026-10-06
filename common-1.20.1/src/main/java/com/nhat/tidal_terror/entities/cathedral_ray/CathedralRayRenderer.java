package com.nhat.tidal_terror.entities.cathedral_ray;

import com.nhat.tidal_terror.TidalTerror;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class CathedralRayRenderer extends MobRenderer<CathedralRayEntity, CathedralRayModel> {
    private static final ResourceLocation SKIN = new ResourceLocation(TidalTerror.MODID,
            "textures/entity/cathedral_ray/cathedral_ray.png");
    private static final ResourceLocation GLOW = new ResourceLocation(TidalTerror.MODID,
            "textures/entity/cathedral_ray/cathedral_ray_glow.png");
    public CathedralRayRenderer(EntityRendererProvider.Context context) {
        super(context, new CathedralRayModel(context.bakeLayer(CathedralRayModel.LAYER)), 0.8F);
        addLayer(new RenderLayer<CathedralRayEntity, CathedralRayModel>(this) {
            @Override public void render(PoseStack pose, MultiBufferSource buffers, int light,
                    CathedralRayEntity ray, float limbSwing, float limbAmount, float partialTick,
                    float age, float yaw, float pitch) {
                if (ray.isInvisible()) return;
                VertexConsumer consumer = buffers.getBuffer(RenderType.eyes(GLOW));
                getParentModel().renderToBuffer(pose, consumer, 15728880, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(CathedralRayEntity ray) { return SKIN; }
}
