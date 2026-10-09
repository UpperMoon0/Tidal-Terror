package com.nhat.tidal_terror.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import java.util.Map;

/** Worker-local decoration view. Halo reads predict terrain and never request neighbour chunks. */
final class PrivateReefBlockAccess implements ReefBlockAccess,LevelReader {
    private final Map<Integer,LevelChunkSection> sections;
    private final ChunkAccess chunk;
    private final ReefTerrain terrain;
    private final long seed;
    private final int seaLevel,mx,mz;

    PrivateReefBlockAccess(Map<Integer,LevelChunkSection> sections,ChunkAccess chunk,ReefTerrain terrain,long seed,int seaLevel) {
        this.sections=sections;this.chunk=chunk;this.terrain=terrain;this.seed=seed;this.seaLevel=seaLevel;
        mx=chunk.getPos().getMinBlockX();mz=chunk.getPos().getMinBlockZ();
    }
    public LevelReader reader() { return this; }
    @Override public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
    @Override public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos) { return null; }
    @Override public int getMinBuildHeight() { return ReefProvinceLayout.DEEP_BOTTOM; }
    @Override public int getHeight() { return 320-ReefProvinceLayout.DEEP_BOTTOM; }
    @Override public int getSeaLevel() { return seaLevel; }
    @Override public boolean isClientSide() { return false; }
    @Override public boolean hasChunk(int x,int z) { return x==(mx>>4) && z==(mz>>4); }
    @Override public net.minecraft.world.level.BlockGetter getChunkForCollisions(int x,int z) { return this; }
    // This view provides block/fluid/shape reads, not world admission, lighting or entity access.
    // Concrete methods are remapped with the production jar, unlike reflective method-name dispatch.
    private static UnsupportedOperationException unsupported() { return new UnsupportedOperationException("World query outside private reef block view"); }
    @Override public ChunkAccess getChunk(int x,int z,net.minecraft.world.level.chunk.ChunkStatus status,boolean create) { throw unsupported(); }
    @Override public int getHeight(net.minecraft.world.level.levelgen.Heightmap.Types type,int x,int z) { throw unsupported(); }
    @Override public int getSkyDarken() { throw unsupported(); }
    @Override public net.minecraft.world.level.biome.BiomeManager getBiomeManager() { throw unsupported(); }
    @Override public net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> getUncachedNoiseBiome(int x,int y,int z) { throw unsupported(); }
    @Override public net.minecraft.world.level.dimension.DimensionType dimensionType() { throw unsupported(); }
    @Override public net.minecraft.core.RegistryAccess registryAccess() { throw unsupported(); }
    @Override public net.minecraft.world.flag.FeatureFlagSet enabledFeatures() { throw unsupported(); }
    @Override public float getShade(net.minecraft.core.Direction direction,boolean shade) { throw unsupported(); }
    @Override public net.minecraft.world.level.lighting.LevelLightEngine getLightEngine() { throw unsupported(); }
    @Override public int getBlockTint(BlockPos pos,net.minecraft.world.level.ColorResolver color) { throw unsupported(); }
    @Override public net.minecraft.world.level.border.WorldBorder getWorldBorder() { throw unsupported(); }
    @Override public java.util.List<net.minecraft.world.phys.shapes.VoxelShape> getEntityCollisions(net.minecraft.world.entity.Entity entity,net.minecraft.world.phys.AABB bounds) { throw unsupported(); }
    private boolean owns(BlockPos pos) { return pos.getX()>=mx && pos.getX()<mx+16 && pos.getZ()>=mz && pos.getZ()<mz+16; }
    @Override public BlockState getBlockState(BlockPos pos) {
        if(owns(pos)) {
            if(pos.getY()>=-64)return chunk.getBlockState(pos);
            var section=sections.get(Math.floorDiv(pos.getY(),16));
            return section==null?Blocks.AIR.defaultBlockState():section.getBlockState(pos.getX()&15,pos.getY()&15,pos.getZ()&15);
        }
        if(pos.getY()<ReefProvinceLayout.DEEP_BOTTOM || terrain.provinceSample(pos.getX(),pos.getZ())==null)return Blocks.AIR.defaultBlockState();
        int floor=terrain.floor(pos.getX(),pos.getZ());
        if(pos.getY()>floor)return pos.getY()<seaLevel?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState();
        if(ReefProvinceLayout.bedrock(seed,pos.getX(),pos.getY(),pos.getZ(),floor))return Blocks.BEDROCK.defaultBlockState();
        int sand=6+(int)Math.floorMod((long)pos.getX()*31+(long)pos.getZ()*17,3);
        return pos.getY()>floor-sand?Blocks.SAND.defaultBlockState():Blocks.DEEPSLATE.defaultBlockState();
    }
    public boolean setBlock(BlockPos pos,BlockState state,int flags) {
        if(!owns(pos) || pos.getY()>=-64)return false;
        var section=sections.get(Math.floorDiv(pos.getY(),16));
        if(section==null)return false;
        section.setBlockState(pos.getX()&15,pos.getY()&15,pos.getZ()&15,state,false);
        return true;
    }
}
