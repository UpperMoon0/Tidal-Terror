package com.nhat.tidal_terror.entities.cathedral_ray;
import com.nhat.tidal_terror.client.ReefRenderState;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.Identifier;
public final class CathedralRayRenderer extends MobRenderer<CathedralRayEntity,ReefRenderState,CathedralRayModel> {
 private static final Identifier SKIN=Identifier.fromNamespaceAndPath("tidalterror","textures/entity/cathedral_ray/cathedral_ray.png");
 public CathedralRayRenderer(EntityRendererProvider.Context context){super(context,new CathedralRayModel(context.bakeLayer(CathedralRayModel.LAYER)),.8F);addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<ReefRenderState,CathedralRayModel>(this){
   @Override public void submit(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,int light,ReefRenderState state,float yaw,float pitch){
    if(state.isInvisible)return;
    collector.submitModel(getParentModel(),state,pose,net.minecraft.client.renderer.rendertype.RenderTypes.eyes(Identifier.fromNamespaceAndPath("tidalterror","textures/entity/cathedral_ray/cathedral_ray_glow.png")),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,-1,null,state.outlineColor,null);
   }});}
 @Override public ReefRenderState createRenderState(){return new ReefRenderState();}
 @Override public void extractRenderState(CathedralRayEntity entity,ReefRenderState state,float partialTick){super.extractRenderState(entity,state,partialTick);state.movement=(float)entity.getDeltaMovement().length();state.submerged=entity.isInWater();state.attack=entity.getAttackAnim(partialTick);}
 @Override public Identifier getTextureLocation(ReefRenderState state){return SKIN;}
}
