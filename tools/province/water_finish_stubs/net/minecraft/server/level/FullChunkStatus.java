package net.minecraft.server.level;
public enum FullChunkStatus { FULL,BLOCK_TICKING;
    public boolean isOrAfter(FullChunkStatus other){return ordinal()>=other.ordinal();}
}
