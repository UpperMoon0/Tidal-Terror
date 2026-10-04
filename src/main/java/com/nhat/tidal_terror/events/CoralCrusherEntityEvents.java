package com.nhat.tidal_terror.events;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TidalTerror.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CoralCrusherEntityEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CORAL_CRUSHER.get(), CoralCrusherEntity.createAttributes().build());
    }
}
