package net.minecraft.server.level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.*;
public final class ServerLevel extends Level {
    public final Source source=new Source();private final Object server=new Object();
    public Source getChunkSource(){return source;}
    public Object getServer(){return server;}
    public int getMinBuildHeight(){return 0;} public int getSeaLevel(){return 4;}
    public void setBlock(BlockPos p,BlockState state,int flags){source.getChunkNow(p.getX()>>4,p.getZ()>>4).setBlockState(p,state);}
    public static final class Source {
        public final Map<ChunkPos,LevelChunk> chunks=new HashMap<>();
        public Object getGenerator(){return this;}
        public LevelChunk getChunkNow(int x,int z){return chunks.get(new ChunkPos(x,z));}
    }
}
