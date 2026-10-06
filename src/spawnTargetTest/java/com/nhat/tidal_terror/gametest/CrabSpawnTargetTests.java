package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.worldgen.CrabSpawnTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;

/** Exercise the native spawner, not just the target-selection helper. */
public final class CrabSpawnTargetTests {
    public static void verify(GameTestHelper h, Holder<Biome> reef, java.util.function.Function<net.minecraft.server.level.ServerLevel,net.minecraft.server.level.ServerPlayer> visitor) {
        // The flat GameTest Overworld's sea level is -63, below any valid deep-water
        // spawn. Use an isolated synthetic pool in the native Nether (sea level 32)
        // so the production depth rule can run unchanged.
        var level = h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        // Keep this synthetic habitat away from the parallel AI fixtures.
        var center = h.absolutePos(new BlockPos(512, 0, 512));
        var chunk = level.getChunkAt(center);
        int x = chunk.getPos().getMinBlockX(), z = chunk.getPos().getMinBlockZ(), y = level.getSeaLevel()-16;
        for(int cx=-1;cx<=1;cx++) for(int cz=-1;cz<=1;cz++)
            level.getChunkAt(new BlockPos(x+cx*16,y,z+cz*16)).fillBiomesFromNoise(
                    (qx,qy,qz,sampler)->reef, level.getChunkSource().randomState().sampler());
        for (int dx=-16; dx<32; dx++) for (int dz=-16; dz<32; dz++) {
            for (int dy=-1;dy<level.getHeight()-y;dy++) level.setBlock(new BlockPos(x+dx,y+dy,z+dz),
                    (dy==-1?Blocks.SANDSTONE:dy<=24?Blocks.WATER:Blocks.AIR).defaultBlockState(), 2);
        }
        var player = visitor.apply(level);
        player.setPos(x+48.5,y+4,z+8.5);
        var sampled = new BlockPos(x+8,y+12,z+8);
        var floor = sampled.atY(y);
        h.assertTrue(CrabSpawnTarget.position(ModEntities.SHARDBACK_POOL,level,sampled).equals(floor), "Crab sampled open water instead of the floor");
        for (var pool:ModEntities.reefPools()) if(pool!=ModEntities.SHARDBACK_POOL)
            h.assertTrue(CrabSpawnTarget.position(pool,level,sampled).equals(sampled), "Changed another species' height");
        int[] spawned={0}; int successful=0;
        level.getRandom().setSeed(20261006L);
        for(int attempt=0;attempt<64;attempt++) {
            int before=spawned[0];
            NaturalSpawner.spawnCategoryForChunk(ModEntities.SHARDBACK_POOL,level,chunk,(type,pos,c)->true,(mob,c)->{
                h.assertTrue(Math.abs(mob.getY()-y)<.001 && level.noCollision(mob), "Crab spawned floating or clipping");
                spawned[0]++;mob.discard();
            });
            if(spawned[0]>before)successful++;
        }
        System.out.println("TIDAL_CRAB_SPAWN: successful="+successful+"/64 spawned="+spawned[0]);
        h.assertTrue(successful>=50,"Seabed attempts still mostly fail: "+successful+"/64");
        int before=spawned[0];
        for(int attempt=0;attempt<16;attempt++) NaturalSpawner.spawnCategoryForChunk(ModEntities.SHARDBACK_POOL,level,chunk,
                (type,pos,c)->false,(mob,c)->{spawned[0]++;mob.discard();});
        h.assertTrue(spawned[0]==before,"Bypassed the native extra spawn/cap predicate");
        for(int dx=-16;dx<32;dx++)for(int dz=-16;dz<32;dz++)
            level.setBlock(new BlockPos(x+dx,y+1,z+dz),Blocks.STONE.defaultBlockState(),2);
        for(int attempt=0;attempt<16;attempt++) NaturalSpawner.spawnCategoryForChunk(ModEntities.SHARDBACK_POOL,level,chunk,
                (type,pos,c)->true,(mob,c)->{spawned[0]++;mob.discard();});
        h.assertTrue(spawned[0]==before,"Crabs spawned inside blocked headroom");
        level.setBlock(floor.above(),Blocks.WATER.defaultBlockState(),2);
        level.setBlock(floor.below(),Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(CrabSpawnTarget.position(ModEntities.SHARDBACK_POOL,level,sampled).equals(sampled),"Targeted unsuitable substrate");
        level.setBlock(floor.below(),Blocks.SANDSTONE.defaultBlockState(),2);
        level.setBlock(floor,Blocks.AIR.defaultBlockState(),2);
        h.assertTrue(CrabSpawnTarget.position(ModEntities.SHARDBACK_POOL,level,sampled).equals(sampled),"Targeted a dry floor");
        player.discard();
        h.succeed();
    }
    private CrabSpawnTargetTests() {}
}
