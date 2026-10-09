package net.minecraftforge.event.level;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
public class ChunkEvent {
    protected final ChunkAccess chunk;protected final Level level;
    public ChunkEvent(ChunkAccess chunk,Level level){this.chunk=chunk;this.level=level;}
    public ChunkAccess getChunk(){return chunk;}public Level getLevel(){return level;}
    public static final class Load extends ChunkEvent {
        private final boolean fresh;
        public Load(ChunkAccess chunk,Level level,boolean fresh){super(chunk,level);this.fresh=fresh;}
        public boolean isNewChunk(){return fresh;}
    }
}
