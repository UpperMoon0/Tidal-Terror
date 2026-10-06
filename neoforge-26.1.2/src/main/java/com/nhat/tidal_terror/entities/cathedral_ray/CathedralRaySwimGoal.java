package com.nhat.tidal_terror.entities.cathedral_ray;

import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import static com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayEntity.Behavior.*;

/** A single movement owner, following vanilla water navigation rather than teleporting. */
public final class CathedralRaySwimGoal extends Goal {
    public static final int SAFE_DELAY = 200, HEAL_INTERVAL = 80, CURIOSITY_DURATION = 120;
    private final CathedralRayEntity ray;
    private LivingEntity attacker;
    private Vec3 rememberedDanger;
    private Player visitor;
    private int dangerMemory, quietTicks, healTicks, phaseTicks, curiosityCooldown, routeCooldown;

    public CathedralRaySwimGoal(CathedralRayEntity ray) {
        this.ray = ray;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean canUse() { return true; }
    @Override public boolean canContinueToUse() { return true; }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void stop() { ray.getNavigation().stop(); }

    public void onHurt(LivingEntity source) {
        attacker = source;
        rememberedDanger = source == null ? null : source.position();
        dangerMemory = 200;
        quietTicks = healTicks = 0;
        visitor = null;
        curiosityCooldown = 600;
        change(FLEE);
    }
    private void change(CathedralRayEntity.Behavior behavior) {
        if (ray.getBehavior() != behavior) {
            ray.setBehavior(behavior);
            phaseTicks = 0;
            routeCooldown = 0;
            ray.getNavigation().stop();
        }
    }
    private boolean active(LivingEntity entity) {
        return entity != null && entity.isAlive()
                && (!(entity instanceof Player player) || !player.isCreative() && !player.isSpectator());
    }
    private LivingEntity danger() {
        if (active(attacker) && ray.distanceToSqr(attacker) < 24 * 24 && dangerMemory > 0) return attacker;
        return ray.level().getEntitiesOfClass(CoralCrusherEntity.class, ray.getBoundingBox().inflate(18),
                shark -> shark.isAlive() && shark.isInWater() && ray.distanceToSqr(shark) < 18 * 18)
                .stream().min(Comparator.comparingDouble(ray::distanceToSqr)).orElse(null);
    }
    @Override public void tick() {
        phaseTicks++;
        if (routeCooldown > 0) routeCooldown--;
        if (curiosityCooldown > 0) curiosityCooldown--;
        if (dangerMemory > 0) dangerMemory--;
        LivingEntity threat = danger();
        if (threat != null) {
            rememberedDanger = threat.position();
            dangerMemory = 200;
        }
        boolean safe = ray.isInWater() && threat == null && dangerMemory == 0;
        if (!safe) quietTicks = healTicks = 0;
        else if (++quietTicks >= SAFE_DELAY && ++healTicks >= HEAL_INTERVAL) {
            ray.heal(1);
            healTicks = 0;
        }
        if (!ray.isInWater()) { ray.getNavigation().stop(); return; }
        // Damage and predators preempt every social behavior, at any health level.
        if (!safe) {
            visitor = null;
            curiosityCooldown = 600;
            change(FLEE);
            if (routeCooldown == 0 && (ray.getNavigation().isDone() || phaseTicks % 40 == 1)) flee();
            return;
        }
        if (ray.getBehavior() == FLEE && quietTicks < SAFE_DELAY) {
            if (routeCooldown == 0 && ray.getNavigation().isDone()) cruise(1.1);
            return;
        }
        if (ray.getBehavior() == FLEE) { rememberedDanger = null; attacker = null; change(CRUISE); }

        if (visitor != null && (!calm(visitor) || phaseTicks >= CURIOSITY_DURATION)) {
            visitor = null;
            curiosityCooldown = 600;
            change(CRUISE);
        }
        if (visitor == null && curiosityCooldown == 0) {
            visitor = ray.level().getEntitiesOfClass(Player.class, ray.getBoundingBox().inflate(10), this::calm)
                    .stream().min(Comparator.comparingDouble(ray::distanceToSqr)).orElse(null);
            if (visitor != null) change(CURIOUS);
        }
        if (visitor != null) {
            ray.getLookControl().setLookAt(visitor, 15, 10);
            double distance = ray.distanceToSqr(visitor);
            if (distance <= 6 * 6) ray.getNavigation().stop();
            else if (routeCooldown == 0 && (ray.getNavigation().isDone() || phaseTicks % 40 == 1)) {
                routeCooldown = 20;
                Vec3 offset = ray.position().subtract(visitor.position()).normalize().scale(6);
                navigate(visitor.position().add(offset), .65);
            }
            return;
        }
        var neighbors = ray.level().getEntitiesOfClass(CathedralRayEntity.class, ray.getBoundingBox().inflate(20),
                other -> other != ray && other.isAlive() && other.isInWater() && other.getBehavior() != FLEE
                        && ray.distanceToSqr(other) < 20 * 20);
        // Lower entity IDs form an acyclic leader hierarchy, with no reciprocal following.
        var leader = neighbors.stream().filter(other -> other.getId() < ray.getId())
                .min(Comparator.comparingInt(CathedralRayEntity::getId)).orElse(null);
        if (leader != null) {
            change(SCHOOL);
            if (routeCooldown == 0 && (ray.getNavigation().isDone() || phaseTicks % 40 == 1)) {
                routeCooldown = 20;
                Vec3 separation = Vec3.ZERO;
                for (var other : neighbors) {
                    Vec3 away = ray.position().subtract(other.position());
                    if (away.lengthSqr() < 6 * 6)
                        separation = separation.add(away.lengthSqr() < .01
                                ? new Vec3(ray.getId() > other.getId() ? 1 : -1, 0, 0) : away.normalize());
                }
                Vec3 heading = leader.getDeltaMovement().multiply(1, 0, 1);
                if (heading.lengthSqr() < .001) heading = new Vec3(0, 0, 1);
                heading = heading.normalize();
                Vec3 side = new Vec3(heading.z, 0, -heading.x).scale((ray.getId() % 2 == 0 ? 1 : -1) * 6);
                Vec3 destination = leader.position().subtract(heading.scale(4)).add(side).add(separation.scale(4));
                Vec3 fromLeader = destination.subtract(leader.position());
                if (fromLeader.lengthSqr() < .01) fromLeader = side;
                if (fromLeader.lengthSqr() < 6.5 * 6.5)
                    destination = leader.position().add(fromLeader.normalize().scale(6.5));
                if (!navigate(destination, .85) && ray.getNavigation().isDone()) cruise(.85);
            }
        } else {
            change(CRUISE);
            if (routeCooldown == 0 && (ray.getNavigation().isDone() || phaseTicks % 180 == 1)) cruise(.85);
        }
    }
    private boolean calm(Player player) {
        return active(player) && player.isInWater() && player.getDeltaMovement().lengthSqr() < .01
                && ray.distanceToSqr(player) <= 10 * 10 && ray.hasLineOfSight(player);
    }
    private void flee() {
        routeCooldown = 20;
        Vec3 direction = rememberedDanger == null ? new Vec3(1, 0, 0)
                : ray.position().subtract(rememberedDanger).multiply(1, 0, 1);
        if (direction.lengthSqr() < .01) direction = new Vec3(1, 0, 0);
        direction = direction.normalize();
        for (int i = 0; i < 12; i++) {
            double angle = (i % 2 == 0 ? 1 : -1) * (i / 2) * .22;
            Vec3 rotated = new Vec3(direction.x * Math.cos(angle) - direction.z * Math.sin(angle),
                    i % 3 - 1, direction.x * Math.sin(angle) + direction.z * Math.cos(angle));
            Vec3 destination = ray.position().add(rotated.scale(6));
            if (rememberedDanger != null && destination.distanceToSqr(rememberedDanger)
                    <= ray.position().distanceToSqr(rememberedDanger)) continue;
            if (navigate(destination, 1.3)) return;
        }
        ray.getNavigation().stop();
    }
    private void cruise(double speed) {
        routeCooldown = 40;
        for (int i = 0; i < 24; i++) {
            Vec3 destination = ray.position().add(ray.getRandom().nextInt(25) - 12,
                    ray.getRandom().nextInt(7) - 3, ray.getRandom().nextInt(25) - 12);
            if (destination.distanceToSqr(ray.position()) >= 6 * 6 && navigate(destination, speed)) return;
        }
    }
    private boolean clearance(Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        return ray.level().hasChunkAt(pos) && ray.level().getFluidState(pos).is(FluidTags.WATER)
                && ray.level().getFluidState(pos.above()).is(FluidTags.WATER)
                && ray.level().getFluidState(pos.below()).is(FluidTags.WATER)
                && ray.level().noCollision(ray, ray.getBoundingBox().move(point.subtract(ray.position())).inflate(.25, .25, .25));
    }
    private boolean navigate(Vec3 destination, double speed) {
        if (!clearance(destination)) return false;
        // SwimNodeEvaluator addresses the lower corner of the mob's footprint;
        // Path#getEntityPosAtNode restores this width offset when steering.
        double offset = (int)(ray.getBbWidth() + 1) * .5;
        Path path = ray.getNavigation().createPath(BlockPos.containing(destination.add(-offset, 0, -offset)), 0);
        if (path == null || !path.canReach()) return false;
        Vec3 previous = ray.position();
        for (int i = 0; i < path.getNodeCount(); i++) {
            Vec3 point = path.getEntityPosAtNode(ray, i);
            int steps = Math.max(1, (int)Math.ceil(previous.distanceTo(point)));
            for (int step = 1; step <= steps; step++)
                if (!clearance(previous.lerp(point, (double)step / steps))) return false;
            previous = point;
        }
        return ray.getNavigation().moveTo(path, speed);
    }
}
