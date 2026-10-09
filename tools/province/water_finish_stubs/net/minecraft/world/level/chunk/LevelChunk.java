package net.minecraft.world.level.chunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.io.*;
public final class LevelChunk extends ChunkAccess {
    private final ServerLevel level;
    private final BlockState[][][] states=new BlockState[16][4][16];
    public boolean unsaved;
    public FullChunkStatus status=FullChunkStatus.BLOCK_TICKING;
    public LevelChunk(ServerLevel level,ChunkPos pos){
        super(pos);this.level=level;
        for(int x=0;x<16;x++)for(int y=0;y<4;y++)for(int z=0;z<16;z++)states[x][y][z]=(y==0?Blocks.SAND:y==1?Blocks.MAGMA_BLOCK:Blocks.AIR).defaultBlockState();
        level.source.chunks.put(pos,this);
    }
    public Level getLevel(){return level;}
    public FullChunkStatus getFullStatus(){return status;}
    public BlockState getBlockState(BlockPos p){return states[p.getX()&15][p.getY()][p.getZ()&15];}
    public void setBlockState(BlockPos p,BlockState state){states[p.getX()&15][p.getY()][p.getZ()&15]=state;}
    public void setUnsaved(boolean value){unsaved=value;}
    public void write(DataOutput out)throws IOException {
        for(int x=0;x<16;x++)for(int y=0;y<4;y++)for(int z=0;z<16;z++)out.writeUTF(states[x][y][z].block.name);
    }
    public void read(DataInput in)throws IOException {
        for(int x=0;x<16;x++)for(int y=0;y<4;y++)for(int z=0;z<16;z++){
            String name=in.readUTF();for(var block:Blocks.ALL)if(block.name.equals(name))states[x][y][z]=block.defaultBlockState();
        }
    }
}
