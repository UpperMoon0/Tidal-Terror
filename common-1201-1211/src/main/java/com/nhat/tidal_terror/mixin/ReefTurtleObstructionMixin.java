package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.ReefAnimalSpawns;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Match Forge's natural deep-reef turtle position exception, preserving collisions and despawn range. */
@Mixin(NaturalSpawner.class)
public abstract class ReefTurtleObstructionMixin {
    @Inject(method="isValidPositionForMob", at=@At("RETURN"), cancellable=true)
    private static void reefTurtle(ServerLevel level, Mob mob, double distance, CallbackInfoReturnable<Boolean> result) {
        if (!result.getReturnValueZ() && mob instanceof Turtle
                && !(distance > mob.getType().getCategory().getDespawnDistance() * mob.getType().getCategory().getDespawnDistance() && mob.removeWhenFarAway(distance))
                && ReefAnimalSpawns.deepWater(level, mob.blockPosition())
                && level.noCollision(mob) && level.isUnobstructed(mob)) result.setReturnValue(true);
    }
}
