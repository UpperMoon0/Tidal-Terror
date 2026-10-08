package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.ProvinceSeeds;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomState.class)
public abstract class ProvinceRandomStateMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void tidalterror$bindSeed(NoiseGeneratorSettings settings,
            HolderGetter<NormalNoise.NoiseParameters> noises, long seed, CallbackInfo ci) {
        ProvinceSeeds.bind(((RandomState)(Object)this).sampler(), seed);
    }
}
