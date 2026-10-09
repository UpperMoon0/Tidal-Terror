package net.minecraft.world.level.chunk;
import net.minecraft.world.level.ChunkPos;
public class ChunkAccess {
    protected final ChunkPos pos;
    public ChunkAccess(ChunkPos pos){this.pos=pos;}
    public ChunkPos getPos(){return pos;}
}
