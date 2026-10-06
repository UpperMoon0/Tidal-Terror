package com.nhat.tidal_terror.mixin;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelChunk.class)
public abstract class ReefChunkFinishMixin {
    @Inject(method="postProcessGeneration",at=@At("TAIL"))private void pending(CallbackInfo ci) {
        var chunk=(LevelChunk)(Object)this;
        if(chunk.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
            com.nhat.tidal_terror.worldgen.ReefWaterFinish.loaded(level,chunk);
    }
}
