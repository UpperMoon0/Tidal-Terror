package com.nhat.tidal_terror.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Finish newly generated reef water after native cross-chunk postprocessing. */

public final class ReefWaterFinish {
    private static final String MARKER="tidalterror:unfinished_reef_water";
    private static final Map<ServerLevel,Set<ChunkPos>> PENDING=new IdentityHashMap<>();
    public static void loaded(ServerLevel level,net.minecraft.world.level.chunk.LevelChunk chunk) {
        chunk.setData(UNFINISHED,true);chunk.markUnsaved();
        synchronized(PENDING){PENDING.computeIfAbsent(level,k->new HashSet<>()).add(chunk.getPos());}
    }
    private static final net.neoforged.neoforge.registries.DeferredRegister<net.neoforged.neoforge.attachment.AttachmentType<?>> ATTACHMENTS=net.neoforged.neoforge.registries.DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.ATTACHMENT_TYPES,"tidalterror");
    private static final java.util.function.Supplier<net.neoforged.neoforge.attachment.AttachmentType<Boolean>> UNFINISHED=ATTACHMENTS.register("unfinished_reef_water",()->net.neoforged.neoforge.attachment.AttachmentType.builder(()->false).serialize(com.mojang.serialization.Codec.BOOL.fieldOf("pending")).build());
    public static void register(net.neoforged.bus.api.IEventBus bus){ATTACHMENTS.register(bus);}
    public static void restore(net.minecraft.world.level.chunk.ChunkAccess access){
        if(access instanceof net.minecraft.world.level.chunk.LevelChunk chunk && chunk.getData(UNFINISHED) && chunk.getLevel() instanceof ServerLevel level)loaded(level,chunk);
    }
    public static void stopped(net.minecraft.server.MinecraftServer server) {
        synchronized(PENDING){PENDING.keySet().removeIf(level->level.getServer()==server);}
    }
    public static void tick(ServerLevel level) {
        List<ChunkPos> positions;
        synchronized(PENDING){var pending=PENDING.get(level);if(pending==null || pending.isEmpty())return;positions=new ArrayList<>(pending);}
        var terrain=new ReefTerrain(level,level.getChunkSource().getGenerator());
        for(ChunkPos pos:positions){
            // Forge Load can fire before FULL. Never request another chunk from
            // that callback; wait for the nonblocking native FULL-chunk lookup.
            var chunk=level.getChunkSource().getChunkNow(pos.x(),pos.z());
            // ChunkMap.prepareTickingChunk invokes postProcessGeneration before
            // BLOCK_TICKING. FULL alone is too early to finish the water.
            if(chunk==null || !chunk.getFullStatus().isOrAfter(net.minecraft.server.level.FullChunkStatus.BLOCK_TICKING))continue;
            BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();
            for(int x=pos.getMinBlockX();x<=pos.getMaxBlockX();x++)for(int z=pos.getMinBlockZ();z<=pos.getMaxBlockZ();z++){
                if(!terrain.reef(x,z))continue;
                // Read the generated sediment, including after reload. Native
                // noise-height reconstruction is expensive and is unnecessary
                // for a basin whose continuous sand layer already exists.
                int floor=level.getMinY()-1;
                for(int y=level.getSeaLevel()-1;y>=level.getMinY();y--){
                    p.set(x,y,z);if(chunk.getBlockState(p).is(Blocks.SAND)){floor=y;break;}
                }
                if(floor<level.getMinY())continue;
                for(int y=level.getMinY();y<level.getSeaLevel();y++){
                    p.set(x,y,z);
                    // Source water is stable; flags 2 updates clients without
                    // requesting neighbour chunks through block notifications.
                    var state=chunk.getBlockState(p);
                    // UnderwaterMagmaFeature checks blocks throughout its
                    // radius, without a biome test at every written position.
                    if(state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.SOUL_SAND))
                        level.setBlock(p,Blocks.SANDSTONE.defaultBlockState(),2);
                    else if(y>floor && (state.isAir() || state.is(Blocks.BUBBLE_COLUMN)))
                        level.setBlock(p,Blocks.WATER.defaultBlockState(),2);
                }
            }
            synchronized(PENDING){var pending=PENDING.get(level);if(pending!=null)pending.remove(pos);}
            chunk.setData(UNFINISHED,false);chunk.markUnsaved();
        }
    }
}
