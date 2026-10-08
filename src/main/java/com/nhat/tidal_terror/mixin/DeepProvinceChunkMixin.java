package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.DeepProvinceGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fresh proto-to-FULL conversion, before chunk admission/ticking/network snapshots. */
@Mixin(LevelChunk.class)
public abstract class DeepProvinceChunkMixin {
    @Inject(method="<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V",at=@At("RETURN"))
    private void deepProvince(ServerLevel level,ProtoChunk proto,LevelChunk.PostLoadProcessor processor,CallbackInfo ci) {
        DeepProvinceGenerator.generate(level,(LevelChunk)(Object)this,proto);
    }
}
