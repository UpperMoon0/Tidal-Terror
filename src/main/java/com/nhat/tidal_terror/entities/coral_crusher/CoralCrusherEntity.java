package com.nhat.tidal_terror.entities.coral_crusher;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public class CoralCrusherEntity extends WaterAnimal {
    private static final EntityDataAccessor<Boolean> DATA_SANDY =
            SynchedEntityData.defineId(CoralCrusherEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int SANDY_SPAWN_HEIGHT = 24;

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SANDY, false);
    }

    public boolean isSandy() {
        return this.entityData.get(DATA_SANDY);
    }

    public void setSandy(boolean sandy) {
        this.entityData.set(DATA_SANDY, sandy);
    }

    public static boolean sandyAtSpawn(ServerLevelAccessor level, BlockPos pos) {
        if (!level.getBiome(pos).is(ReefWorldgen.BIOME)) return false;
        // OCEAN_FLOOR includes the giant corals. Find the sediment under them,
        // so swimming beside a high coral crown never counts as seabed spawning.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(),
                Math.min(level.getHeight(Heightmap.Types.OCEAN_FLOOR, pos.getX(), pos.getZ()) - 1,
                        level.getSeaLevel() - 1), pos.getZ());
        for (; cursor.getY() >= level.getMinBuildHeight(); cursor.move(0, -1, 0)) {
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
            MobSpawnType reason, @Nullable SpawnGroupData groupData, @Nullable CompoundTag spawnTag) {
        this.setSandy(sandyAtSpawn(level, this.blockPosition()));
        return super.finalizeSpawn(level, difficulty, reason, groupData, spawnTag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Skin", this.isSandy() ? "sandy" : "blue");
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Existing sharks without a Skin tag retain their original blue skin.
        this.setSandy("sandy".equals(tag.getString("Skin")));
    }

    public CoralCrusherEntity(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);
        // Minecraft 1.20.1 Dolphin's controls steer navigation in all three axes.
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
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
        // Dolphin's melee multiplier avoids overshooting the swim controller's turns.
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        // RandomSwimmingGoal owns MOVE, so melee can interrupt wandering.
        this.goalSelector.addGoal(3, new RandomSwimmingGoal(this, 1.0D, 50));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public void travel(Vec3 travelVector) {
        // Match Dolphin.travel: consume the swim controller's inputs rather than
        // replacing navigation's velocity with an unrelated random vector.
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }
}
