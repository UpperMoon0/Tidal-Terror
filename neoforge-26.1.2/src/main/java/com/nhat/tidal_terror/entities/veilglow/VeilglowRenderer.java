package com.nhat.tidal_terror.entities.veilglow;
import com.nhat.tidal_terror.client.ReefRenderState;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.Identifier;
public final class VeilglowRenderer extends MobRenderer<VeilglowEntity,ReefRenderState,VeilglowModel> {
 private static final Identifier SKIN=Identifier.fromNamespaceAndPath("tidalterror","textures/entity/veilglow/veilglow.png");
 public VeilglowRenderer(EntityRendererProvider.Context context){super(context,new VeilglowModel(context.bakeLayer(VeilglowModel.LAYER)),.35F);addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<ReefRenderState,VeilglowModel>(this){
   private final VeilglowModel bellModel=new VeilglowModel(context.bakeLayer(VeilglowModel.LAYER),true);
   @Override public void submit(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,int light,ReefRenderState state,float yaw,float pitch){
    if(state.isInvisible)return;
    collector.submitModel(getParentModel(),state,pose,net.minecraft.client.renderer.rendertype.RenderTypes.eyes(Identifier.fromNamespaceAndPath("tidalterror","textures/entity/veilglow/veilglow_glow.png")),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,-1,null,state.outlineColor,null);
   }});addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<ReefRenderState,VeilglowModel>(this){
   private final VeilglowModel bellModel=new VeilglowModel(context.bakeLayer(VeilglowModel.LAYER),true);
   @Override public void submit(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,int light,ReefRenderState state,float yaw,float pitch){
    if(state.isInvisible)return;
    collector.submitModel(bellModel,state,pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(SKIN),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,-1,null,state.outlineColor,null);
   }});}
 @Override public ReefRenderState createRenderState(){return new ReefRenderState();}
 @Override public void extractRenderState(VeilglowEntity entity,ReefRenderState state,float partialTick){super.extractRenderState(entity,state,partialTick);state.movement=(float)entity.getDeltaMovement().length();state.submerged=entity.isInWater();state.attack=entity.getAttackAnim(partialTick);state.jellyBehavior=entity.getBehavior();}
 @Override public Identifier getTextureLocation(ReefRenderState state){return SKIN;}
}
