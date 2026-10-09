package com.nhat.tidal_terror.worldgen;

import com.nstut.endless.vertical.EndlessVerticalEngine;
import com.nstut.endless.vertical.VerticalPagePos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;
import java.nio.file.*;
import java.util.*;

/** Two-JVM checkpoints where sparse/native saves and water finishing are incomplete. */
final class InterruptedGenerationAudit {
    private static final String MARKER="tidalterror:unfinished_reef_water";
    private static final Path PROTO=Path.of("interrupted-proto.nbt");
    private static final Path WATER=Path.of("interrupted-water.nbt");
    private static ChunkPos waterPos;

    private static void require(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }

    @SuppressWarnings("unchecked")
    private static Map<ServerLevel,LinkedHashMap<ChunkPos,Integer>> queues() throws ReflectiveOperationException {
        var field=ReefWaterFinish.class.getDeclaredField("PENDING");field.setAccessible(true);
        return (Map<ServerLevel,LinkedHashMap<ChunkPos,Integer>>)field.get(null);
    }

    static void start(ServerLevel level,ReefProvinceLayout.Center center,boolean reload) throws Exception {
        verifySparseCheckpoint(level,center,reload);
        verifyLegacyMarkers(level);
        // Away from the terrain/spawn witnesses; this part of the outer Wastes
        // has a dense sand floor so it exercises the native repair loop.
        waterPos=new ChunkPos((center.x()+(int)(495*ReefProvinceLayout.SCALE))>>4,(center.z()+256)>>4);
        level.setChunkForced(waterPos.x,waterPos.z,true);
        level.getChunk(waterPos.x,waterPos.z);
        if(reload) {
            var data=NbtIo.readCompressed(WATER.toFile());
            require(data.contains(MARKER,Tag.TAG_INT),"Water checkpoint did not persist an integer cursor");
            int cursor=data.getInt(MARKER);
            require(cursor>0 && cursor<256,"Water checkpoint was not partial");
            // Read native NBT in this fresh JVM, independently of forced-chunk
            // startup ordering, then let normal END ticks finish the real chunk.
            var queues=queues();
            synchronized(queues) { queues.get(level).remove(waterPos); }
            var restored=ChunkSerializer.read(level,level.getPoiManager(),waterPos,data);
            require(restored instanceof ImposterProtoChunk,"Water checkpoint is not a native FULL chunk");
            synchronized(queues) {
                require(Objects.equals(queues.get(level).get(waterPos),cursor),"Cold load lost the partial repair cursor");
            }
            verifyPlayerEdits(((ImposterProtoChunk)restored).getWrapped());
        }
    }

    private static void verifySparseCheckpoint(ServerLevel level,ReefProvinceLayout.Center center,boolean reload) throws Exception {
        // These detached native chunks are never registered in ChunkMap. Only
        // the FEATURES proto NBT is saved, never the converted FULL chunk.
        var pos=new ChunkPos(center.x()>>4,(center.z()+512)>>4);
        var pagePos=new VerticalPagePos(pos.x,-1,pos.z);
        var vertical=EndlessVerticalEngine.world(level);
        require(!vertical.loadedPageYs(pos.x,pos.z).contains(-1),"Sparse checkpoint was already resident");
        require(vertical.pageExists(pagePos)==reload,"Unexpected sparse checkpoint persistence state");
        if(!reload) {
            var proto=new ProtoChunk(pos,UpgradeData.EMPTY,level,level.registryAccess().registryOrThrow(Registries.BIOME),null);
            proto.setStatus(ChunkStatus.FEATURES);
            NbtIo.writeCompressed(ChunkSerializer.write(level,proto),PROTO.toFile());
        }
        var data=NbtIo.readCompressed(PROTO.toFile());
        require(!data.getString("Status").equals("minecraft:full"),"Fixture accidentally saved native FULL storage");
        var terrain=new ReefTerrain(level,level.getChunkSource().getGenerator());
        var edit=new BlockPos(pos.getMinBlockX(),terrain.floor(pos.getMinBlockX(),pos.getMinBlockZ())+8,pos.getMinBlockZ());
        admitProto(level,pos,data);
        if(!reload) {
            vertical.setBlockState(edit,Blocks.GOLD_BLOCK.defaultBlockState());
            vertical.setBlockState(edit.above(),Blocks.AIR.defaultBlockState());
            // An intentionally removed entire section is authoritative too.
            for(int x=0;x<16;x++)for(int y=-80;y<-64;y++)for(int z=0;z<16;z++)
                vertical.setBlockState(new BlockPos(pos.getMinBlockX()+x,y,pos.getMinBlockZ()+z),Blocks.AIR.defaultBlockState());
        }
        verifySparseEdits(level,pos,edit);
        // Re-admission also preserves pages that are already loaded in memory.
        admitProto(level,pos,data);
        verifySparseEdits(level,pos,edit);
        if(!reload) {
            vertical.flushDirty(); // Sparse storage commits ahead of native FULL persistence.
            vertical.unloadColumn(pos.x,pos.z);
            require(vertical.pageExists(pagePos),"Sparse checkpoint failed to reach disk");
            require(!vertical.loadedPageYs(pos.x,pos.z).contains(-1),"Sparse checkpoint failed to evict");
        }
        System.out.println("DEEP_INTERRUPTED_SAVE_PASS phase="+(reload?"COLD_RECOVERY":"SPARSE_ONLY_SAVE")+" pos="+pos);
    }

    private static void admitProto(ServerLevel level,ChunkPos pos,net.minecraft.nbt.CompoundTag data) {
        var restored=ChunkSerializer.read(level,level.getPoiManager(),pos,data.copy());
        require(restored instanceof ProtoChunk && !(restored instanceof ImposterProtoChunk),"Checkpoint is not an unfinished native protochunk");
        var proto=(ProtoChunk)restored;
        DeepProvinceGenerator.prepare(level,proto);
        var full=new LevelChunk(level,proto,null); // Production proto-to-FULL mixin performs admission.
        require(full.isUnsaved(),"Recovered native chunk was not marked for saving");
        require(full.getSections().length==24,"Recovery widened native dense arrays");
    }

    private static void verifySparseEdits(ServerLevel level,ChunkPos pos,BlockPos edit) {
        var vertical=EndlessVerticalEngine.world(level);
        require(vertical.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Deep re-admission overwrote a surviving player block");
        require(vertical.getBlockState(edit.above()).isAir(),"Deep re-admission refilled a surviving player excavation");
        require(vertical.getSectionForRendering(pos.x,-5,pos.z)==null,"Deep re-admission recreated an intentionally removed section");
    }

    static void finish(ServerLevel level,boolean reload) throws Exception {
        var chunk=level.getChunk(waterPos.x,waterPos.z);
        if(reload) {
            verifyPlayerEdits(chunk);
            finishWaterWitness(level,chunk);
            verifyPlayerEdits(chunk);
            require(chunk.getBlockState(pendingEdit(level)).is(Blocks.WATER),"Unfinished water column did not resume");
            var queues=queues();
            synchronized(queues) { require(!queues.get(level).containsKey(waterPos),"Resumed water repair never completed"); }
            require(!saveWater(level,chunk).contains(MARKER),"Completed repair left a save marker");
            System.out.println("REEF_WATER_COLD_RESTART_PASS completed columns preserved; unfinished columns resumed");
            return;
        }
        require(chunk.getFullStatus().isOrAfter(net.minecraft.server.level.FullChunkStatus.BLOCK_TICKING),"Water witness is not ticking");
        int x=waterPos.getMinBlockX(),z=waterPos.getMinBlockZ(),y=level.getSeaLevel();
        require(new ReefTerrain(level,level.getChunkSource().getGenerator()).province(x,z),"Water witness is outside province");
        chunk.setBlockState(new BlockPos(x,y-4,z),Blocks.SANDSTONE.defaultBlockState(),false);
        chunk.setBlockState(new BlockPos(x,y-3,z),Blocks.SAND.defaultBlockState(),false);
        chunk.setBlockState(new BlockPos(x,y-6,z),Blocks.MAGMA_BLOCK.defaultBlockState(),false);
        chunk.setBlockState(pendingEdit(level).below(),Blocks.SAND.defaultBlockState(),false);
        chunk.setBlockState(pendingEdit(level),Blocks.AIR.defaultBlockState(),false);
        var queues=queues();LinkedHashMap<ChunkPos,Integer> original;
        synchronized(queues) {
            original=queues.put(level,new LinkedHashMap<>());
            queues.get(level).put(waterPos,0);
        }
        try {
            ReefWaterFinish.tick(new TickEvent.LevelTickEvent(LogicalSide.SERVER,TickEvent.Phase.END,level,()->true));
            require(chunk.getBlockState(new BlockPos(x,y-6,z)).is(Blocks.SANDSTONE),"First water column was not repaired");
            int cursor=queues.get(level).get(waterPos);
            require(cursor>0 && cursor<=64,"Water fixture did not stop within the column budget");
            // Player edits only after this first column has completed.
            chunk.setBlockState(new BlockPos(x,y-6,z),Blocks.MAGMA_BLOCK.defaultBlockState(),false);
            chunk.setBlockState(new BlockPos(x,y-5,z),Blocks.SOUL_SAND.defaultBlockState(),false);
            chunk.setUnsaved(true);
            var data=saveWater(level,chunk);
            require(data.contains(MARKER,Tag.TAG_INT) && data.getInt(MARKER)==cursor,"Native save lost the next-column cursor");
            NbtIo.writeCompressed(data,WATER.toFile());
            if(original==null)original=new LinkedHashMap<>();
            original.put(waterPos,cursor);
            System.out.println("REEF_WATER_PARTIAL_SAVE_PASS cursor="+cursor);
        } finally {
            synchronized(queues) { queues.put(level,original); }
        }
    }

    private static void finishWaterWitness(ServerLevel level,LevelChunk chunk) throws Exception {
        var queues=queues();LinkedHashMap<ChunkPos,Integer> original,isolated=new LinkedHashMap<>();
        synchronized(queues) {
            original=queues.get(level);
            Integer cursor=original==null?null:original.get(waterPos);
            System.out.println("REEF_WATER_RESTART_QUEUE pendingChunks="+(original==null?0:original.size())
                +" cursor="+cursor+" fullStatus="+chunk.getFullStatus()+" unfinishedState="+chunk.getBlockState(pendingEdit(level)));
            if(cursor==null)return;
            isolated.put(waterPos,cursor);
            queues.put(level,isolated);
        }
        try {
            require(chunk.getFullStatus().isOrAfter(net.minecraft.server.level.FullChunkStatus.BLOCK_TICKING),"Restored water witness is not ticking");
            // The production queue can contain thousands of non-ticking chunks.
            // Its 32-inspection cap deliberately takes many rounds to revisit
            // this witness, so 200 whole-server ticks are not a completion SLA.
            // Isolate only scheduling, as in the fresh-save phase. Execute the
            // real native END handler and require forward progress every call.
            int calls=0,initial=isolated.get(waterPos);
            while(isolated.containsKey(waterPos)) {
                int before=isolated.get(waterPos);
                ReefWaterFinish.tick(new TickEvent.LevelTickEvent(LogicalSide.SERVER,TickEvent.Phase.END,level,()->true));
                int after=isolated.getOrDefault(waterPos,256);
                require(after>before && after<=Math.min(256,before+64),"Restored water cursor failed its progress/budget contract: "+before+" -> "+after);
                require(++calls<=256-initial,"Restored water repair exceeded its remaining-column bound");
            }
            System.out.println("REEF_WATER_RESTART_DRAIN_PASS initialCursor="+initial+" nativeHandlerCalls="+calls);
        } finally {
            synchronized(queues) {
                // Retain any asynchronous chunk-load markers received meanwhile.
                original.remove(waterPos);
                isolated.forEach((pos,cursor)->original.merge(pos,cursor,Math::max));
                queues.put(level,original);
            }
        }
    }

    private static net.minecraft.nbt.CompoundTag saveWater(ServerLevel level,LevelChunk chunk) {
        var data=ChunkSerializer.write(level,chunk);
        // Forge dispatches Save from ChunkMap, after ChunkSerializer.write.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
            new net.minecraftforge.event.level.ChunkDataEvent.Save(chunk,level,data));
        return data;
    }

    private static void verifyLegacyMarkers(ServerLevel level) throws Exception {
        var pos=new ChunkPos(-20000,-20000);
        var chunk=new LevelChunk(level,pos);
        var original=ChunkSerializer.write(level,chunk);
        var queues=queues();
        synchronized(queues) { queues.computeIfAbsent(level,ignored->new LinkedHashMap<>()); }
        for(int test=0;test<7;test++) {
            var data=original.copy();
            if(test==0)data.putBoolean(MARKER,true);
            if(test==1)data.putBoolean(MARKER,false);
            if(test>=2 && test<=5)data.putInt(MARKER,new int[]{0,255,-1,256}[test-2]);
            synchronized(queues) { queues.get(level).remove(pos); }
            ChunkSerializer.read(level,level.getPoiManager(),pos,data);
            Integer expected=test==0 || test==2?Integer.valueOf(0):test==3?Integer.valueOf(255):null;
            synchronized(queues) {
                require(Objects.equals(queues.get(level).get(pos),expected),"Wrong legacy/boundary marker handling for case "+test);
                queues.get(level).remove(pos);
            }
        }
        System.out.println("REEF_WATER_MARKER_COMPATIBILITY_PASS cases=7");
    }

    private static BlockPos pendingEdit(ServerLevel level) {
        return new BlockPos(waterPos.getMaxBlockX(),level.getSeaLevel()-2,waterPos.getMaxBlockZ());
    }

    private static void verifyPlayerEdits(LevelChunk chunk) {
        int x=waterPos.getMinBlockX(),z=waterPos.getMinBlockZ(),y=chunk.getLevel().getSeaLevel();
        require(chunk.getBlockState(new BlockPos(x,y-6,z)).is(Blocks.MAGMA_BLOCK),"Completed-column magma was replayed");
        require(chunk.getBlockState(new BlockPos(x,y-5,z)).is(Blocks.SOUL_SAND),"Completed-column soul sand was replayed");
    }
}
