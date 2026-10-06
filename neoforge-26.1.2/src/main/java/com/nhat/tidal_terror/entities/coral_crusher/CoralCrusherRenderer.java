package com.nhat.tidal_terror.entities.coral_crusher;
import com.nhat.tidal_terror.client.ReefRenderState;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.Identifier;
public final class CoralCrusherRenderer extends MobRenderer<CoralCrusherEntity,ReefRenderState,CoralCrusherModel> {
 private static final Identifier SKIN=Identifier.fromNamespaceAndPath("tidalterror","textures/entity/coral_crusher/coral_crusher.generated-v1.png");
 public CoralCrusherRenderer(EntityRendererProvider.Context context){super(context,new CoralCrusherModel(context.bakeLayer(ModModelLayers.CORAL_CRUSHER_LAYER)),2F);}
 @Override public ReefRenderState createRenderState(){return new ReefRenderState();}
 @Override public void extractRenderState(CoralCrusherEntity entity,ReefRenderState state,float partialTick){super.extractRenderState(entity,state,partialTick);state.movement=(float)entity.getDeltaMovement().length();state.submerged=entity.isInWater();state.attack=entity.getAttackAnim(partialTick);state.sandy=entity.isSandy();state.windingUp=entity.getBehavior()==CoralCrusherEntity.Behavior.WINDUP||entity.getBehavior()==CoralCrusherEntity.Behavior.MELEE_WINDUP;}
 @Override public Identifier getTextureLocation(ReefRenderState state){return state.sandy?Identifier.fromNamespaceAndPath("tidalterror","textures/entity/coral_crusher/coral_crusher.sandy-v1.png"):SKIN;}
}
