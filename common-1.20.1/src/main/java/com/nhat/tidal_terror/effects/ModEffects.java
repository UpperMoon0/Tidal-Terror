package com.nhat.tidal_terror.effects;

import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import dev.architectury.registry.registries.*;

public final class ModEffects {
    private static final ThreadLocal<LivingEntity> DAMAGING_BLEED = new ThreadLocal<>();
    private static final String ATTACKER = "tidalterror:bleeding_attacker";
    private static final String PULSE_TICKS = "tidalterror:bleeding_pulse_ticks";
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(TidalTerror.MODID, net.minecraft.core.registries.Registries.MOB_EFFECT);
    public static final RegistrySupplier<MobEffect> REEF_BLEEDING = EFFECTS.register("reef_bleeding", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xAA4655) {
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
        @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) return;
            var data = com.nhat.tidal_terror.platform.ReefEntityData.get(entity);
            // Legacy saves have no independent countdown; retain their existing pulse phase.
            int remaining = data.contains(PULSE_TICKS) ? data.getInt(PULSE_TICKS)
                    : Math.floorMod(entity.getEffect(REEF_BLEEDING.get()).getDuration() - 1, 40) + 1;
            remaining--;
            data.putInt(PULSE_TICKS, remaining <= 0 ? 40 : remaining);
            if (remaining > 0) return;
            float damage = com.nhat.tidal_terror.balance.ReefBalance.bleedingDamage(amplifier,com.nhat.tidal_terror.items.ReefArmorItem.hasFullSet(entity));
            var owner = bleedingAttacker(entity);
            var source = entity.damageSources().magic();
            if (owner != null) source = new net.minecraft.world.damagesource.DamageSource(source.typeHolder(), owner) {
                // Preserve positionless status damage; credit must not add shield blocking.
                @Override public net.minecraft.world.phys.Vec3 getSourcePosition() { return null; }
            };
            var previous = DAMAGING_BLEED.get();
            DAMAGING_BLEED.set(entity);
            try {
                if (entity.hurt(source, damage))
                    com.nhat.tidal_terror.particles.ModParticles.bleed(entity, 8 + 2 * net.minecraft.util.Mth.clamp(amplifier, 0, 3));
            } finally {
                if (previous == null) DAMAGING_BLEED.remove(); else DAMAGING_BLEED.set(previous);
            }
        }
    });
    public static boolean isBleedingDamage(LivingEntity entity) { return DAMAGING_BLEED.get() == entity; }
    /** Called only for a newly added effect, never a refresh or amplifier update. */
    public static void beginBleeding(LivingEntity entity) {
        com.nhat.tidal_terror.platform.ReefEntityData.get(entity).putInt(PULSE_TICKS, 40);
        com.nhat.tidal_terror.platform.ReefEntityData.get(entity).remove(ATTACKER);
    }
    public static void clearBleedingClock(LivingEntity entity) {
        com.nhat.tidal_terror.platform.ReefEntityData.get(entity).remove(PULSE_TICKS);
        com.nhat.tidal_terror.platform.ReefEntityData.get(entity).remove(ATTACKER);
    }
    /** Track only accepted equal/stronger applications; a weak hit cannot steal an active bleed. */
    public static void applyBleeding(LivingEntity victim, net.minecraft.world.entity.Entity attacker, int duration, int power) {
        var old = victim.getEffect(REEF_BLEEDING.get());
        int previousPower = old == null ? -1 : old.getAmplifier();
        boolean accepted = victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(REEF_BLEEDING.get(), duration, power, false, false, true), attacker);
        var active = victim.getEffect(REEF_BLEEDING.get());
        if (accepted && active != null && active.getAmplifier() == power && previousPower <= power) {
            if (attacker == null) com.nhat.tidal_terror.platform.ReefEntityData.get(victim).remove(ATTACKER);
            else com.nhat.tidal_terror.platform.ReefEntityData.get(victim).putUUID(ATTACKER, attacker.getUUID());
        }
    }
    private static net.minecraft.world.entity.Entity bleedingAttacker(LivingEntity victim) {
        var data = com.nhat.tidal_terror.platform.ReefEntityData.get(victim);
        if (!data.hasUUID(ATTACKER) || !(victim.level() instanceof net.minecraft.server.level.ServerLevel level)) return null;
        var id = data.getUUID(ATTACKER);
        var player = level.getServer().getPlayerList().getPlayer(id);
        return player != null ? player : level.getEntity(id);
    }
    private ModEffects() {}
    public static void register() { EFFECTS.register(); }
}
