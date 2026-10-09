package net.minecraft.world.level.chunk;
public final class ImposterProtoChunk extends ChunkAccess {
    private final LevelChunk wrapped;
    public ImposterProtoChunk(LevelChunk chunk){super(chunk.getPos());wrapped=chunk;}
    public LevelChunk getWrapped(){return wrapped;}
}
