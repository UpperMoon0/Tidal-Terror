package com.nhat.tidal_terror.gametest;
import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
@GameTestHolder("tidalterror") @PrefixGameTestTemplate(false)
public class ShardbackTests {
 private static void pool(GameTestHelper helper){
  for(int x=0;x<=23;x++)for(int z=0;z<=23;z++)for(int y=3;y<=8;y++)
   helper.setBlock(x,y,z,y==3?Blocks.SANDSTONE:Blocks.WATER);
 }
 @GameTest(template="reef_life_pool",timeoutTicks=60)
 public static void seabedHabitatAndEgg(GameTestHelper helper){
  pool(helper);var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(10,4,10));
  var type=ModEntities.SHARDBACK.get();var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
  var reef=registry.getHolderOrThrow(ReefWorldgen.BIOME);
  var view=(ServerLevelAccessor)java.lang.reflect.Proxy.newProxyInstance(ShardbackTests.class.getClassLoader(),new Class[]{ServerLevelAccessor.class},
   (proxy,method,args)->switch(method.getName()){
    case "getBiome"->reef;case "getSeaLevel"->pos.getY()+80;default->method.invoke(level,args);
   });
  helper.assertTrue(SpawnPlacements.checkSpawnRules(type,view,MobSpawnType.NATURAL,pos,level.random),"Rejected submerged reef sand");
  level.setBlock(pos.below(),Blocks.FIRE_CORAL_BLOCK.defaultBlockState(),2);
  helper.assertTrue(!SpawnPlacements.checkSpawnRules(type,view,MobSpawnType.NATURAL,pos,level.random),"Accepted coral ledge instead of sediment seabed");
  level.setBlock(pos.below(),Blocks.SANDSTONE.defaultBlockState(),2);
  helper.assertTrue(!SpawnPlacements.checkSpawnRules(type,view,MobSpawnType.NATURAL,pos.above(),level.random),"Accepted floating water without seabed");
  level.setBlock(pos.above(),Blocks.STONE.defaultBlockState(),2);
  helper.assertTrue(!SpawnPlacements.checkSpawnRules(type,view,MobSpawnType.NATURAL,pos,level.random),"Accepted obstructed head room");
  level.setBlock(pos.above(),Blocks.WATER.defaultBlockState(),2);
  for(var key:registry.registryKeySet())if(key.location().getNamespace().equals("minecraft")){
   var holder=registry.getHolderOrThrow(key);
   var other=(ServerLevelAccessor)java.lang.reflect.Proxy.newProxyInstance(ShardbackTests.class.getClassLoader(),new Class[]{ServerLevelAccessor.class},
    (proxy,method,args)->method.getName().equals("getBiome")?holder:method.invoke(view,args));
   helper.assertTrue(!SpawnPlacements.checkSpawnRules(type,other,MobSpawnType.NATURAL,pos,level.random),"Leaked spawning into "+key);
  }
  helper.assertTrue(reef.value().getMobSettings().getMobs(type.getCategory()).unwrap().stream()
   .anyMatch(e->e.type==type&&e.minCount==1&&e.maxCount==3),"Missing native spawn group");
  var egg=TidalTerror.SHARDBACK_SPAWN_EGG.get();
  helper.assertTrue(egg.getType(null)==type&&egg.getColor(0)==0xffffff&&egg.getColor(1)==0xffffff,"Wrong egg entity/tints");
  var crab=type.spawn(level,egg.getDefaultInstance(),null,pos,MobSpawnType.SPAWN_EGG,false,false);
  helper.assertTrue(crab!=null&&crab.isAlive()&&crab.getTarget()==null,"Native egg spawn failed");
  helper.assertTrue(level.noCollision(crab),"Crab collides in open pool");
  helper.assertTrue(crab.checkSpawnObstruction(level),"Native aquatic spawn obstruction rejected water");
  level.setBlock(pos.east(),Blocks.STONE.defaultBlockState(),2);
  helper.assertTrue(!level.noCollision(crab),"Crab overlaps solid terrain");
  System.out.println("SHARDBACK_TEST seabedHabitatAndEgg PASS");helper.succeed();
 }
 @GameTest(template="reef_life_pool",timeoutTicks=240)
 public static void walksOnSeabedAndBreathes(GameTestHelper helper){
  pool(helper);var crab=helper.spawn(ModEntities.SHARDBACK.get(),10,4,10);
  long seed=crab.getRandom().nextLong();crab.getRandom().setSeed(seed);
  System.out.println("SHARDBACK_TEST walksOnSeabedAndBreathes seed="+seed);
  helper.onEachTick(()->{
   if(crab.getHealth()!=16 || !helper.getLevel().noCollision(crab)) {
    var box=crab.getBoundingBox();var solids=new java.util.ArrayList<String>();
    for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX+.001,box.minY+.001,box.minZ+.001),
      BlockPos.containing(box.maxX-.001,box.maxY-.001,box.maxZ-.001)))
     if(!helper.getLevel().getBlockState(pos).getCollisionShape(helper.getLevel(),pos).isEmpty())
      solids.add(helper.relativePos(pos)+"="+helper.getLevel().getBlockState(pos));
    helper.assertTrue(false,"Seabed crab collision/damage: seed="+seed+" tick="+crab.tickCount+" health="+crab.getHealth()
      +" position="+crab.position()+" box="+box+" solids="+solids+" eye="+crab.getEyePosition()
      +" water="+crab.isInWater()+" air="+crab.getAirSupply()+" behavior="+crab.getBehavior()
      +" damage="+crab.getLastDamageSource());
   }
  });
  var start=crab.position();var destination=helper.absolutePos(new BlockPos(15,4,10));
  helper.runAfterDelay(5,()->{
   helper.assertTrue(crab.getNavigation().moveTo(destination.getX()+.5,destination.getY(),destination.getZ()+.5,1),"Native seabed path failed");
  });
  helper.runAfterDelay(180,()->{
   helper.assertTrue(crab.position().distanceTo(start)>.75,"Crab did not walk along the seabed");
   helper.assertTrue(Math.abs(crab.getY()-start.y)<.6,"Crab left the seabed vertically; position="+crab.position()+", start="+start);
   helper.assertTrue(crab.isInWater()&&crab.isAlive()&&crab.getHealth()==16,"Crab drowned or lost health underwater: health="+crab.getHealth()+" water="+crab.isInWater()+" air="+crab.getAirSupply()+" position="+crab.position()+" damage="+crab.getLastDamageSource());
   helper.assertTrue(crab.getTarget()==null,"Peaceful crab acquired an attack target");
   System.out.println("SHARDBACK_TEST walksOnSeabedAndBreathes PASS distance="+crab.position().distanceTo(start));helper.succeed();
  });
 }
}
