package com.nhat.tidal_terror.client;
import net.minecraft.client.renderer.special.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
public record ReefSpearSpecialRenderer(ModelPart root) implements NoDataSpecialModelRenderer {
 private static final Identifier TEXTURE=Identifier.fromNamespaceAndPath("tidalterror","textures/item/reef_spear_model.png");
 @Override public void submit(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline){
  collector.submitModelPart(root,pose,net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(TEXTURE),light,overlay,null,false,foil,-1,null,outline);
 }
 @Override public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> out){root.getExtentsForGui(new com.mojang.blaze3d.vertex.PoseStack(),out);}
 public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
  public static final com.mojang.serialization.MapCodec<Unbaked> CODEC=com.mojang.serialization.MapCodec.unit(new Unbaked());
  @Override public com.mojang.serialization.MapCodec<Unbaked> type(){return CODEC;}
  @Override public ReefSpearSpecialRenderer bake(SpecialModelRenderer.BakingContext context){return new ReefSpearSpecialRenderer(context.entityModelSet().bakeLayer(ReefSpearModel.LAYER));}
 }
}
