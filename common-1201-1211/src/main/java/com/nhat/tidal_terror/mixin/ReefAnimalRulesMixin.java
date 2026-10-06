package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.ReefAnimalSpawns;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
public abstract class ReefAnimalRulesMixin {
    @Inject(method="checkSpawnRules",at=@At("RETURN"),cancellable=true)
    private static void underwater(EntityType<?> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random,CallbackInfoReturnable<Boolean> ci) {
        if(!ci.getReturnValueZ() && reason==MobSpawnType.NATURAL
            && (type==EntityType.DOLPHIN||type==EntityType.TROPICAL_FISH||type==EntityType.TURTLE)
            && ReefAnimalSpawns.deepWater(level,pos))ci.setReturnValue(true);
    }
}
