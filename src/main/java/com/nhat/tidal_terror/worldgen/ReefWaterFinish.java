package com.nhat.tidal_terror.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
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
    private static final Map<ServerLevel,LinkedHashMap<ChunkPos,Integer>> PENDING=new IdentityHashMap<>();
    private static final int COLUMNS_PER_CHUNK=16*16;
    private static final int MAX_COLUMNS_PER_TICK=64;
    private static final int MAX_CHUNKS_INSPECTED_PER_TICK=32;
    private static final long MAX_NANOS_PER_TICK=3_000_000L;
    @SubscribeEvent public static void loaded(ChunkEvent.Load event){
        if(!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level))return;
        synchronized(PENDING){PENDING.computeIfAbsent(level,k->new LinkedHashMap<>()).putIfAbsent(event.getChunk().getPos(),0);}
    }
    @SubscribeEvent public static void read(ChunkDataEvent.Load event){
        var data=event.getData();
        // The old byte/boolean marker meant that no progress had been saved.
        // An integer records the next column, including zero for a fresh queue.
        int cursor;
        if(data.contains(MARKER,Tag.TAG_INT))cursor=data.getInt(MARKER);
        else if(data.contains(MARKER,Tag.TAG_BYTE) && data.getBoolean(MARKER))cursor=0;
        else return;
        if(cursor<0 || cursor>=COLUMNS_PER_CHUNK)return;
        var access=event.getChunk();
        var chunk=access instanceof net.minecraft.world.level.chunk.ImposterProtoChunk wrapped?wrapped.getWrapped():
                access instanceof net.minecraft.world.level.chunk.LevelChunk full?full:null;
        if(chunk!=null && chunk.getLevel() instanceof ServerLevel level)
            synchronized(PENDING){PENDING.computeIfAbsent(level,k->new LinkedHashMap<>()).putIfAbsent(chunk.getPos(),cursor);}
    }
    @SubscribeEvent public static void save(ChunkDataEvent.Save event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        synchronized(PENDING){
            var pending=PENDING.get(level);
            if(pending!=null && pending.containsKey(event.getChunk().getPos()))
                event.getData().putInt(MARKER,pending.get(event.getChunk().getPos()));
            else event.getData().remove(MARKER);
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){
        synchronized(PENDING){PENDING.keySet().removeIf(level->level.getServer()==event.getServer());}
    }
    private static void rotate(ServerLevel level,ChunkPos pos){
        synchronized(PENDING){
            var pending=PENDING.get(level);
            if(pending!=null && pending.containsKey(pos)){
                int cursor=pending.remove(pos);
                pending.put(pos,cursor);
            }
        }
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END || !(event.level instanceof ServerLevel level))return;
        List<ChunkPos> positions;
        synchronized(PENDING){
            var pending=PENDING.get(level);
            if(pending==null || pending.isEmpty())return;
            positions=new ArrayList<>();
            // Avoid O(all pending chunks) snapshots on every server tick.
            for(var pos:pending.keySet()){
                positions.add(pos);
                if(positions.size()>=MAX_CHUNKS_INSPECTED_PER_TICK)break;
            }
        }
        long started=System.nanoTime();
        int processed=0;
        var terrain=new ReefTerrain(level,level.getChunkSource().getGenerator());
        BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();
        for(ChunkPos pos:positions){
            if(processed>=MAX_COLUMNS_PER_TICK || (processed>0 && System.nanoTime()-started>=MAX_NANOS_PER_TICK))break;
            // FULL can precede postProcessGeneration. Do not cause synchronous chunk loads.
            var chunk=level.getChunkSource().getChunkNow(pos.x,pos.z);
            if(chunk==null || !chunk.getFullStatus().isOrAfter(net.minecraft.server.level.FullChunkStatus.BLOCK_TICKING)){
                rotate(level,pos);
                continue;
            }
            int cursor;
            synchronized(PENDING){
                var pending=PENDING.get(level);
                if(pending==null || !pending.containsKey(pos))continue;
                cursor=pending.get(pos);
            }
            while(cursor<COLUMNS_PER_CHUNK && processed<MAX_COLUMNS_PER_TICK
                    && (processed==0 || System.nanoTime()-started<MAX_NANOS_PER_TICK)){
                int x=pos.getMinBlockX()+(cursor/16),z=pos.getMinBlockZ()+(cursor%16);
                cursor++;
                processed++;
                if(!terrain.province(x,z))continue;
                int floor=level.getMinBuildHeight()-1;
                for(int y=level.getSeaLevel()-1;y>=level.getMinBuildHeight();y--){
                    p.set(x,y,z);
                    if(chunk.getBlockState(p).is(Blocks.SAND)){floor=y;break;}
                }
                if(floor<level.getMinBuildHeight())continue;
                for(int y=level.getMinBuildHeight();y<level.getSeaLevel();y++){
                    p.set(x,y,z);
                    var state=chunk.getBlockState(p);
                    if(state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.SOUL_SAND))
                        level.setBlock(p,Blocks.SANDSTONE.defaultBlockState(),2);
                    else if(y>floor && (state.isAir() || state.is(Blocks.BUBBLE_COLUMN)))
                        level.setBlock(p,Blocks.WATER.defaultBlockState(),2);
                }
            }
            synchronized(PENDING){
                var pending=PENDING.get(level);
                if(pending!=null && pending.containsKey(pos)){
                    if(cursor==COLUMNS_PER_CHUNK)pending.remove(pos);
                    else {pending.remove(pos);pending.put(pos,cursor);} // rotate to avoid starvation
                }
            }
            // Persist the blocks and their next-column cursor together until complete.
            chunk.setUnsaved(true);
        }
        if(processed>0 && Boolean.getBoolean("tidalterror.profileWaterFinish")){
            int remaining;
            synchronized(PENDING){var pending=PENDING.get(level);remaining=pending==null?0:pending.size();}
            System.out.println("REEF_WATER_FINISH_BUDGET columns="+processed+" remainingChunks="+remaining
                +" elapsedNanos="+(System.nanoTime()-started));
        }
    }
}
