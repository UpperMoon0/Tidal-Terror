package com.nhat.tidal_terror.entities.cathedral_ray;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A peaceful reef glider. Swimming is driven by native water navigation. */
public class CathedralRayEntity extends WaterAnimal {
    public enum Behavior { CRUISE, SCHOOL, CURIOUS, FLEE }
    private CathedralRaySwimGoal swimGoal;
    private Behavior behavior = Behavior.CRUISE;
    public Behavior getBehavior() { return behavior; }
    void setBehavior(Behavior behavior) { this.behavior = behavior; }
    public CathedralRayEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
        moveControl = new SmoothSwimmingMoveControl(this, 45, 8, 0.025F, 0.1F, false);
        lookControl = new SmoothSwimmingLookControl(this, 8);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.MOVEMENT_SPEED, 0.7).add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override protected void registerGoals() {
        swimGoal = new CathedralRaySwimGoal(this);
        goalSelector.addGoal(1, swimGoal);
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide && swimGoal != null)
            swimGoal.onHurt(source.getEntity() instanceof LivingEntity living ? living : null);
        return accepted;
    }

    @Override protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override public void travel(Vec3 input) {
        if (isEffectiveAi() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else super.travel(input);
    }
}
