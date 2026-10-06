package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.ReefAnimalSpawns;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
public abstract class ReefTurtlePlacementMixin {
    @Inject(method = "isSpawnPositionOk", at = @At("RETURN"), cancellable = true)
    private static void tidalterror$underwaterTurtles(EntityType<?> type, LevelReader level,
            BlockPos pos, CallbackInfoReturnable<Boolean> result) {
        if (!result.getReturnValueZ() && type == EntityType.TURTLE
                && ReefAnimalSpawns.deepWater(level, pos))
            result.setReturnValue(true);
    }
}
