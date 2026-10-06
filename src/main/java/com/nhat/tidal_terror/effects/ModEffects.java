package com.nhat.tidal_terror.effects;

import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ModEffects {
    private static final ThreadLocal<LivingEntity> DAMAGING_BLEED = new ThreadLocal<>();
    private static final String ATTACKER = "tidalterror:bleeding_attacker";
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
        entity.getPersistentData().putInt(PULSE_TICKS, 40);
        entity.getPersistentData().remove(ATTACKER);
    }
    public static void clearBleedingClock(LivingEntity entity) {
        entity.getPersistentData().remove(PULSE_TICKS);
        entity.getPersistentData().remove(ATTACKER);
    }
    /** Track only accepted equal/stronger applications; a weak hit cannot steal an active bleed. */
    public static void applyBleeding(LivingEntity victim, net.minecraft.world.entity.Entity attacker, int duration, int power) {
        var old = victim.getEffect(REEF_BLEEDING.get());
        int previousPower = old == null ? -1 : old.getAmplifier();
        boolean accepted = victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(REEF_BLEEDING.get(), duration, power, false, false, true), attacker);
        var active = victim.getEffect(REEF_BLEEDING.get());
        if (accepted && active != null && active.getAmplifier() == power && previousPower <= power) {
            if (attacker == null) victim.getPersistentData().remove(ATTACKER);
            else victim.getPersistentData().putUUID(ATTACKER, attacker.getUUID());
        }
    }
    private static net.minecraft.world.entity.Entity bleedingAttacker(LivingEntity victim) {
        var data = victim.getPersistentData();
        if (!data.hasUUID(ATTACKER) || !(victim.level() instanceof net.minecraft.server.level.ServerLevel level)) return null;
        var id = data.getUUID(ATTACKER);
        var player = level.getServer().getPlayerList().getPlayer(id);
        return player != null ? player : level.getEntity(id);
    }
    private ModEffects() {}
    public static void register(IEventBus bus) { EFFECTS.register(bus); }
}
