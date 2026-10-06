package com.nhat.tidal_terror.fabric;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.entities.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public final class TidalTerrorFabric implements ModInitializer {
    @Override public void onInitialize() {
        new TidalTerror();
        FabricDefaultAttributeRegistry.register(ModEntities.CORAL_CRUSHER.get(), com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.CATHEDRAL_RAY.get(), com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.VEILGLOW.get(), com.nhat.tidal_terror.entities.veilglow.VeilglowEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.SHARDBACK.get(), com.nhat.tidal_terror.entities.shardback.ShardbackEntity.createAttributes());
        ReefFabricSpawns.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_WORLD_TICK.register(com.nhat.tidal_terror.worldgen.ReefWaterFinish::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(com.nhat.tidal_terror.worldgen.ReefWaterFinish::stopped);
    }
}
