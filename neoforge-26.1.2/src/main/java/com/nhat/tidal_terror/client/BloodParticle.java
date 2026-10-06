package com.nhat.tidal_terror.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;

/** Small blood droplets disperse into a translucent plume in water and fall in air. */
public final class BloodParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final float initialSize;
    private BloodParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz, sprites.get(0, 1));
        this.sprites = sprites;
        xd = dx; yd = dy; zd = dz;
        setSize(.02F, .02F);
        quadSize = initialSize = .09F + random.nextFloat() * .05F;
        lifetime = 28 + random.nextInt(17);
        hasPhysics = true;
        setSpriteFromAge(sprites);
    }
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (++age >= lifetime) { remove(); return; }
        boolean water = level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER);
        yd += water ? .0007 : -.012;
        move(xd, yd, zd);
        float drag = water ? .93F : .98F;
        xd *= drag; yd *= drag; zd *= drag;
        float progress = (float) age / lifetime;
        quadSize = initialSize * (water ? 1 + 1.8F * progress : 1);
        alpha = (water ? .95F : 1) * Math.min(1, 2 * (1 - progress));
        setSpriteFromAge(sprites);
    }
    @Override protected SingleQuadParticle.Layer getLayer() { return SingleQuadParticle.Layer.TRANSLUCENT; }
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz, net.minecraft.util.RandomSource random) {
            return new BloodParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
