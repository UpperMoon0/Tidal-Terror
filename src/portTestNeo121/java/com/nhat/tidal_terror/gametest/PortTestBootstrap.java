package com.nhat.tidal_terror.gametest;
@net.neoforged.fml.common.EventBusSubscriber(modid="tidalterror")
public final class PortTestBootstrap {
 @net.neoforged.bus.api.SubscribeEvent public static void register(net.neoforged.neoforge.event.RegisterGameTestsEvent event){event.register(PortTestBootstrap.class);}
 @net.minecraft.gametest.framework.GameTestGenerator public static java.util.Collection<net.minecraft.gametest.framework.TestFunction> tests(){
  var result=new java.util.ArrayList<net.minecraft.gametest.framework.TestFunction>();
  for(var clazz:java.util.List.of(PortEquipmentTests.class,ReefLifeAiTests.class,ReefSpawnPoolTests.class))for(var method:clazz.getDeclaredMethods()){
   var metadata=method.getAnnotation(net.minecraft.gametest.framework.GameTest.class);if(metadata==null)continue;
   result.add(new net.minecraft.gametest.framework.TestFunction("tidalterror:ports","tidalterror:"+clazz.getSimpleName().toLowerCase(java.util.Locale.ROOT)+"."+method.getName().toLowerCase(java.util.Locale.ROOT),metadata.template(),metadata.timeoutTicks(),metadata.setupTicks(),metadata.required(),helper->{
    try{method.invoke(null,helper);}catch(java.lang.reflect.InvocationTargetException e){if(e.getCause() instanceof RuntimeException cause)throw cause;throw new RuntimeException(e.getCause());}catch(ReflectiveOperationException e){throw new RuntimeException(e);}
   }));
  }return result;
 }
 @net.neoforged.bus.api.SubscribeEvent public static void world(net.neoforged.neoforge.event.server.ServerStartedEvent event){
  for(var level:event.getServer().getAllLevels()){
   event.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
   level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,event.getServer());
   level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,event.getServer());level.setDayTime(18000);
  }
 }
}
