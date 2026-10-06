package com.nhat.tidal_terror.gametest;
@net.neoforged.fml.common.EventBusSubscriber(modid="tidalterror")
public final class PortTestBootstrap {
 @net.neoforged.bus.api.SubscribeEvent public static void register(net.neoforged.neoforge.event.RegisterGameTestsEvent event){
  var environment=event.registerEnvironment(net.minecraft.resources.Identifier.fromNamespaceAndPath("tidalterror","equipment"),new net.minecraft.gametest.framework.TestEnvironmentDefinition.AllOf());
  java.util.Map<String,java.util.function.Consumer<net.minecraft.gametest.framework.GameTestHelper>> tests=java.util.Map.of("biome_survey",ReefSurvey::sample);
  tests.forEach((name,run)->event.registerTest(net.minecraft.resources.Identifier.fromNamespaceAndPath("tidalterror",name),new net.minecraft.gametest.framework.GameTestInstance(new net.minecraft.gametest.framework.TestData<>(environment,net.minecraft.resources.Identifier.fromNamespaceAndPath("tidalterror","reef_life_pool"),620,0,true)){
   @Override public void run(net.minecraft.gametest.framework.GameTestHelper helper){run.accept(helper);}
   @Override public com.mojang.serialization.MapCodec<? extends net.minecraft.gametest.framework.GameTestInstance> codec(){return com.mojang.serialization.MapCodec.unit(this);}
   @Override protected net.minecraft.network.chat.MutableComponent typeDescription(){return net.minecraft.network.chat.Component.literal("Tidal Terror equipment regression");}
  }));
 }
}
