package com.nhat.tidal_terror.particles;

import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import dev.architectury.registry.registries.*;

public final class ModParticles {
    private static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> REGISTRY = DeferredRegister.create(TidalTerror.MODID, net.minecraft.core.registries.Registries.PARTICLE_TYPE);
    public static final RegistrySupplier<SimpleParticleType> BLOOD = REGISTRY.register("blood", () -> new SimpleParticleType(false) {});
    private ModParticles() {}
    public static void register() { REGISTRY.register(); }
    public static void bleed(LivingEntity entity, int count) {
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(BLOOD.get(), entity.getX(), entity.getY() + entity.getBbHeight() * .55, entity.getZ(),
                    count, entity.getBbWidth() * .2, entity.getBbHeight() * .15, entity.getBbWidth() * .2, .055);
        }
    }
}
