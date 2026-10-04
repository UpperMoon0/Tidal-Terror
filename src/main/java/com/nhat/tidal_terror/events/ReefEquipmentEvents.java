package com.nhat.tidal_terror.events;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.items.ReefArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TidalTerror.MODID)
public final class ReefEquipmentEvents {
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
