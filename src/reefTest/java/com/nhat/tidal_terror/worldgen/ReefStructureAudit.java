package com.nhat.tidal_terror.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.RuinedPortalPiece;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces;

/** Real vanilla templates, processors, chunk clipping, loot, and saved pieces. */
public final class ReefStructureAudit {
    public static void verify(ServerLevel level, BlockPos center) {
        var generator = level.getChunkSource().getGenerator();
        var terrain = new ReefTerrain(level, generator);
        BlockPos reef = null, outside = null;
        for (int distance = 128; distance <= 2048; distance += 32) {
            int x = center.getX() + distance, z = center.getZ();
            if (reef == null && terrain.giant(x, z)) reef = new BlockPos(x, 25, z);
            if (outside == null && !terrain.reef(x, z)) outside = new BlockPos(x, 25, z);
            if (reef != null && outside != null) break;
        }
        check(reef != null && outside != null, "Missing structure test sites");
        int tests = 0;
        for (var site : new BlockPos[]{reef, outside}) {
            boolean inReef = terrain.reef(site.getX(), site.getZ());
            for (var rotation : Rotation.values()) {
                var ship = new ShipwreckPieces.ShipwreckPiece(level.getStructureManager(),
                        new ResourceLocation("minecraft", "shipwreck/with_mast"), site, rotation, false);
                var size = ship.template().getSize();
                // Vanilla uses the unrotated template's rectangle to average
                // heights; the injection changes only reef height samples.
                int sum = 0;
                for (int x = site.getX(); x < site.getX() + size.getX(); x++) {
                    for (int z = site.getZ(); z < site.getZ() + size.getZ(); z++) {
                        level.getChunk(x >> 4, z >> 4);
                        sum += terrain.reef(x, z) ? terrain.anchorFloor(x, z) + 1
                                : level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                    }
                }
                int expected = sum / (size.getX() * size.getZ());
                placeAcrossChunks(level, ship);
                check(ship.templatePosition().getY() == expected,
                        "Shipwreck height mismatch " + rotation + " reef=" + inReef);
                var saved = ship.createTag(StructurePieceSerializationContext.fromLevel(level));
                var restored = new ShipwreckPieces.ShipwreckPiece(level.getStructureManager(), saved);
                placeAcrossChunks(level, restored);
                check(restored.templatePosition().getY() == expected, "Reloaded wreck changed height");
                check(!inReef || Math.abs(expected - terrain.anchorFloor(site.getX(), site.getZ())) <= 6,
                        "Shipwreck suspended above seabed");
                tests++;
            }
            var id = new ResourceLocation("minecraft", "ruined_portal/portal_1");
            var template = level.getStructureManager().getOrCreate(id);
            var portal = new RuinedPortalPiece(level.getStructureManager(), site,
                    RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR, new RuinedPortalPiece.Properties(),
                    id, template, Rotation.NONE, Mirror.NONE,
                    new BlockPos(template.getSize().getX() / 2, 0, template.getSize().getZ() / 2));
            int expectedBottom = portal.getBoundingBox().minY();
            if (inReef) {
                expectedBottom = Integer.MAX_VALUE;
                var box = portal.getBoundingBox();
                for (int x = box.minX(); x <= box.maxX(); x++)
                    for (int z = box.minZ(); z <= box.maxZ(); z++)
                        expectedBottom = Math.min(expectedBottom, terrain.anchorFloor(x, z));
            }
            placeAcrossChunks(level, portal);
            check(portal.getBoundingBox().minY() == expectedBottom, "Portal height mismatch reef=" + inReef);
            var saved = portal.createTag(StructurePieceSerializationContext.fromLevel(level));
            var restored = new RuinedPortalPiece(level.getStructureManager(), saved);
            placeAcrossChunks(level, restored);
            check(restored.getBoundingBox().minY() == expectedBottom, "Reloaded portal changed height");
            if (inReef) {
                int obsidian = 0, netherrack = 0;
                var box = portal.getBoundingBox();
                for (int x = box.minX() - 14; x <= box.maxX() + 14; x++) {
                    for (int z = box.minZ() - 14; z <= box.maxZ() + 14; z++) {
                        for (int y = expectedBottom - 12; y <= expectedBottom + template.getSize().getY(); y++) {
                            var state = level.getBlockState(new BlockPos(x, y, z));
                            if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)) obsidian++;
                            if (state.is(Blocks.NETHERRACK)) {
                                netherrack++;
                                for (int below = y - 1; below > terrain.anchorFloor(x, z); below--) {
                                    check(!level.getBlockState(new BlockPos(x, below, z)).is(Blocks.WATER),
                                            "Unsupported portal foundation at " + new BlockPos(x, y, z));
                                }
                            }
                        }
                    }
                }
                check(obsidian > 0 && netherrack > 0, "Native portal blocks/foundation missing");
            }
            tests++;
        }
        System.out.println("REEF_AUDIT STRUCTURES PASS cases=" + tests
                + " rotatedWrecks=8 portals=2 chunkClipping=true savedReload=true vanillaControl=true");
    }

    private static void placeAcrossChunks(ServerLevel level, TemplateStructurePiece piece) {
        var box = piece.getBoundingBox();
        // Portal's native processor spreads its foundation from the center.
        // Each call gets the same full-height chunk bounds used by vanilla.
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                level.getChunk(x, z);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                        RandomSource.create(7152026 + x * 31L + z),
                        new BoundingBox(x * 16, level.getMinBuildHeight(), z * 16,
                                x * 16 + 15, level.getMaxBuildHeight() - 1, z * 16 + 15),
                        new ChunkPos(x, z), piece.templatePosition());
            }
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException(message);
    }
}
