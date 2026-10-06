package com.nhat.tidal_terror.mixin;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ChunkSerializer.class)
public abstract class ReefChunkSaveMixin {
    @Inject(method="write",at=@At("RETURN"))private static void save(ServerLevel level,ChunkAccess chunk,CallbackInfoReturnable<CompoundTag> ci) {
        com.nhat.tidal_terror.worldgen.ReefWaterFinish.save(level,chunk,ci.getReturnValue());
    }
    @Inject(method="read",at=@At("RETURN"))private static void read(ServerLevel level,net.minecraft.world.entity.ai.village.poi.PoiManager poi,net.minecraft.world.level.ChunkPos pos,CompoundTag data,CallbackInfoReturnable<net.minecraft.world.level.chunk.ProtoChunk> ci) {
        com.nhat.tidal_terror.worldgen.ReefWaterFinish.read(ci.getReturnValue(),data);
    }
}
