package com.nhat.tidal_terror.events;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.items.ReefArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TidalTerror.MODID)
public final class ReefEquipmentEvents {
    @SubscribeEvent public static void bloodTrail(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide || entity.tickCount % 8 != 0 || !entity.isAlive()) return;
        var effect = entity.getEffect(com.nhat.tidal_terror.effects.ModEffects.REEF_BLEEDING.get());
        if (effect != null) com.nhat.tidal_terror.particles.ModParticles.bleed(entity, 3 + net.minecraft.util.Mth.clamp(effect.getAmplifier(), 0, 3));
    }
    @SubscribeEvent public static void anchor(LivingKnockBackEvent event) {
        var wearer = event.getEntity();
        if (!wearer.isInWater() || !wearer.onGround()) return;
        int pieces = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR && wearer.getItemBySlot(slot).getItem() instanceof ReefArmorItem) pieces++;
        }
        event.setStrength(event.getStrength() * (1 - 0.05F * pieces));
    }
}
