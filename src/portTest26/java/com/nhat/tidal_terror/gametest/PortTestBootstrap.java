package com.nhat.tidal_terror.gametest;
@net.neoforged.fml.common.EventBusSubscriber(modid="tidalterror")
public final class PortTestBootstrap {
 @net.neoforged.bus.api.SubscribeEvent public static void register(net.neoforged.neoforge.event.RegisterGameTestsEvent event){
  var environment=event.registerEnvironment(net.minecraft.resources.Identifier.fromNamespaceAndPath("tidalterror","equipment"),new net.minecraft.gametest.framework.TestEnvironmentDefinition.AllOf());
  java.util.Map<String,java.util.function.Consumer<net.minecraft.gametest.framework.GameTestHelper>> tests=java.util.Map.ofEntries(
   java.util.Map.entry("refresh_clock",PortEquipmentTests::refreshRetainsClockAndUpdatedDamage),
   java.util.Map.entry("saved_clock",PortEquipmentTests::independentCountdownSurvivesReload),
   java.util.Map.entry("fang_arrow_land",PortEquipmentTests::fangArrowBleedsOnLand),
   java.util.Map.entry("armor_bleed_resistance",PortEquipmentTests::armorResistanceAppliesOnLand),
   java.util.Map.entry("repairs_and_enchantments",PortEquipmentTests::equipmentRepairsAndRestrictions),
   java.util.Map.entry("jellypulseandloosebloom",ReefLifeAiTests::jellyPulseAndLooseBloom),
   java.util.Map.entry("jellyescapespredator",ReefLifeAiTests::jellyEscapesPredator),
   java.util.Map.entry("jellydamagethensaferecovery",ReefLifeAiTests::jellyDamageThenSafeRecovery),
   java.util.Map.entry("jellyescaperespectssolidwall",ReefLifeAiTests::jellyEscapeRespectsSolidWall),
   java.util.Map.entry("crabforageswithoutchangingsediment",ReefLifeAiTests::crabForagesWithoutChangingSediment),
   java.util.Map.entry("crabescapesandshelters",ReefLifeAiTests::crabEscapesAndShelters),
   java.util.Map.entry("crabfinishesshelterroutewhenslowed",ReefLifeAiTests::crabFinishesShelterRouteWhenSlowed),
   java.util.Map.entry("crabwarnsthencontactpinchesandretreats",ReefLifeAiTests::crabWarnsThenContactPinchesAndRetreats),
   java.util.Map.entry("creativeandspectatordonotprovokecrab",ReefLifeAiTests::creativeAndSpectatorDoNotProvokeCrab),
   java.util.Map.entry("crabcannotrouteacrossunsupportedwater",helper -> { try { ReefLifeAiTests.crabCannotRouteAcrossUnsupportedWater(helper); } catch (Exception failure) { throw new RuntimeException(failure); } }));
  tests.forEach((name,run)->event.registerTest(net.minecraft.resources.Identifier.fromNamespaceAndPath("tidalterror",name),new net.minecraft.gametest.framework.GameTestInstance(new net.minecraft.gametest.framework.TestData<>(environment,net.minecraft.resources.Identifier.fromNamespaceAndPath("tidalterror","reef_life_pool"),620,0,true)){
   @Override public void run(net.minecraft.gametest.framework.GameTestHelper helper){run.accept(helper);}
   @Override public com.mojang.serialization.MapCodec<? extends net.minecraft.gametest.framework.GameTestInstance> codec(){return com.mojang.serialization.MapCodec.unit(this);}
   @Override protected net.minecraft.network.chat.MutableComponent typeDescription(){return net.minecraft.network.chat.Component.literal("Tidal Terror equipment regression");}
  }));
 }
}
