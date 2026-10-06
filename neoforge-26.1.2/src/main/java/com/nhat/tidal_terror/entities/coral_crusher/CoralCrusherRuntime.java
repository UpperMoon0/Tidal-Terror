package com.nhat.tidal_terror.entities.coral_crusher;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
/** Native API differences only; behavior and tuning live in the shared goal. */
final class CoralCrusherRuntime {
    static boolean isDrowned(LivingEntity entity) { return entity instanceof net.minecraft.world.entity.monster.zombie.Drowned; }
    static boolean inWater(LivingEntity entity) { return entity.isInWater(); }
    static void bite(CoralCrusherEntity shark,LivingEntity target) { shark.doHurtTarget((net.minecraft.server.level.ServerLevel)shark.level(),target); }
    static int minY(CoralCrusherEntity shark) { return shark.level().getMinY(); }
    static AABB collisionBox(CoralCrusherEntity shark,Vec3 destination) { return shark.getBoundingBox().move(destination.subtract(shark.position())); }
    private CoralCrusherRuntime() {}
}
