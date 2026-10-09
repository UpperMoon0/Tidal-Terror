package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.ReefProvinceAccess;
import com.nstut.endless.vertical.EndlessVerticalEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class DeepProvinceHeightMixin {
    @Inject(method="getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I",at=@At("RETURN"),cancellable=true)
    private void deepFloor(Heightmap.Types type,int x,int z,CallbackInfoReturnable<Integer> cir) {
        if(!((Object)this instanceof ServerLevel level) || !(level.getChunkSource().getGenerator().getBiomeSource() instanceof ReefProvinceAccess access) || !access.deep())return;
        if(type!=Heightmap.Types.OCEAN_FLOOR && type!=Heightmap.Types.OCEAN_FLOOR_WG)return;
        if(cir.getReturnValue()>level.getMinBuildHeight())return;
        int sparse=EndlessVerticalEngine.world(level).getExtendedHeight(type,x,z);
        if(sparse!=Integer.MIN_VALUE)cir.setReturnValue(sparse);
    }
}
