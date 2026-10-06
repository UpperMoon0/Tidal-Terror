package com.nhat.tidal_terror.mixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelChunk.class)
public abstract class ReefChunkFinishMixin {
    // Only unfinished generation takes this constructor. Disk-loaded full chunks
    // use the other constructor, even when postProcessGeneration runs again.
    @Inject(method="<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",at=@At("TAIL"))
    private void generated(ServerLevel level, ProtoChunk proto, LevelChunk.PostLoadProcessor postLoad, CallbackInfo ci) {
        if (!(proto instanceof net.minecraft.world.level.chunk.ImposterProtoChunk))
            com.nhat.tidal_terror.worldgen.ReefWaterFinish.loaded(level,(LevelChunk)(Object)this);
    }
}
