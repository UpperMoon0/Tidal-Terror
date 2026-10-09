package net.minecraft.world.level;
public final class ChunkPos {
    public final int x,z;
    public ChunkPos(int x,int z){this.x=x;this.z=z;}
    public int getMinBlockX(){return x*16;} public int getMinBlockZ(){return z*16;}
    public boolean equals(Object value){return value instanceof ChunkPos p && p.x==x && p.z==z;}
    public int hashCode(){return x*31+z;}
}
