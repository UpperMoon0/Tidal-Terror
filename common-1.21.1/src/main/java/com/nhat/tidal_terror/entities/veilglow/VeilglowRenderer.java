package com.nhat.tidal_terror.entities.veilglow;

import com.nhat.tidal_terror.TidalTerror;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class VeilglowRenderer extends MobRenderer<VeilglowEntity,VeilglowModel> {
    private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath(TidalTerror.MODID,"textures/entity/veilglow/veilglow.png");
    private static final ResourceLocation GLOW=ResourceLocation.fromNamespaceAndPath(TidalTerror.MODID,"textures/entity/veilglow/veilglow_glow.png");
    public VeilglowRenderer(EntityRendererProvider.Context context) {
        super(context,new VeilglowModel(context.bakeLayer(VeilglowModel.LAYER)),0.35F);
        addLayer(new RenderLayer<VeilglowEntity,VeilglowModel>(this) {
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,VeilglowEntity jelly,
                    float swing,float amount,float partial,float age,float yaw,float pitch) {
                if(jelly.isInvisible())return;
                getParentModel().renderToBuffer(pose,buffers.getBuffer(RenderType.eyes(GLOW)),15728880,OverlayTexture.NO_OVERLAY,-1);
            }
        });
        addLayer(new RenderLayer<VeilglowEntity,VeilglowModel>(this) {
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,VeilglowEntity jelly,
                    float swing,float amount,float partial,float age,float yaw,float pitch) {
                if(jelly.isInvisible())return;
                getParentModel().renderBell(pose,buffers.getBuffer(RenderType.entityTranslucent(SKIN)),light,OverlayTexture.NO_OVERLAY);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(VeilglowEntity jelly){return SKIN;}
}
