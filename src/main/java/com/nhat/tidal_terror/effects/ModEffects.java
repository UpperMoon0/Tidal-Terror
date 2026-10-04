package com.nhat.tidal_terror.effects;

import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TidalTerror.MODID);
    public static final RegistryObject<MobEffect> REEF_BLEEDING = EFFECTS.register("reef_bleeding", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xAA4655) {
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration == 40 || duration == 1; }
        @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
            if (!entity.level().isClientSide) entity.hurt(entity.damageSources().magic(), 1);
        }
    });
    private ModEffects() {}
    public static void register(IEventBus bus) { EFFECTS.register(bus); }
}
