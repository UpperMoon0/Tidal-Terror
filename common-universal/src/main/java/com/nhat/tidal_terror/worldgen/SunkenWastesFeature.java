package com.nhat.tidal_terror.worldgen;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Sparse half-buried coral skeletons, written only inside the decorating chunk. */
public final class SunkenWastesFeature extends Feature<NoneFeatureConfiguration> {
    public SunkenWastesFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
        var level = c.level(); var terrain = new ReefTerrain(level, c.chunkGenerator());
        int mx = c.origin().getX() & ~15, mz = c.origin().getZ() & ~15;
        boolean placed = false;
        for (int gx = Math.floorDiv(mx - 40, 144); gx <= Math.floorDiv(mx + 55, 144); gx++)
            for (int gz = Math.floorDiv(mz - 40, 144); gz <= Math.floorDiv(mz + 55, 144); gz++) {
                long seed = CoralCathedralFeature.seed(level.getSeed() ^ 0xdead5eaL, gx, gz);
                Random r = new Random(seed);
                if (r.nextDouble() > .24) continue;
                int cx = gx * 144 + 72 + r.nextInt(25) - 12, cz = gz * 144 + 72 + r.nextInt(25) - 12;
                var sample = terrain.provinceSample(cx, cz);
                if (sample == null || sample.zone() == ReefProvinceLayout.Zone.CATHEDRAL) continue;
                int base = terrain.anchorFloor(cx, cz) - 4;
                var plan = CoralGeometry.build(seed, 16 + r.nextInt(13), r.nextInt(3));
                for (var e : plan.blocks().entrySet()) {
                    var v = e.getKey(); int x = cx + v.x(), z = cz + v.z(), y = base + v.y();
                    if (x < mx || x >= mx + 16 || z < mz || z >= mz + 16 || y >= level.getSeaLevel()) continue;
                    var local = terrain.provinceSample(x, z);
                    if (local == null || local.zone() == ReefProvinceLayout.Zone.CATHEDRAL || y <= terrain.floor(x,z)) continue;
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(Blocks.WATER)) continue;
                    level.setBlock(pos, e.getValue() < 0 ? Blocks.SMOOTH_SANDSTONE.defaultBlockState()
                            : Blocks.DEAD_BRAIN_CORAL_BLOCK.defaultBlockState(), 2);
                    placed = true;
                }
            }
        return placed;
    }
}
