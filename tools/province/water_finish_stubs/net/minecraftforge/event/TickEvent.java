package net.minecraftforge.event;
import net.minecraft.world.level.Level;
public class TickEvent {
    public enum Phase { START,END }
    public static final class LevelTickEvent {
        public final Phase phase;public final Level level;
        public LevelTickEvent(Phase phase,Level level){this.phase=phase;this.level=level;}
    }
}
