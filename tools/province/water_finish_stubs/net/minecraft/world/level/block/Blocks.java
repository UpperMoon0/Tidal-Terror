package net.minecraft.world.level.block;
import net.minecraft.world.level.block.state.BlockState;
public final class Blocks {
    public static final Block AIR=new Block("air"),SAND=new Block("sand"),WATER=new Block("water"),MAGMA_BLOCK=new Block("magma"),SOUL_SAND=new Block("soul"),SANDSTONE=new Block("sandstone"),BUBBLE_COLUMN=new Block("bubble");
    public static final Block[] ALL={AIR,SAND,WATER,MAGMA_BLOCK,SOUL_SAND,SANDSTONE,BUBBLE_COLUMN};
    public static final class Block {
        public final String name; private final BlockState state;
        Block(String name){this.name=name;this.state=new BlockState(this);}
        public BlockState defaultBlockState(){return state;}
    }
}
