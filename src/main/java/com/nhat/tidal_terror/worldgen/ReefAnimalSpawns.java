package com.nhat.tidal_terror.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Extend native spawning only in the submerged Coral Cathedral. */
@Mod.EventBusSubscriber(modid = "tidalterror")
public final class ReefAnimalSpawns {
    public static boolean deepWater(LevelReader level, BlockPos pos) {
        return pos.getY() < level.getSeaLevel() - 13
                && level.getBiome(pos).is(ReefWorldgen.BIOME)
                && level.getWorldBorder().isWithinBounds(pos)
                && level.getBlockState(pos).is(Blocks.WATER)
                && level.getBlockState(pos.above()).is(Blocks.WATER)
                && level.getBlockState(pos.below()).is(Blocks.WATER);
    }

    public static void register(SpawnPlacementRegisterEvent event) {
        // OR adds a reef route without replacing native or other mod predicates.
        event.register(EntityType.DOLPHIN, (type, level, reason, pos, random) ->
                reason == MobSpawnType.NATURAL && deepWater(level, pos));
        event.register(EntityType.TROPICAL_FISH, (type, level, reason, pos, random) ->
                reason == MobSpawnType.NATURAL && deepWater(level, pos));
        event.register(EntityType.TURTLE, (type, level, reason, pos, random) ->
                reason == MobSpawnType.NATURAL && deepWater(level, pos));
    }

    @SubscribeEvent
    public static void turtlePosition(MobSpawnEvent.PositionCheck event) {
        var turtle = event.getEntity();
        if (turtle instanceof Turtle && event.getSpawnType() == MobSpawnType.NATURAL
                && deepWater(event.getLevel(), turtle.blockPosition())
                && event.getLevel().noCollision(turtle) && event.getLevel().isUnobstructed(turtle)
                && event.getResult() != Event.Result.DENY) {
            // Mob's obstruction check rejects liquid. Keep real collision checks.
            event.setResult(Event.Result.ALLOW);
        }
    }
}
