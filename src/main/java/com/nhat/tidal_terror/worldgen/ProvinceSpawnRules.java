package com.nhat.tidal_terror.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;

final class ProvinceSpawnRules {
    static boolean allowed(ServerLevelAccessor access, BlockPos pos, boolean predator) {
        var level = access.getLevel();
        if(ReefSpawnHabitat.deep(level))return ReefSpawnHabitat.allowed(level,pos);
        var terrain = new ReefTerrain(level, level.getChunkSource().getGenerator());
        if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof ReefProvinceAccess)) return true;
        var s = terrain.provinceSample(pos.getX(), pos.getZ());
        if (s == null) return false;
        if (predator && (s.zone() == ReefProvinceLayout.Zone.OUTER_WASTES || s.zone() == ReefProvinceLayout.Zone.INNER_WASTES)) return false;
        int floor = terrain.floor(pos.getX(), pos.getZ());
        return pos.getY() <= floor + (level.getSeaLevel() - floor) * .65;
    }
    private ProvinceSpawnRules() {}
}
