package com.nhat.tidal_terror.entities.shardback;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/** A defensive benthic crab: browses submerged terrain and retreats from danger. */
public class ShardbackEntity extends PathfinderMob {
 public enum Behavior { WANDER, FORAGE, THREATEN, FLEE, SHELTER, RECOVER }
 private static final EntityDataAccessor<Byte> BEHAVIOR=SynchedEntityData.defineId(ShardbackEntity.class,EntityDataSerializers.BYTE);
 private ShardbackForageGoal forageGoal;
 private int pinchCooldown;
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(BEHAVIOR,(byte)0);}
 public Behavior getBehavior(){return Behavior.values()[entityData.get(BEHAVIOR)];}
 void setBehavior(Behavior state){entityData.set(BEHAVIOR,(byte)state.ordinal());}
 public ShardbackEntity(EntityType<? extends PathfinderMob> type,Level level){
  super(type,level);setPathfindingMalus(BlockPathTypes.WATER,0);setPathfindingMalus(BlockPathTypes.WATER_BORDER,0);
 }
 public static AttributeSupplier.Builder createAttributes(){return Mob.createMobAttributes()
  .add(Attributes.MAX_HEALTH,16).add(Attributes.MOVEMENT_SPEED,.2).add(Attributes.ARMOR,4);}
 public static boolean isSeabed(net.minecraft.world.level.block.state.BlockState state){
  return state.is(net.minecraft.tags.BlockTags.SAND)
   || state.is(net.minecraft.world.level.block.Blocks.SANDSTONE)
   || state.is(net.minecraft.world.level.block.Blocks.SMOOTH_SANDSTONE)
   || state.is(net.minecraft.world.level.block.Blocks.GRAVEL)
   || state.is(net.minecraft.world.level.block.Blocks.CLAY);
 }
 @Override protected PathNavigation createNavigation(Level level){return new AmphibiousPathNavigation(this,level);}
 @Override protected void registerGoals(){
  forageGoal=new ShardbackForageGoal(this);goalSelector.addGoal(1,forageGoal);
 }
 @Override public boolean hurt(DamageSource source,float amount){
  boolean hit=super.hurt(source,amount);
  if(hit&&!level().isClientSide&&forageGoal!=null)
   forageGoal.startle(source.getEntity() instanceof LivingEntity living?living:null);
  return hit;
 }
 @Override public void tick(){super.tick();if(pinchCooldown>0)pinchCooldown--;}
 @Override public void playerTouch(Player player){
  super.playerTouch(player);
  if(!level().isClientSide&&isAlive()&&isInWater()&&getBehavior()==Behavior.THREATEN
    &&pinchCooldown==0&&!player.isCreative()&&!player.isSpectator()&&player.invulnerableTime==0
    &&getBoundingBox().intersects(player.getBoundingBox())&&player.hurt(damageSources().mobAttack(this),2)){
   pinchCooldown=60;if(forageGoal!=null)forageGoal.startle(player);
  }
 }
 @Override public boolean canBreatheUnderwater(){return true;}
 // Match native aquatic spawn obstruction: liquid itself is a valid habitat.
 @Override public boolean checkSpawnObstruction(LevelReader level){return level.isUnobstructed(this);}
 @Override public boolean isPushedByFluid(){return false;}
 @Override public void travel(Vec3 input){
  if(isEffectiveAi()&&isInWater()){
   moveRelative(getSpeed(),input);move(MoverType.SELF,getDeltaMovement());
   // Ground friction and downward weight keep the crab on the seabed.
   setDeltaMovement(getDeltaMovement().multiply(.6,.8,.6).add(0,-.03,0));
  }else super.travel(input);
 }
}
