package com.nhat.tidal_terror.entities.coral_crusher;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
/** Native API differences only; behavior and tuning live in the shared goal. */
final class CoralCrusherRuntime {
    static boolean isDrowned(LivingEntity entity) { return entity instanceof net.minecraft.world.entity.monster.Drowned; }
    static boolean inWater(LivingEntity entity) { return entity.isInWaterOrBubble(); }
    static void bite(CoralCrusherEntity shark,LivingEntity target) { shark.doHurtTarget(target); }
    static int minY(CoralCrusherEntity shark) { return shark.level().getMinBuildHeight(); }
    static AABB collisionBox(CoralCrusherEntity shark,Vec3 destination) { return shark.getType().getAABB(destination.x,destination.y,destination.z); }
    private CoralCrusherRuntime() {}
}
