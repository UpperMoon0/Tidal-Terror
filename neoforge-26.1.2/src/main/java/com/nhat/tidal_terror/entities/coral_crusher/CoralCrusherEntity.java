package com.nhat.tidal_terror.entities.coral_crusher;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;
import javax.annotation.Nullable;
import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public class CoralCrusherEntity extends WaterAnimal {
    public enum Behavior { PATROL, INVESTIGATE, CIRCLE, WINDUP, CHARGE, RECOVER, FLEE, MELEE_WINDUP, REPOSITION }
    private static final EntityDataAccessor<Integer> DATA_BEHAVIOR =
            SynchedEntityData.defineId(CoralCrusherEntity.class, EntityDataSerializers.INT);
    private CoralCrusherHuntGoal huntGoal;
    private BlockPos territory;
    private static final EntityDataAccessor<Boolean> DATA_SANDY =
            SynchedEntityData.defineId(CoralCrusherEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int SANDY_SPAWN_HEIGHT = 24;

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SANDY, false);
        builder.define(DATA_BEHAVIOR, Behavior.PATROL.ordinal());
    }

    public boolean isSandy() {
        return this.entityData.get(DATA_SANDY);
    }

    public void setSandy(boolean sandy) {
        this.entityData.set(DATA_SANDY, sandy);
    }

    public Behavior getBehavior() { return Behavior.values()[this.entityData.get(DATA_BEHAVIOR)]; }
    void setBehavior(Behavior behavior) { this.entityData.set(DATA_BEHAVIOR, behavior.ordinal()); }
    public BlockPos getTerritory() {
        if (this.territory == null) this.territory = this.blockPosition();
        return this.territory;
    }
    public boolean isRetreating() { return getBehavior() == Behavior.FLEE; }

    @Override public boolean doHurtTarget(net.minecraft.server.level.ServerLevel serverLevel, net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(serverLevel,target);
        if (hit && !level().isClientSide() && target instanceof LivingEntity victim && victim.isAlive()) {
            com.nhat.tidal_terror.effects.ModEffects.applyBleeding(victim, this, 80, 0);
            com.nhat.tidal_terror.particles.ModParticles.bleed(victim, 12);
        }
        return hit;
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel serverLevel, DamageSource source, float amount) {
        boolean damaged = super.hurtServer(serverLevel, source, amount);
        if (damaged && !level().isClientSide() && this.huntGoal != null)
            this.huntGoal.onHurt(source.getEntity() instanceof LivingEntity attacker ? attacker : null);
        return damaged;
    }

    public static boolean sandyAtSpawn(ServerLevelAccessor level, BlockPos pos) {
        if (!level.getBiome(pos).is(ReefWorldgen.BIOME)) return false;
        // OCEAN_FLOOR includes the giant corals. Find the sediment under them,
        // so swimming beside a high coral crown never counts as seabed spawning.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(),
                Math.min(level.getHeight(Heightmap.Types.OCEAN_FLOOR, pos.getX(), pos.getZ()) - 1,
                        level.getSeaLevel() - 1), pos.getZ());
        for (; cursor.getY() >= level.getMinY(); cursor.move(0, -1, 0)) {
            if (level.getBlockState(cursor).is(Blocks.SAND)) {
                int clearance = pos.getY() - cursor.getY();
                return clearance > 0 && clearance <= SANDY_SPAWN_HEIGHT;
            }
        }
        return false;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        // Vanilla block-use, water-use, and dispenser eggs all finalize with
        // SPAWN_EGG. Natural spawns retain their seabed-based skin selection.
        this.setSandy(reason == EntitySpawnReason.SPAWN_ITEM_USE
                ? this.random.nextBoolean() : sandyAtSpawn(level, this.blockPosition()));
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Skin", this.isSandy() ? "sandy" : "blue");
        tag.store("Territory", BlockPos.CODEC, getTerritory());
        tag.putBoolean("Retreating", isRetreating());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        // Existing sharks without a Skin tag retain their original blue skin.
        this.setSandy("sandy".equals(tag.getStringOr("Skin","blue")));
        this.territory=tag.read("Territory",BlockPos.CODEC).orElseGet(()->tag.child("Territory").map(old->new BlockPos(old.getIntOr("X",0),old.getIntOr("Y",0),old.getIntOr("Z",0))).orElse(null));
        if (tag.getBooleanOr("Retreating",false)) this.huntGoal.resumeRetreat();
    }

    public CoralCrusherEntity(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);
        // Keep native three-axis steering, but disable its constant upward
        // buoyancy. Navigation owns depth during patrol, pursuit and retreat.
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10) {
            @Override
            protected Optional<Float> getXRotD() {
                // Melee looks at the player's eyes, while the water path uses
                // feet height. Let the move controller own pitch during swimming
                // so looking above a waypoint cannot prevent reaching it.
                if (CoralCrusherEntity.this.isInWater() && !CoralCrusherEntity.this.getNavigation().isDone()) {
                    return Optional.empty();
                }
                return super.getXRotD();
            }
        };
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.huntGoal = new CoralCrusherHuntGoal(this);
        this.goalSelector.addGoal(1, this.huntGoal);
        // Investigation supplies the delay; nearby visible players should not
        // be missed for an arbitrary random number of acquisition attempts.
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0,
                true, false, (target,serverLevel) -> this.huntGoal.canAcquire(target)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Drowned.class, 0,
                true, false, (target,serverLevel) -> this.huntGoal.canAcquire(target)) {
            @Override protected AABB getTargetSearchArea(double range) {
                // Vanilla's generic mob search has only four blocks of vertical
                // expansion. Deep-water hunting needs a full three-dimensional search.
                return CoralCrusherEntity.this.getBoundingBox().inflate(48, 48, 48);
            }
        });
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractFish.class, 120,
                true, false, (target,serverLevel) -> this.huntGoal.canAcquire(target) && distanceToSqr(target) < 100));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public void travel(Vec3 travelVector) {
        // Consume navigation's steering without any automatic rise or sink.
        // Existing momentum decays normally; the hunt goal chooses the height.
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(travelVector);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }
}
