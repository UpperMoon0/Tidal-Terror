package com.nhat.tidal_terror.effects;

import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ModEffects {
    private static final String PULSE_TICKS = "tidalterror:bleeding_pulse_ticks";
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TidalTerror.MODID);
    public static final RegistryObject<MobEffect> REEF_BLEEDING = EFFECTS.register("reef_bleeding", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xAA4655) {
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
        @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) return;
            var data = entity.getPersistentData();
            // Legacy saves have no independent countdown; retain their existing pulse phase.
            int remaining = data.contains(PULSE_TICKS) ? data.getInt(PULSE_TICKS)
                    : Math.floorMod(entity.getEffect(REEF_BLEEDING.get()).getDuration() - 1, 40) + 1;
            remaining--;
            data.putInt(PULSE_TICKS, remaining <= 0 ? 40 : remaining);
            if (remaining > 0) return;
            float damage = 1 + .5F * net.minecraft.util.Mth.clamp(amplifier, 0, 3);
            if (com.nhat.tidal_terror.items.ReefArmorItem.hasFullSet(entity)) damage *= .75F;
            if (entity.hurt(entity.damageSources().magic(), damage))
                com.nhat.tidal_terror.particles.ModParticles.bleed(entity, 8 + 2 * net.minecraft.util.Mth.clamp(amplifier, 0, 3));
        }
    });
    /** Called only for a newly added effect, never a refresh or amplifier update. */
    public static void beginBleeding(LivingEntity entity) { entity.getPersistentData().putInt(PULSE_TICKS, 40); }
    public static void clearBleedingClock(LivingEntity entity) { entity.getPersistentData().remove(PULSE_TICKS); }
    private ModEffects() {}
    public static void register(IEventBus bus) { EFFECTS.register(bus); }
}
