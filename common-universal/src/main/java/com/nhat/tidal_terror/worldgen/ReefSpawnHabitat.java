package com.nhat.tidal_terror.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Deep custom fauna occupy the giant-coral canopy, not the empty water above it. */
public final class ReefSpawnHabitat {
    private record ColumnKey(long seed,net.minecraft.world.level.chunk.ChunkGenerator generator,int x,int z) {}
    private static final ChunkColumnCache<ColumnKey> CANOPY=new ChunkColumnCache<>(2048);
    public static boolean deep(ServerLevel level) {
        return level.getChunkSource().getGenerator().getBiomeSource() instanceof ReefProvinceAccess access && access.deep();
    }

    public static int ceiling(ServerLevel level, ReefTerrain terrain, int x, int z) {
        return CANOPY.get(new ColumnKey(level.getSeed(),level.getChunkSource().getGenerator(),x>>4,z>>4),
                (x&15)*16+(z&15),()->calculateCeiling(level,terrain,x,z));
    }
    private static int calculateCeiling(ServerLevel level, ReefTerrain terrain, int x, int z) {
        int top=terrain.floor(x,z);
        // Same seeded anchors, heights and spacing as the giant decorator. This
        // defines a local canopy envelope without scanning hundreds of water blocks.
        for(int gx=Math.floorDiv(x-88,88);gx<=Math.floorDiv(x+88,88);gx++)
            for(int gz=Math.floorDiv(z-88,88);gz<=Math.floorDiv(z+88,88);gz++) {
                var r=new java.util.Random(CoralCathedralFeature.seed(level.getSeed(),gx,gz));
                int cx=gx*88+44+r.nextInt(13)-6,cz=gz*88+44+r.nextInt(13)-6;
                if(!terrain.giant(cx,cz))continue;
                int base=terrain.anchorFloor(cx,cz)+1;
                r.nextInt(3); // style precedes height in the decorator's seed stream
                int height=Math.min(level.getSeaLevel()-base-2-r.nextInt(5),110-r.nextInt(5));
                if(Math.floorMod(gx+gz,3)!=0)height-=12+r.nextInt(20);
                top=Math.max(top,base+height-1);
            }
        return top;
    }

    public static boolean allowed(ServerLevel level, BlockPos pos) {
        if(!deep(level))return true;
        var terrain=new ReefTerrain(level,level.getChunkSource().getGenerator());
        var sample=terrain.provinceSample(pos.getX(),pos.getZ());
        return sample!=null && sample.zone()==ReefProvinceLayout.Zone.CATHEDRAL
                && pos.getY()>terrain.floor(pos.getX(),pos.getZ())
                && pos.getY()<=ceiling(level,terrain,pos.getX(),pos.getZ());
    }
    private ReefSpawnHabitat() {}
}
