package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import com.nstut.endless.vertical.EndlessVerticalEngine;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkAccess.class)
public abstract class DeepProvinceBiomeMixin {
    @Inject(method="getNoiseBiome",at=@At("HEAD"),cancellable=true)
    private void sparseBiome(int x,int y,int z,CallbackInfoReturnable<Holder<Biome>> cir) {
        if(!((Object)this instanceof LevelChunk chunk) || !EndlessVerticalEngine.isExtendedY(chunk.getLevel(),y*4))return;
        var section=EndlessVerticalEngine.world(chunk.getLevel()).getSectionForRendering(x>>2,y>>2,z>>2);
        if(section==null)return;
        var biome=section.getNoiseBiome(x&3,y&3,z&3);
        if(biome.is(ReefWorldgen.BIOME) || biome.is(ReefWorldgen.WASTES))cir.setReturnValue(biome);
    }
}
