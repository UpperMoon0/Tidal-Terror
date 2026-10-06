package com.nhat.tidal_terror.entities.veilglow;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** A slow drifting jellyfish; contact stings, but it never pursues a target. */
public class VeilglowEntity extends WaterAnimal {
    public enum Behavior { DRIFT, PULSE, BLOOM, FLEE, RECOVER }
    private static final EntityDataAccessor<Byte> BEHAVIOR=SynchedEntityData.defineId(VeilglowEntity.class,EntityDataSerializers.BYTE);
    private VeilglowDriftGoal driftGoal;
    private int stingCooldown;
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(BEHAVIOR,(byte)0);}
    public Behavior getBehavior(){return Behavior.values()[entityData.get(BEHAVIOR)];}
    void setBehavior(Behavior state){entityData.set(BEHAVIOR,(byte)state.ordinal());}
    public VeilglowEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
        moveControl = new SmoothSwimmingMoveControl(this, 30, 5, 1.0F, 0.08F, false);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }
    @Override protected void registerGoals() {
        driftGoal=new VeilglowDriftGoal(this);
        goalSelector.addGoal(1,driftGoal);
    }
    @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel serverLevel, DamageSource source,float amount){
        boolean hit=super.hurtServer(serverLevel,source,amount);
        if(hit&&!level().isClientSide()&&driftGoal!=null)
            driftGoal.startle(source.getEntity() instanceof LivingEntity living?living:null);
        return hit;
    }
    @Override protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }
    @Override public void travel(Vec3 input) {
        if(isEffectiveAi() && isInWater()) {
            // Swimming control already scales input by movement speed.
            // Keep drift acceleration above the native tiny-motion cutoff.
            moveRelative(0.04F, input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.93));
        } else super.travel(input);
    }
    @Override public void tick() {
        super.tick();
        if(stingCooldown>0)stingCooldown--;
    }
    @Override public void playerTouch(Player player) {
        super.playerTouch(player);
        if(!level().isClientSide() && isAlive() && isInWater() && !player.isCreative()
                && !player.isSpectator() && stingCooldown==0 && player.invulnerableTime==0
                && getBoundingBox().intersects(player.getBoundingBox())) {
            if(player.hurtServer((net.minecraft.server.level.ServerLevel)level(),damageSources().mobAttack(this),2)){
                stingCooldown=40;
                if(driftGoal!=null)driftGoal.startle(player);
            }
        }
    }
}
