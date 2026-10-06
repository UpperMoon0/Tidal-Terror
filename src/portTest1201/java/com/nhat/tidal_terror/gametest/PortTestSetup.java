package com.nhat.tidal_terror.gametest;
public final class PortTestSetup implements net.fabricmc.api.ModInitializer {
    @Override public void onInitialize() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{
            if(!(server instanceof net.minecraft.gametest.framework.GameTestServer))return;
            var level=server.overworld();
            if(!(level.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource)||level.getSeed()!=0)
                throw new IllegalStateException("Port tests require the native flat seed-0 world");
            server.setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
            var rules=level.getGameRules();
            rules.getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);
            rules.getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,server);
            rules.getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false,server);
            level.setDayTime(18000);level.setWeatherParameters(0,0,false,false);
            System.out.println("PORT_TEST_WORLD flat seed=0 mobSpawning=false");
        });
    }
}
