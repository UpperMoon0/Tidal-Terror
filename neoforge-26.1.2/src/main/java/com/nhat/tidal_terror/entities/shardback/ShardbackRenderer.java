package com.nhat.tidal_terror.entities.shardback;
import com.nhat.tidal_terror.client.ReefRenderState;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.Identifier;
public final class ShardbackRenderer extends MobRenderer<ShardbackEntity,ReefRenderState,ShardbackModel> {
 private static final Identifier SKIN=Identifier.fromNamespaceAndPath("tidalterror","textures/entity/shardback/shardback.png");
 public ShardbackRenderer(EntityRendererProvider.Context context){super(context,new ShardbackModel(context.bakeLayer(ShardbackModel.LAYER)),.55F);}
 @Override public ReefRenderState createRenderState(){return new ReefRenderState();}
 @Override public void extractRenderState(ShardbackEntity entity,ReefRenderState state,float partialTick){super.extractRenderState(entity,state,partialTick);state.movement=(float)entity.getDeltaMovement().horizontalDistance();state.submerged=entity.isInWater();state.attack=entity.getAttackAnim(partialTick);state.crabBehavior=entity.getBehavior();}
 @Override public Identifier getTextureLocation(ReefRenderState state){return SKIN;}
}
