package com.nhat.tidal_terror.events;

@net.neoforged.fml.common.EventBusSubscriber(modid="tidalterror")
public final class ReefTurtleSpawns {
    @net.neoforged.bus.api.SubscribeEvent
    public static void position(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck event) {
        var mob = event.getEntity();
        if (mob instanceof net.minecraft.world.entity.animal.Turtle && event.getSpawnType() == net.minecraft.world.entity.MobSpawnType.NATURAL
                && event.getResult() != net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.FAIL
                && com.nhat.tidal_terror.worldgen.ReefAnimalSpawns.deepWater(event.getLevel(), mob.blockPosition())
                && event.getLevel().noCollision(mob) && event.getLevel().isUnobstructed(mob))
            event.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.SUCCEED);
    }
}
