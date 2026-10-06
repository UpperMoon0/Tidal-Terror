package com.nhat.tidal_terror.particles;

import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ModParticles {
    private static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, TidalTerror.MODID);
    public static final RegistryObject<SimpleParticleType> BLOOD = REGISTRY.register("blood", () -> new SimpleParticleType(false));
    private ModParticles() {}
    public static void register(IEventBus bus) { REGISTRY.register(bus); }
    public static void bleed(LivingEntity entity, int count) {
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(BLOOD.get(), entity.getX(), entity.getY() + entity.getBbHeight() * .55, entity.getZ(),
                    count, entity.getBbWidth() * .2, entity.getBbHeight() * .15, entity.getBbWidth() * .2, .055);
        }
    }
}
