package com.nhat.tidal_terror.entities.coral_crusher;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import static com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity.Behavior.*;

/** One MOVE/LOOK owner; native water paths remain responsible for steering. */
public final class CoralCrusherHuntGoal extends Goal {
    public static final float RETREAT_HEALTH = .30F, RESUME_HEALTH = .60F;
    public static final int SAFE_DELAY = 200, HEAL_INTERVAL = 80, WINDUP_TICKS = 20, RECOVERY_TICKS = 60;
    // Native navigation multipliers, not blocks per tick. Swimming controls
    // combine them with MOVEMENT_SPEED and the aquatic acceleration factor.
    public static final double PATROL_SPEED = .8, INVESTIGATE_SPEED = .95,
            PURSUIT_SPEED = 1.1, CIRCLE_SPEED = .9, CHARGE_SPEED = 1.5,
            RECOVERY_SPEED = .85, REPOSITION_SPEED = 1.0, FLEE_SPEED = 1.25;
    private final CoralCrusherEntity shark;
    private int phaseTicks, quietTicks, healTicks, disengageTicks;
    private LivingEntity threat;
    private Vec3 lastThreat, chargeEnd, chargeDirection;
    private int threatMemory;
    private boolean biteAttempted;
    private double circleAngle;
    private LivingEntity aggressor;
    private LivingEntity combatTarget;
    private int angerTicks, chargeTicks;
    private boolean lastAttackWasMelee;
    private Vec3 recoveryDirection;

    public CoralCrusherHuntGoal(CoralCrusherEntity shark) {
        this.shark = shark;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean canUse() { return true; }
    @Override public boolean canContinueToUse() { return true; }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { shark.getTerritory(); }
    @Override public void stop() { shark.getNavigation().stop(); }

    public boolean canAcquire(LivingEntity target) {
        return !shark.isRetreating() && disengageTicks == 0
                && shark.getHealth() > shark.getMaxHealth() * RETREAT_HEALTH
                && validTarget(target) && shark.distanceToSqr(target) <= (CoralCrusherRuntime.isDrowned(target) ? 48 * 48 : 32 * 32)
                && target.position().distanceToSqr(Vec3.atCenterOf(shark.getTerritory())) <= 96 * 96;
    }
    private boolean validTarget(@Nullable LivingEntity target) {
        return target != null && target.isAlive() && CoralCrusherRuntime.inWater(target)
                && (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator());
    }
    public void resumeRetreat() { change(FLEE); quietTicks = healTicks = 0; }
    public void onHurt(@Nullable LivingEntity attacker) {
        quietTicks = healTicks = 0;
        if (attacker != null && attacker.isAlive()) {
            threat = attacker; lastThreat = attacker.position(); threatMemory = 300;
        }
        if (shark.getHealth() <= shark.getMaxHealth() * RETREAT_HEALTH) resumeRetreat();
        else if (!shark.isRetreating() && validTarget(attacker)) {
            // Retaliation bypasses the passive reacquisition delay, but an
            // attacking drowned must never replace a living player target.
            if (attacker instanceof Player || !(shark.getTarget() instanceof Player)) {
                aggressor = attacker; angerTicks = 600; disengageTicks = 0;
                if (shark.getTarget() != attacker || shark.getBehavior() == PATROL) change(INVESTIGATE);
                shark.setTarget(attacker);
            }
        }
    }
    private void change(CoralCrusherEntity.Behavior state) {
        shark.setBehavior(state); phaseTicks = 0;
        shark.getNavigation().stop();
    }
    @Override public void tick() {
        phaseTicks++;
        if (disengageTicks > 0) disengageTicks--;
        if (threatMemory > 0) threatMemory--;
        if (angerTicks > 0) angerTicks--;
        if (shark.getHealth() <= shark.getMaxHealth() * RETREAT_HEALTH && !shark.isRetreating()) {
            if (validTarget(shark.getTarget())) {
                threat = shark.getTarget(); lastThreat = threat.position(); threatMemory = 300;
            }
            resumeRetreat();
        }
        LivingEntity nearbyThreat = nearbyThreat();
        boolean safe = CoralCrusherRuntime.inWater(shark) && nearbyThreat == null && !validTarget(shark.getTarget());
        if (!safe) { quietTicks = healTicks = 0; }
        else {
            quietTicks++;
            if (quietTicks >= SAFE_DELAY && ++healTicks >= HEAL_INTERVAL) {
                shark.heal(1); healTicks = 0;
            }
        }
        if (shark.isRetreating()) {
            shark.setTarget(null);
            if (nearbyThreat != null) {
                threat = nearbyThreat; lastThreat = nearbyThreat.position(); threatMemory = 300;
            }
            if (safe && quietTicks >= SAFE_DELAY && shark.getHealth() >= shark.getMaxHealth() * RESUME_HEALTH) {
                threat = null; lastThreat = null; change(PATROL);
            } else {
                if (phaseTicks % 15 == 1 || shark.getNavigation().isDone()) flee();
                return;
            }
        }
        // Native target goals retain player > drowned > fish priority. Restore
        // a remembered attacker if sight loss cleared it, without demoting players.
        if (angerTicks > 0 && validTarget(aggressor) && shark.distanceToSqr(aggressor) <= 64 * 64
                && (aggressor instanceof Player || !(shark.getTarget() instanceof Player))) shark.setTarget(aggressor);
        LivingEntity target = shark.getTarget();
        if (!validTarget(target) || shark.distanceToSqr(target) > 64 * 64
                || target.position().distanceToSqr(Vec3.atCenterOf(shark.getTerritory())) > 96 * 96) {
            if (target != null || shark.getBehavior() != PATROL) {
                shark.setTarget(null); disengageTicks = 200;
            }
            if (shark.getBehavior() != PATROL) change(PATROL);
            // Keep a patrol route long enough to finish its turn and swim.
            // Replacing it every two seconds can leave a large shark oscillating.
            if (shark.getNavigation().isDone() || phaseTicks % 160 == 1) patrol();
            return;
        }
        quietTicks = healTicks = 0;
        if (target != combatTarget) {
            combatTarget = target;
            change(INVESTIGATE);
        }
        shark.getLookControl().setLookAt(target, 30, 30);
        switch (shark.getBehavior()) {
            case PATROL -> change(INVESTIGATE);
            case INVESTIGATE -> {
                boolean angry = target == aggressor && angerTicks > 0;
                if (phaseTicks % 10 == 1 || shark.getNavigation().isDone())
                    approach(target.position(), angry ? PURSUIT_SPEED : INVESTIGATE_SPEED);
                if (angry && phaseTicks >= 10 && shark.distanceToSqr(target) <= 12 * 12) {
                    chooseAttack(target); break;
                }
                if (phaseTicks >= 30 && shark.distanceToSqr(target) <= 12 * 12) {
                    circleAngle = Math.atan2(shark.getZ() - target.getZ(), shark.getX() - target.getX());
                    change(CIRCLE);
                } else if (phaseTicks > 900) disengage();
            }
            case CIRCLE -> {
                circleAngle += .035;
                if (phaseTicks % 10 == 1) {
                    Vec3 orbit = target.position().add(Math.cos(circleAngle) * 6,
                            Math.sin(circleAngle * .5) * 1.5, Math.sin(circleAngle) * 6);
                    if (!approach(orbit, CIRCLE_SPEED)) approach(target.position(), PATROL_SPEED);
                }
                int interestTime = target.getHealth() < target.getMaxHealth()
                        || target.getDeltaMovement().lengthSqr() > .01 ? 60 : 90;
                if (phaseTicks >= interestTime && shark.distanceToSqr(target) <= 12 * 12
                        && shark.getSensing().hasLineOfSight(target)) chooseAttack(target);
                else if (phaseTicks > 200) disengage();
            }
            case WINDUP -> {
                shark.getNavigation().stop();
                shark.setDeltaMovement(shark.getDeltaMovement().scale(.8));
                if (phaseTicks >= WINDUP_TICKS) beginCharge(target);
            }
            case CHARGE -> {
                if (!biteAttempted && shark.distanceToSqr(target) <= attackReachSqr(target)
                        && shark.getSensing().hasLineOfSight(target)) {
                    biteAttempted = true;
                    lastAttackWasMelee = false;
                    shark.swing(InteractionHand.MAIN_HAND);
                    CoralCrusherRuntime.bite(shark,target);
                    // Native retaliation (for example Thorns) can hurt the
                    // shark inside doHurtTarget and immediately trigger retreat.
                    if (shark.isRetreating()) return;
                    // Continue along the committed route during recovery;
                    // no repeated bite checks or pursuit of a dodging target.
                    beginRecovery(target, true);
                } else if (phaseTicks >= chargeTicks || shark.getNavigation().isDone()) {
                    beginRecovery(target, true);
                }
            }
            case RECOVER -> {
                // Keep swimming through the cooldown, including after a close
                // bite and after the charge's committed path has finished.
                // This is a pass/escape route, never another attack or homing charge.
                if (shark.getNavigation().isDone() && phaseTicks % 10 == 1) swimRecovery();
                if (phaseTicks >= RECOVERY_TICKS) {
                    if (inMeleeRange(target) && !lastAttackWasMelee) change(MELEE_WINDUP);
                    else change(REPOSITION);
                }
            }
            case MELEE_WINDUP -> {
                shark.getNavigation().stop();
                shark.setDeltaMovement(shark.getDeltaMovement().scale(.8));
                if (phaseTicks >= WINDUP_TICKS) {
                    if (!inMeleeRange(target)) { change(REPOSITION); break; }
                    lastAttackWasMelee = true;
                    shark.swing(InteractionHand.MAIN_HAND);
                    CoralCrusherRuntime.bite(shark,target);
                    if (!shark.isRetreating()) beginRecovery(target, false);
                }
            }
            case REPOSITION -> {
                if (phaseTicks % 30 == 1) {
                    Vec3 away = shark.position().subtract(target.position()).multiply(1, 0, 1);
                    if (away.lengthSqr() < .01) away = new Vec3(1, 0, 0);
                    boolean routed = false;
                    for (int i = 0; i < 8 && !routed; i++)
                        routed = approach(target.position().add(away.normalize().yRot(i * .4F).scale(8)), REPOSITION_SPEED);
                    if (!routed) approach(target.position(), REPOSITION_SPEED);
                }
                if (phaseTicks >= 30 && shark.distanceToSqr(target) >= 6 * 6
                        && shark.distanceToSqr(target) <= 14 * 14 && Math.abs(shark.getY() - target.getY()) <= 3
                        && shark.getSensing().hasLineOfSight(target)) change(WINDUP);
                else if (phaseTicks >= 140) chooseAttack(target);
            }
            default -> { }
        }
    }
    private double attackReachSqr(LivingEntity target) {
        // Same reach calculation as Minecraft 1.20.1 MeleeAttackGoal.
        return shark.getBbWidth() * 2 * shark.getBbWidth() * 2 + target.getBbWidth();
    }
    private boolean inMeleeRange(LivingEntity target) {
        return shark.distanceToSqr(target) <= attackReachSqr(target)
                && Math.abs(shark.getY() - target.getY()) <= 1.5 && shark.getSensing().hasLineOfSight(target);
    }
    private void chooseAttack(LivingEntity target) {
        change(inMeleeRange(target) && !lastAttackWasMelee ? MELEE_WINDUP : WINDUP);
    }
    private void beginCharge(LivingEntity target) {
        chargeDirection = target.position().subtract(shark.position()).normalize();
        chargeEnd = target.position().add(chargeDirection.scale(6));
        change(CHARGE); biteAttempted = false;
        // SmoothSwimmingMoveControl changes pitch by five degrees per tick.
        // Include turning time and route length instead of a fixed two-second cutoff.
        chargeTicks = Math.min(160, Math.max(60, (int)Math.ceil(shark.distanceTo(target) * 6) + 20));
        if (!approach(chargeEnd, CHARGE_SPEED)) {
            chargeEnd = target.position();
            if (!approach(chargeEnd, CHARGE_SPEED)) { beginRecovery(target, true); return; }
        }
    }
    private void beginRecovery(LivingEntity target, boolean fromCharge) {
        recoveryDirection = fromCharge && chargeDirection != null ? chargeDirection
                : target.position().subtract(shark.position()).normalize();
        if (recoveryDirection.lengthSqr() < .01) recoveryDirection = shark.getLookAngle();
        shark.setBehavior(RECOVER); phaseTicks = 0;
        if (fromCharge && !shark.getNavigation().isDone()) {
            // Preserve the committed endpoint; only lower the steering speed.
            shark.getNavigation().setSpeedModifier(RECOVERY_SPEED);
        } else {
            shark.getNavigation().stop();
            swimRecovery();
        }
    }
    private void swimRecovery() {
        // Prefer the previous pass direction and fan out around obstructions.
        // Keep the first reachable heading until its route finishes.
        for (int i = 0; i < 16; i++) {
            float angle = (float)((i % 2 == 0 ? 1 : -1) * ((i + 1) / 2) * .35);
            Vec3 direction = recoveryDirection.yRot(angle);
            if (i >= 8) {
                // A completed steep pass may face the floor or surface. Turn
                // laterally instead of repeatedly requesting dry/solid endpoints.
                direction = new Vec3(direction.x, 0, direction.z);
                if (direction.lengthSqr() < .01) direction = new Vec3(1, 0, 0).yRot(angle);
                direction = direction.normalize();
            }
            for (int length : new int[]{6, 3}) {
                if (approach(shark.position().add(direction.scale(length)), RECOVERY_SPEED)) {
                    recoveryDirection = direction;
                    return;
                }
            }
        }
        // No water route: do not force velocity through solid reef blocks.
        // The finite cooldown still transitions to repositioning.
    }
    private void disengage() {
        shark.setTarget(null); disengageTicks = 200; change(PATROL);
    }
    private LivingEntity nearbyThreat() {
        LivingEntity nearest = null;
        double distance = 24 * 24;
        for (Player player : shark.level().players()) {
            if (player.isAlive() && !player.isCreative() && !player.isSpectator()
                    && shark.distanceToSqr(player) < distance) {
                nearest = player; distance = shark.distanceToSqr(player);
            }
        }
        if (threat != null && threat.isAlive() && !(threat instanceof Player player && (player.isCreative() || player.isSpectator()))
                && shark.distanceToSqr(threat) < distance) nearest = threat;
        return nearest;
    }
    private void flee() {
        Vec3 origin = lastThreat != null && threatMemory > 0 ? lastThreat : null;
        Vec3 away = origin == null ? new Vec3(shark.getRandom().nextDouble() - .5, 0,
                shark.getRandom().nextDouble() - .5).normalize() : shark.position().subtract(origin).normalize();
        if (away.lengthSqr() < .01) away = new Vec3(1, 0, 0);
        if (away.horizontalDistanceSqr() < .25) {
            // Preserve a stable horizontal heading while escaping vertically
            // aligned threats; changing it every repath causes oscillation.
            Vec3 horizontal = new Vec3(away.x, 0, away.z);
            horizontal = horizontal.lengthSqr() < .0001 ? new Vec3(1, 0, 0) : horizontal.normalize();
            away = horizontal.add(0, Math.copySign(.3, away.y), 0).normalize();
        }
        for (int i = 0; i < 16; i++) {
            double angle = (i % 2 == 0 ? 1 : -1) * (i / 2) * .18;
            Vec3 candidate = shark.position().add(away.yRot((float) angle).scale(4 + i % 4))
                    .add(0, (shark.getRandom().nextDouble() - .5) * 3, 0);
            if (origin != null && candidate.distanceToSqr(origin) <= shark.position().distanceToSqr(origin) + 1) continue;
            if (approach(candidate, FLEE_SPEED)) return;
        }
        shark.getNavigation().stop();
    }
    private void patrol() {
        BlockPos home = shark.getTerritory();
        Vec3 anchor = Vec3.atBottomCenterOf(home);
        if (shark.isSandy() && shark.level().hasChunkAt(home)) {
            var floor = home.mutable();
            for (int y = home.getY() - 1; y >= CoralCrusherRuntime.minY(shark); y--) {
                floor.setY(y);
                if (shark.level().getBlockState(floor).is(Blocks.SAND)) {
                    anchor = new Vec3(anchor.x, y + 8, anchor.z); break;
                }
            }
        }
        if (anchor.distanceToSqr(shark.position()) > 16 * 16)
            anchor = shark.position().add(anchor.subtract(shark.position()).normalize().scale(10));
        for (int i = 0; i < 16; i++) {
            Vec3 candidate = anchor.add(shark.getRandom().nextInt(25) - 12,
                    shark.getRandom().nextInt(shark.isSandy() ? 5 : 11) - (shark.isSandy() ? 3 : 5),
                    shark.getRandom().nextInt(25) - 12);
            if (candidate.distanceToSqr(shark.position()) > 4 && approach(candidate, PATROL_SPEED)) return;
        }
    }
    private boolean approach(Vec3 destination, double speed) {
        BlockPos pos = BlockPos.containing(destination);
        if (!shark.level().hasChunkAt(pos) || !shark.level().getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                || !shark.level().getFluidState(pos.above()).is(net.minecraft.tags.FluidTags.WATER)
                || !shark.level().noCollision(shark, CoralCrusherRuntime.collisionBox(shark,destination))) return false;
        double offset = (int)(shark.getBbWidth() + 1) * .5;
        var path = shark.getNavigation().createPath(BlockPos.containing(destination.add(-offset, 0, -offset)), 0);
        // Reject partial paths into a wall rather than treating them as a charge.
        return path != null && path.canReach() && shark.getNavigation().moveTo(path, speed);
    }
}
