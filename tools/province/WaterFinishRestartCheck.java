import com.nhat.tidal_terror.worldgen.ReefWaterFinish;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Executes the production handlers with small API stubs; not a native Minecraft run. */
public final class WaterFinishRestartCheck {
    private static final String MARKER="tidalterror:unfinished_reef_water";
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked")
    private static LinkedHashMap<ChunkPos,Integer> pending(ServerLevel level)throws Exception {
        var field=ReefWaterFinish.class.getDeclaredField("PENDING");field.setAccessible(true);
        var queues=(Map<ServerLevel,LinkedHashMap<ChunkPos,Integer>>)field.get(null);
        return queues.computeIfAbsent(level,key->new LinkedHashMap<>());
    }
    private static CompoundTag save(ServerLevel level,LevelChunk chunk){
        var data=new CompoundTag();ReefWaterFinish.save(new ChunkDataEvent.Save(chunk,level,data));return data;
    }
    private static void tick(ServerLevel level){ReefWaterFinish.tick(new TickEvent.LevelTickEvent(TickEvent.Phase.END,level));}
    public static void main(String[] args)throws Exception {
        var level=new ServerLevel();var chunk=new LevelChunk(level,new ChunkPos(0,0));
        if(args[0].equals("save")){
            ReefWaterFinish.loaded(new ChunkEvent.Load(chunk,level,true));
            require(save(level,chunk).contains(MARKER,Tag.TAG_INT) && save(level,chunk).getInt(MARKER)==0,"Fresh cursor zero was not persisted");
            tick(level);
            int cursor=pending(level).get(chunk.getPos());
            require(cursor>0 && cursor<=64,"First repair exceeded budget or made no progress");
            require(chunk.getBlockState(new BlockPos(0,1,0)).is(Blocks.SANDSTONE),"First column did not complete");
            require(chunk.getBlockState(new BlockPos(15,1,15)).is(Blocks.MAGMA_BLOCK),"Final column completed too early");
            chunk.setBlockState(new BlockPos(0,1,0),Blocks.MAGMA_BLOCK.defaultBlockState());
            chunk.setBlockState(new BlockPos(0,2,0),Blocks.SOUL_SAND.defaultBlockState());
            chunk.setBlockState(new BlockPos(0,3,0),Blocks.AIR.defaultBlockState());
            var data=save(level,chunk);
            require(data.contains(MARKER,Tag.TAG_INT) && data.getInt(MARKER)==cursor,"Partial cursor was not saved");
            try(var out=new DataOutputStream(Files.newOutputStream(Path.of(args[1])))){out.writeInt(cursor);data.write(out);chunk.write(out);}
            System.out.println("WATER_FINISH_STUB_SAVE_PASS cursor="+cursor);
        } else if(args[0].equals("resume")){
            require(pending(level).isEmpty(),"Restart did not start with an empty queue");
            CompoundTag data;int expected;
            try(var in=new DataInputStream(Files.newInputStream(Path.of(args[1])))){expected=in.readInt();data=CompoundTag.read(in);chunk.read(in);}
            ReefWaterFinish.read(new ChunkDataEvent.Load(new ImposterProtoChunk(chunk),data));
            require(Objects.equals(pending(level).get(chunk.getPos()),expected),"Restart reset the partial cursor");
            ReefWaterFinish.loaded(new ChunkEvent.Load(chunk,level,true));
            require(Objects.equals(pending(level).get(chunk.getPos()),expected),"Activation reset restored progress");
            for(int i=0;i<256 && !pending(level).isEmpty();i++)tick(level);
            require(pending(level).isEmpty(),"Resumed work did not finish");
            require(chunk.getBlockState(new BlockPos(0,1,0)).is(Blocks.MAGMA_BLOCK),"Completed-column magma was overwritten");
            require(chunk.getBlockState(new BlockPos(0,2,0)).is(Blocks.SOUL_SAND),"Completed-column soul sand was overwritten");
            require(chunk.getBlockState(new BlockPos(0,3,0)).isAir(),"Completed-column excavation was overwritten");
            require(chunk.getBlockState(new BlockPos(15,1,15)).is(Blocks.SANDSTONE),"Unfinished column did not resume");
            require(chunk.getBlockState(new BlockPos(15,3,15)).is(Blocks.WATER),"Unfinished water did not fill");
            require(!save(level,chunk).contains(MARKER),"Completed marker was not removed");
            require(chunk.unsaved,"Progress did not mark the chunk dirty");
            markers(level,chunk);
            largeBacklog();
            System.out.println("WATER_FINISH_STUB_COLD_RESTART_PASS player edits preserved, pending columns repaired, legacy/boundary tags passed");
        } else throw new IllegalArgumentException(args[0]);
    }
    private static void largeBacklog()throws Exception {
        var level=new ServerLevel();var chunk=new LevelChunk(level,new ChunkPos(0,0));
        var queue=pending(level);
        // Reproduce the native reload's scale: unloaded/non-ticking chunks
        // rotate, while a ready partial chunk receives at most 64 columns.
        for(int i=1;i<6298;i++)queue.put(new ChunkPos(i,0),0);
        queue.put(chunk.getPos(),64);
        for(int tick=0;tick<200;tick++)tick(level);
        Integer cursor=queue.get(chunk.getPos());
        require(cursor!=null && cursor>=64 && cursor<256,"Backlog fixture unexpectedly met a 200-tick completion deadline");
        require(chunk.getBlockState(new BlockPos(15,3,15)).isAir(),"Backlogged last column repaired prematurely");
        var unrelated=new LinkedHashMap<>(queue);unrelated.remove(chunk.getPos());
        queue.clear();queue.put(chunk.getPos(),cursor);
        int calls=0;
        while(queue.containsKey(chunk.getPos())){
            int before=queue.get(chunk.getPos());tick(level);int after=queue.getOrDefault(chunk.getPos(),256);
            require(after>before && after<=Math.min(256,before+64),"Isolated native handler failed to advance within budget");
            require(++calls<=256-cursor,"Isolated repair exceeded remaining-column bound");
        }
        require(chunk.getBlockState(new BlockPos(15,3,15)).is(Blocks.WATER),"Isolated final column did not repair");
        queue.putAll(unrelated);require(queue.size()==6297,"Unrelated pending work was lost");
        ReefWaterFinish.stopped(new ServerStoppedEvent(level.getServer()));
        System.out.println("WATER_FINISH_STUB_BACKLOG_PASS queued=6298 cursorAfter200Ticks="+cursor+" isolatedCalls="+calls);
    }
    private static void markers(ServerLevel level,LevelChunk chunk)throws Exception {
        for(int test=0;test<8;test++){
            pending(level).clear();var data=new CompoundTag();
            if(test==0)data.putBoolean(MARKER,true);
            if(test==1)data.putBoolean(MARKER,false);
            if(test>=2 && test<=6)data.putInt(MARKER,new int[]{0,1,255,-1,256}[test-2]);
            ReefWaterFinish.read(new ChunkDataEvent.Load(chunk,data));
            Integer expected=switch(test){case 0,2->0;case 3->1;case 4->255;default->null;};
            require(Objects.equals(pending(level).get(chunk.getPos()),expected),"Wrong legacy/boundary handling for case "+test);
        }
        ReefWaterFinish.loaded(new ChunkEvent.Load(chunk,level,true));
        ReefWaterFinish.stopped(new ServerStoppedEvent(level.getServer()));
        require(pending(level).isEmpty(),"Server stop retained the queue");
    }
}
