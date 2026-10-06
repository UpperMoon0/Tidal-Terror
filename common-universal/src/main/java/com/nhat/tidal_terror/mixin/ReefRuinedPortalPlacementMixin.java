package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.ReefTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.RuinedPortalPiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RuinedPortalPiece.class)
public abstract class ReefRuinedPortalPlacementMixin {
    @Unique private ReefTerrain tidalterror$terrain;

    @Shadow private static int getSurfaceY(LevelAccessor level, int x, int z,
            RuinedPortalPiece.VerticalPlacement placement) { throw new AssertionError(); }

    @Inject(method = "postProcess", at = @At("HEAD"))
    private void tidalterror$seabed(WorldGenLevel level, StructureManager structures,
            ChunkGenerator generator, RandomSource random, BoundingBox bounds,
            ChunkPos chunk, BlockPos pivot, CallbackInfo ci) {
        this.tidalterror$terrain = new ReefTerrain(level, generator);
        var piece = (TemplateStructurePiece) (Object) this;
        var box = piece.getBoundingBox();
        var center = box.getCenter();
        if (!this.tidalterror$terrain.reef(center.getX(), center.getZ())) return;
        // Bury the foundation at the lowest reef column under the footprint,
        // rather than balancing a flat platform above sloping sand or corals.
        int bottom = Integer.MAX_VALUE;
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (this.tidalterror$terrain.reef(x, z))
                    bottom = Math.min(bottom, this.tidalterror$terrain.anchorFloor(x, z));
            }
        }
        piece.move(0, bottom - box.minY(), 0);
    }

    @Redirect(method = "spreadNetherrack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/structures/RuinedPortalPiece;getSurfaceY(Lnet/minecraft/world/level/LevelAccessor;IILnet/minecraft/world/level/levelgen/structure/structures/RuinedPortalPiece$VerticalPlacement;)I"))
    private int tidalterror$foundation(LevelAccessor level, int x, int z,
            RuinedPortalPiece.VerticalPlacement placement) {
        return this.tidalterror$terrain.reef(x, z)
                ? this.tidalterror$terrain.anchorFloor(x, z) : getSurfaceY(level, x, z, placement);
    }

    @Inject(method = "postProcess", at = @At("TAIL"))
    private void tidalterror$groundFoundation(WorldGenLevel level, StructureManager structures,
            ChunkGenerator generator, RandomSource random, BoundingBox bounds,
            ChunkPos chunk, BlockPos pivot, CallbackInfo ci) {
        var piece = (TemplateStructurePiece) (Object) this;
        var box = piece.getBoundingBox();
        var center = box.getCenter();
        if (!this.tidalterror$terrain.reef(center.getX(), center.getZ())) return;
        // Vanilla's random drip columns can end in water on a sloping reef.
        // Extend only the bottom of its netherrack foundation to sediment.
        var cursor = new BlockPos.MutableBlockPos();
        for (int x = center.getX() - 14; x <= center.getX() + 14; x++) {
            for (int z = center.getZ() - 14; z <= center.getZ() + 14; z++) {
                if (!this.tidalterror$terrain.reef(x, z)) continue;
                int floor = this.tidalterror$terrain.anchorFloor(x, z);
                for (int y = floor + 1; y <= box.maxY(); y++) {
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).is(Blocks.NETHERRACK)) continue;
                    for (int below = y - 1; below > floor; below--) {
                        cursor.set(x, below, z);
                        if (!level.getBlockState(cursor).is(Blocks.WATER)) break;
                        level.setBlock(cursor, Blocks.NETHERRACK.defaultBlockState(), 2);
                    }
                    break;
                }
            }
        }
    }
}
