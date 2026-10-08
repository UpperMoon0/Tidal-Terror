package com.nhat.tidal_terror.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Finish newly generated reef water after native cross-chunk postprocessing. */
@Mod.EventBusSubscriber(modid="tidalterror")
public final class ReefWaterFinish {
    private static final String MARKER="tidalterror:unfinished_reef_water";
    private static final Map<ServerLevel,Set<ChunkPos>> PENDING=new IdentityHashMap<>();
    @SubscribeEvent public static void loaded(ChunkEvent.Load event){
        if(!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level))return;
        synchronized(PENDING){PENDING.computeIfAbsent(level,k->new HashSet<>()).add(event.getChunk().getPos());}
    }
    @SubscribeEvent public static void read(ChunkDataEvent.Load event){
        if(!event.getData().getBoolean(MARKER))return;
        var access=event.getChunk();
        var chunk=access instanceof net.minecraft.world.level.chunk.ImposterProtoChunk wrapped?wrapped.getWrapped():
                access instanceof net.minecraft.world.level.chunk.LevelChunk full?full:null;
        if(chunk!=null && chunk.getLevel() instanceof ServerLevel level)
            synchronized(PENDING){PENDING.computeIfAbsent(level,k->new HashSet<>()).add(chunk.getPos());}
    }
    @SubscribeEvent public static void save(ChunkDataEvent.Save event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        synchronized(PENDING){
            var pending=PENDING.get(level);
            if(pending!=null && pending.contains(event.getChunk().getPos()))event.getData().putBoolean(MARKER,true);
            else event.getData().remove(MARKER);
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){
        synchronized(PENDING){PENDING.keySet().removeIf(level->level.getServer()==event.getServer());}
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END || !(event.level instanceof ServerLevel level))return;
        List<ChunkPos> positions;
        synchronized(PENDING){var pending=PENDING.get(level);if(pending==null || pending.isEmpty())return;positions=new ArrayList<>(pending);}
        var terrain=new ReefTerrain(level,level.getChunkSource().getGenerator());
        for(ChunkPos pos:positions){
            // Forge Load can fire before FULL. Never request another chunk from
            // that callback; wait for the nonblocking native FULL-chunk lookup.
            var chunk=level.getChunkSource().getChunkNow(pos.x,pos.z);
            // ChunkMap.prepareTickingChunk invokes postProcessGeneration before
            // BLOCK_TICKING. FULL alone is too early to finish the water.
            if(chunk==null || !chunk.getFullStatus().isOrAfter(net.minecraft.server.level.FullChunkStatus.BLOCK_TICKING))continue;
            BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();
            for(int x=pos.getMinBlockX();x<=pos.getMaxBlockX();x++)for(int z=pos.getMinBlockZ();z<=pos.getMaxBlockZ();z++){
                if(!terrain.province(x,z))continue;
                // Read the generated sediment, including after reload. Native
                // noise-height reconstruction is expensive and is unnecessary
                // for a basin whose continuous sand layer already exists.
                int floor=level.getMinBuildHeight()-1;
                for(int y=level.getSeaLevel()-1;y>=level.getMinBuildHeight();y--){
                    p.set(x,y,z);if(chunk.getBlockState(p).is(Blocks.SAND)){floor=y;break;}
                }
                if(floor<level.getMinBuildHeight())continue;
                for(int y=level.getMinBuildHeight();y<level.getSeaLevel();y++){
                    p.set(x,y,z);
                    // Source water is stable; flags 2 updates clients without
                    // requesting neighbour chunks through block notifications.
                    var state=chunk.getBlockState(p);
                    // UnderwaterMagmaFeature checks blocks throughout its
                    // radius, without a biome test at every written position.
                    if(state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.SOUL_SAND))
                        level.setBlock(p,Blocks.SANDSTONE.defaultBlockState(),2);
                    else if(y>floor && (state.isAir() || state.is(Blocks.BUBBLE_COLUMN)))
                        level.setBlock(p,Blocks.WATER.defaultBlockState(),2);
                }
            }
            synchronized(PENDING){var pending=PENDING.get(level);if(pending!=null)pending.remove(pos);}
            chunk.setUnsaved(true);
        }
    }
}
