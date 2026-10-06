package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.ReefTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShipwreckPieces.ShipwreckPiece.class)
public abstract class ReefShipwreckPlacementMixin {
    @Unique private ReefTerrain tidalterror$terrain;

    @Inject(method = "postProcess", at = @At("HEAD"))
    private void tidalterror$prepare(WorldGenLevel level, StructureManager structures,
            ChunkGenerator generator, RandomSource random, BoundingBox bounds,
            ChunkPos chunk, BlockPos pivot, CallbackInfo ci) {
        this.tidalterror$terrain = new ReefTerrain(level, generator);
    }

    @Redirect(method = "postProcess", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/WorldGenLevel;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    private int tidalterror$seabed(WorldGenLevel level, Heightmap.Types type, int x, int z) {
        // Keep vanilla's averaging, beach burial, rotations, and loot handling.
        // The coordinate-only anchor is stable across all intersecting chunks.
        return this.tidalterror$terrain.reef(x, z)
                ? this.tidalterror$terrain.anchorFloor(x, z) + 1 : level.getHeight(type, x, z);
    }
}
