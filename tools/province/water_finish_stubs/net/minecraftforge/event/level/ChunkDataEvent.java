package net.minecraftforge.event.level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
public class ChunkDataEvent extends ChunkEvent {
    private final CompoundTag data;
    public ChunkDataEvent(ChunkAccess chunk,Level level,CompoundTag data){super(chunk,level);this.data=data;}
    public CompoundTag getData(){return data;}
    public static final class Load extends ChunkDataEvent {public Load(ChunkAccess chunk,CompoundTag data){super(chunk,null,data);}}
    public static final class Save extends ChunkDataEvent {public Save(ChunkAccess chunk,Level level,CompoundTag data){super(chunk,level,data);}}
}
