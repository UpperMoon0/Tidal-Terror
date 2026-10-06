package com.nhat.tidal_terror.entities.cathedral_ray;

import com.nhat.tidal_terror.entities.ReefNavigation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Native swim paths with whole-wing clearance and precise corner following. */
public final class RayWaterNavigation extends WaterBoundPathNavigation {
    public RayWaterNavigation(Mob mob, Level level) { super(mob, level); }

    public static boolean clearSegment(Mob mob, Vec3 start, Vec3 end) {
        int steps = Math.max(1, (int)Math.ceil(start.distanceTo(end) * 4));
        for (int i = 0; i <= steps; i++)
            if (!ReefNavigation.clear(mob, start.lerp(end, (double)i / steps), false)) return false;
        return true;
    }

    @Override protected boolean canMoveDirectly(Vec3 start, Vec3 end) {
        // Native navigation supplies a body-center origin and a feet destination.
        return clearSegment(mob, start.add(0, -mob.getBbHeight() * .5, 0), end);
    }

    @Override protected void followThePath() {
        // Vanilla's width/2 tolerance can skip corners over two blocks early.
        Vec3 next = path.getNextEntityPos(mob);
        if (Math.abs(mob.getX() - next.x) < .4 && Math.abs(mob.getZ() - next.z) < .4
                && Math.abs(mob.getY() - next.y) < .5) path.advance();
        doStuckDetection(getTempMobPos());
    }
}
