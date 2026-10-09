package com.nhat.tidal_terror.mixin;

import com.mojang.datafixers.util.Either;
import com.nhat.tidal_terror.worldgen.DeepProvinceGenerator;
import net.minecraft.Util;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

/** FULL conversion queues work on the server thread; prepare before invoking that continuation.
 * Saved protochunks can already be past RAW_GENERATION, so both native load and generation need this. */
@Mixin(ChunkStatus.class)
public abstract class DeepProvincePreparationMixin {
    @Unique
    private Function<ChunkAccess,CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>>> tidalterror$prepare(
        ServerLevel level,Function<ChunkAccess,CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>>> convert) {
        if((Object)this!=ChunkStatus.FULL || !(level.getChunkSource().getGenerator().getBiomeSource()
            instanceof com.nhat.tidal_terror.worldgen.ReefProvinceAccess access) || !access.deep()) return convert;
        return chunk->{
            if(!(chunk instanceof ProtoChunk) || chunk instanceof ImposterProtoChunk) return convert.apply(chunk);
            return CompletableFuture.runAsync(()->DeepProvinceGenerator.prepare(level,chunk),Util.backgroundExecutor())
                .thenCompose(ignored->convert.apply(chunk));
        };
    }
    @ModifyVariable(method="generate",at=@At("HEAD"),argsOnly=true)
    private Function<ChunkAccess,CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>>> prepareGenerated(
        Function<ChunkAccess,CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>>> value,
        Executor executor,ServerLevel level,ChunkGenerator generator,StructureTemplateManager templates,
        ThreadedLevelLightEngine lights,Function<ChunkAccess,CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>>> convert,
        List<ChunkAccess> chunks) {
        return tidalterror$prepare(level,value);
    }
    @ModifyVariable(method="load",at=@At("HEAD"),argsOnly=true)
    private Function<ChunkAccess,CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>>> prepareLoaded(
        Function<ChunkAccess,CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>>> value,
        ServerLevel level,StructureTemplateManager templates,ThreadedLevelLightEngine lights,
        Function<ChunkAccess,CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>>> convert,ChunkAccess chunk) {
        return tidalterror$prepare(level,value);
    }
}
