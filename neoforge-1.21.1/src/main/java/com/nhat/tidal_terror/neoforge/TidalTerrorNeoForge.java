package com.nhat.tidal_terror.neoforge;
import net.neoforged.fml.common.Mod;
import com.nhat.tidal_terror.entities.ModEntities;
import static com.nhat.tidal_terror.worldgen.ReefWorldgen.BIOME;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.FluidTags;
import net.neoforged.bus.api.IEventBus;
@Mod("tidalterror")
public final class TidalTerrorNeoForge {
    public TidalTerrorNeoForge(IEventBus bus) {
        new com.nhat.tidal_terror.TidalTerror();
        bus.addListener(TidalTerrorNeoForge::attributes);
        bus.addListener(TidalTerrorNeoForge::spawns);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Post event)->{
            if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)com.nhat.tidal_terror.worldgen.ReefWaterFinish.tick(level);
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->com.nhat.tidal_terror.worldgen.ReefWaterFinish.stopped(event.getServer()));
    }
    private static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(com.nhat.tidal_terror.entities.ModEntities.CORAL_CRUSHER.get(),com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity.createAttributes().build());
        event.put(com.nhat.tidal_terror.entities.ModEntities.CATHEDRAL_RAY.get(),com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayEntity.createAttributes().build());
        event.put(com.nhat.tidal_terror.entities.ModEntities.VEILGLOW.get(),com.nhat.tidal_terror.entities.veilglow.VeilglowEntity.createAttributes().build());
        event.put(com.nhat.tidal_terror.entities.ModEntities.SHARDBACK.get(),com.nhat.tidal_terror.entities.shardback.ShardbackEntity.createAttributes().build());
    }
    private static void spawns(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.SHARDBACK.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.OCEAN_FLOOR, (type, level, reason, pos, random) ->
                    level.getBiome(pos).is(BIOME) && pos.getY()<level.getSeaLevel()-4
                    && level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WATER)
                    && level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.WATER)
                    && com.nhat.tidal_terror.entities.shardback.ShardbackEntity.isSeabed(level.getBlockState(pos.below()))
                    && level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP),
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.VEILGLOW.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> {
                    if(!level.getBiome(pos).is(BIOME) || pos.getY()>=level.getSeaLevel()-8)return false;
                    // The tall bell and hanging ribbons need an entirely submerged column.
                    for(int y=-1;y<=3;y++)if(!level.getBlockState(pos.above(y)).is(net.minecraft.world.level.block.Blocks.WATER))return false;
                    return true;
                }, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.CATHEDRAL_RAY.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) ->
                    level.getBiome(pos).is(BIOME) && pos.getY() < level.getSeaLevel()-4
                    && level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WATER)
                    && level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.WATER)
                    && level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.WATER),
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.CORAL_CRUSHER.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> {
                    if (!level.getBiome(pos).is(BIOME) || pos.getY() >= level.getSeaLevel()-4) return false;
                    // Unlike WaterAnimal's surface-only rule, reef sharks can use deep water.
                    // NaturalSpawner subsequently checks the entity's actual 2x1 collision box.
                    return level.getFluidState(pos).is(FluidTags.WATER)
                            && level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.WATER)
                            && level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.WATER);
                }, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

}
