package net.minecraft.world.level.block.state;
import net.minecraft.world.level.block.Blocks;
public final class BlockState {
    public final Blocks.Block block;
    public BlockState(Blocks.Block block){this.block=block;}
    public boolean is(Blocks.Block other){return block==other;}
    public boolean isAir(){return is(Blocks.AIR);}
}
