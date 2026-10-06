package com.nhat.tidal_terror.gametest;

import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Test-only guard: isolated rooms must not inherit terrain or ambient mobs. */
@Mod.EventBusSubscriber(modid="tidalterror")
public final class NativeFixtureEnvironment {
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        var server=event.getServer();
        if (!(server instanceof GameTestServer)) return;
        var level=server.overworld();
        if (!(level.getChunkSource().getGenerator() instanceof FlatLevelSource)
                || level.getSeed()!=0 || server.getWorldData().worldGenOptions().generateStructures())
            throw new IllegalStateException("Native fixtures require the isolated flat, seed-0, structure-free world");
        var rules=level.getGameRules();
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false,server);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);
        level.setDayTime(18000); // Dry undead fixtures must not take sunlight damage.
        level.setWeatherParameters(0,0,false,false);
        server.setDifficulty(Difficulty.NORMAL,true);
        System.out.println("NATIVE_FIXTURE_WORLD flat seed=0 structures=false mobSpawning=false fresh=true");
    }
}
