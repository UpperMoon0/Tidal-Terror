package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.worldgen.ProvinceSeeds;
import com.nhat.tidal_terror.worldgen.ReefProvinceAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Native noise generation supplies a chunk-cached sampler, not RandomState.sampler(). */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class ProvinceClimateSamplerMixin {
    @Redirect(method = "doCreateBiomes", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/chunk/ChunkAccess;fillBiomesFromNoise(Lnet/minecraft/world/level/biome/BiomeResolver;Lnet/minecraft/world/level/biome/Climate$Sampler;)V"))
    private void tidalterror$seedCachedSampler(ChunkAccess chunk, BiomeResolver resolver,
            Climate.Sampler sampler, Blender blender, RandomState state,
            StructureManager structures, ChunkAccess targetChunk) {
        if (((NoiseBasedChunkGenerator)(Object)this).getBiomeSource() instanceof ReefProvinceAccess)
            ProvinceSeeds.bindAlias(sampler, state.sampler());
        chunk.fillBiomesFromNoise(resolver, sampler);
    }
}
