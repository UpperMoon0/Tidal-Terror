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
public final class ProvinceStructureAudit {
    public static void verify(ServerLevel level, BlockPos center) {
        var generator = level.getChunkSource().getGenerator();
        var terrain = new ReefTerrain(level, generator);
        int tests = 0;
        int[] offsets = {0, 230, 340, 500, 800};
        var zones = new ReefProvinceLayout.Zone[]{ReefProvinceLayout.Zone.CATHEDRAL,
                ReefProvinceLayout.Zone.RIM, ReefProvinceLayout.Zone.INNER_WASTES,
                ReefProvinceLayout.Zone.OUTER_WASTES, ReefProvinceLayout.Zone.OCEAN};
        for (int i = 0; i < offsets.length; i++) {
            var site = new BlockPos(center.getX() + (int)Math.round(offsets[i] * ReefProvinceLayout.SCALE), 25, center.getZ());
            var sample = terrain.provinceSample(site.getX(), site.getZ());
            check(i == 4 ? sample == null : sample != null && sample.zone() == zones[i],
                    "Wrong structure fixture zone " + zones[i]);
            boolean inReef = terrain.province(site.getX(), site.getZ());
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
                        sum += terrain.province(x, z) ? terrain.anchorFloor(x, z) + 1
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
                check(!inReef || expected < level.getSeaLevel(),
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
        System.out.println("PROVINCE_AUDIT STRUCTURES PASS cases=" + tests
                + " rotatedWrecks=20 portals=5 zones=5 chunkClipping=true savedReload=true vanillaControl=true");
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

