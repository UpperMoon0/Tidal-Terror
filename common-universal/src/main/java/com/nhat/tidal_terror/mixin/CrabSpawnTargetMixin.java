package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.CrabSpawnTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NaturalSpawner.class)
public abstract class CrabSpawnTargetMixin {
    @Shadow private static BlockPos getRandomPosWithin(Level level, LevelChunk chunk) { throw new AssertionError(); }

    @Redirect(method="spawnCategoryForChunk", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/NaturalSpawner;getRandomPosWithin(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/chunk/LevelChunk;)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos seabedStart(Level sampledLevel, LevelChunk sampledChunk,
            MobCategory category, ServerLevel level, LevelChunk chunk,
            NaturalSpawner.SpawnPredicate predicate, NaturalSpawner.AfterSpawnCallback callback) {
        return CrabSpawnTarget.position(category, level, getRandomPosWithin(sampledLevel, sampledChunk));
    }

}
