package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.veilglow.VeilglowEntity;
import com.nhat.tidal_terror.entities.shardback.ShardbackEntity;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("tidalterror") @PrefixGameTestTemplate(false)
public class ReefLifeAiTests {
 private static void water(GameTestHelper h,boolean floor){
  for(int x=0;x<=23;x++)for(int z=0;z<=23;z++)for(int y=3;y<=14;y++)
   h.setBlock(x,y,z,y==3&&floor?Blocks.SANDSTONE:Blocks.WATER);
 }
 private static net.minecraft.world.entity.Mob shark(GameTestHelper h,int z){
  var s=h.spawn(ModEntities.CORAL_CRUSHER.get(),10,5,z);s.setNoAi(true);s.setNoGravity(true);return s;
 }
 private static ServerPlayer player(GameTestHelper h,GameType mode){
  var level=h.getLevel();level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
  var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"reef-life-test"));
  p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
   new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p){
    @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
    @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
   };
  level.addNewPlayer(p);p.setGameMode(mode);p.setNoGravity(true);
  p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(20,6,20)));
  for(int i=0;i<65;i++)p.tick();p.doTick();return p;
 }
 @GameTest(template="reef_life_pool",timeoutTicks=240)
 public static void jellyPulseAndLooseBloom(GameTestHelper h){
  water(h,false);var a=h.spawn(ModEntities.VEILGLOW.get(),10,6,10);
  var b=h.spawn(ModEntities.VEILGLOW.get(),14,6,10);var start=b.position();boolean[] seen={false,false};
  h.onEachTick(()->{seen[0]|=b.getBehavior()==VeilglowEntity.Behavior.PULSE;seen[1]|=b.getBehavior()==VeilglowEntity.Behavior.BLOOM;});
  h.runAfterDelay(180,()->{
   h.assertTrue(seen[0]&&seen[1],"Jelly never alternated pulse and loose bloom");
   h.assertTrue(b.position().distanceTo(start)>.5,"Jelly bloom did not navigate; distance="+b.position().distanceTo(start));
   h.assertTrue(a.getTarget()==null&&b.getTarget()==null,"Bloom acquired an attack target");h.succeed();
  });
 }
 @GameTest(template="reef_life_pool",timeoutTicks=180)
 public static void jellyEscapesPredator(GameTestHelper h){
  water(h,false);var j=h.spawn(ModEntities.VEILGLOW.get(),10,6,10);var s=shark(h,6);double before=j.distanceToSqr(s);
  h.runAfterDelay(80,()->{
   h.assertTrue(j.getBehavior()==VeilglowEntity.Behavior.FLEE,"Jelly ignored nearby predator");
   h.assertTrue(j.distanceToSqr(s)>before+4,"Jelly did not move away from predator; before="+before+", now="+j.distanceToSqr(s));
   h.assertTrue(j.getTarget()==null&&j.getHealth()==12,"Jelly attacked or took passive predator damage");h.succeed();
  });
 }
 @GameTest(template="reef_life_pool",timeoutTicks=620)
 public static void jellyDamageThenSafeRecovery(GameTestHelper h){
  water(h,false);var j=h.spawn(ModEntities.VEILGLOW.get(),10,6,10);float[] damaged={0};
  h.runAfterDelay(5,()->{
   j.hurt(h.getLevel().damageSources().generic(),2);damaged[0]=j.getHealth();
   h.assertTrue(j.getBehavior()==VeilglowEntity.Behavior.FLEE,"Damage did not interrupt jelly drift");
  });
  h.runAfterDelay(100,()->h.assertTrue(j.getHealth()==damaged[0],"Jelly healed during escape"));
  h.runAfterDelay(560,()->{
   h.assertTrue(j.getBehavior()!=VeilglowEntity.Behavior.FLEE&&j.getHealth()>damaged[0],"Jelly failed quiet-water recovery");
   h.assertTrue(j.getTarget()==null&&j.isInWater(),"Recovery pursued a target or beached");h.succeed();
  });
 }
 @GameTest(template="reef_life_pool",timeoutTicks=160)
 public static void jellyEscapeRespectsSolidWall(GameTestHelper h){
  water(h,false);for(int x=0;x<=23;x++)for(int y=3;y<=14;y++)h.setBlock(x,y,13,Blocks.STONE);
  var j=h.spawn(ModEntities.VEILGLOW.get(),10,6,10);shark(h,6);
  h.runAfterDelay(100,()->{
   h.assertTrue(j.getBehavior()==VeilglowEntity.Behavior.FLEE,"Missing blocked escape reaction");
   h.assertTrue(j.position().z<h.absolutePos(new net.minecraft.core.BlockPos(0,0,13)).getZ()-.7,"Jelly crossed solid wall");
   h.assertTrue(h.getLevel().noCollision(j)&&j.isInWater(),"Escape clipped terrain or left water");h.succeed();
  });
 }
 @GameTest(template="reef_life_pool",timeoutTicks=240)
 public static void crabForagesWithoutChangingSediment(GameTestHelper h){
  water(h,true);var c=h.spawn(ModEntities.SHARDBACK.get(),10,4,10);var start=c.position();boolean[] fed={false};double[] furthest={0};
  h.onEachTick(()->{fed[0]|=c.getBehavior()==ShardbackEntity.Behavior.FORAGE;furthest[0]=Math.max(furthest[0],c.position().distanceTo(start));});
  h.runAfterDelay(180,()->{
   h.assertTrue(fed[0],"Crab never paused to forage");
   h.assertTrue(furthest[0]>.75,"Crab did not browse between patches; maximum displacement="+furthest[0]);
   h.assertBlockPresent(Blocks.SANDSTONE,new net.minecraft.core.BlockPos(10,3,10));
   h.assertTrue(c.getTarget()==null&&Math.abs(c.getY()-start.y)<.5,"Crab foraging attacked or floated");h.succeed();
  });
 }
 @GameTest(template="reef_life_pool",timeoutTicks=240)
 public static void crabEscapesAndShelters(GameTestHelper h){
  water(h,true);for(int z=11;z<=18;z++)for(int y=4;y<=7;y++)h.setBlock(16,y,z,Blocks.STONE);
  var c=h.spawn(ModEntities.SHARDBACK.get(),10,4,10);var s=shark(h,6);double before=c.distanceToSqr(s);
  // Entity water flags are initialized by native ticks, not by spawn().
  // Keep the crab at the encounter start until its stationary predator is wet.
  c.setNoAi(true);h.runAfterDelay(5,()->{h.assertTrue(s.isInWater(),"Predator fixture never entered water");c.setNoAi(false);});
  boolean[] escaped={false},covered={false};
  h.onEachTick(()->{escaped[0]|=c.getBehavior()==ShardbackEntity.Behavior.FLEE;covered[0]|=c.getBehavior()==ShardbackEntity.Behavior.SHELTER;});
  h.runAfterDelay(180,()->{
   h.assertTrue(escaped[0]&&covered[0],"Crab failed escape/cover cycle; behavior="+c.getBehavior()+" position="+c.position());
   h.assertTrue(c.distanceToSqr(s)>before+4,"Crab moved toward predator");
   h.assertTrue(c.getTarget()==null&&c.isInWater()&&h.getLevel().noCollision(c),"Crab attacked, beached or clipped cover");h.succeed();
  });
 }
 @GameTest(template="reef_life_pool",timeoutTicks=160)
 public static void crabWarnsThenContactPinchesAndRetreats(GameTestHelper h){
  water(h,true);var c=h.spawn(ModEntities.SHARDBACK.get(),10,4,10);var p=player(h,GameType.SURVIVAL);
  p.setPos(c.position().add(2,0,0));p.doTick();
  h.runAfterDelay(30,()->{
   h.assertTrue(c.getBehavior()==ShardbackEntity.Behavior.THREATEN,"Crab did not warn close survival player");
   c.setNoAi(true);p.setPos(c.position());p.setHealth(20);p.invulnerableTime=0;c.playerTouch(p);
   h.assertTrue(p.getHealth()==18,"Contact pinch must deal one heart");
   h.assertTrue(c.getBehavior()==ShardbackEntity.Behavior.FLEE&&c.getTarget()==null,"Crab chased instead of retreating after pinch");
   p.invulnerableTime=0;c.playerTouch(p);h.assertTrue(p.getHealth()==18,"Repeated pinch bypassed cooldown");
   p.discard();h.succeed();
  });
 }
 @GameTest(template="reef_life_pool",timeoutTicks=160)
 public static void creativeAndSpectatorDoNotProvokeCrab(GameTestHelper h){
  water(h,true);var c=h.spawn(ModEntities.SHARDBACK.get(),10,4,10);var p=player(h,GameType.CREATIVE);
  p.setPos(c.position().add(2,0,0));p.doTick();
  h.runAfterDelay(25,()->{
   h.assertTrue(c.getBehavior()!=ShardbackEntity.Behavior.THREATEN,"Creative player provoked crab");
   p.setGameMode(GameType.SPECTATOR);p.setPos(c.position().add(2,0,0));p.doTick();
  });
  h.runAfterDelay(55,()->{
   h.assertTrue(c.getBehavior()!=ShardbackEntity.Behavior.THREATEN&&p.getHealth()==20,"Spectator provoked crab or took damage");
   p.discard();h.succeed();
  });
 }
 @GameTest(template="reef_life_pool",timeoutTicks=140)
 public static void crabCannotRouteAcrossUnsupportedWater(GameTestHelper h){
  water(h,true);
  for(int x=13;x<=15;x++)for(int z=0;z<=23;z++)h.setBlock(x,3,z,Blocks.WATER);
  var c=h.spawn(ModEntities.SHARDBACK.get(),10,4,10);
  h.runAfterDelay(5,()->{
   var destination=h.absoluteVec(new net.minecraft.world.phys.Vec3(19.5,4,10.5));
   h.assertTrue(!com.nhat.tidal_terror.entities.ReefNavigation.move(c,destination,1,true),"Crab accepted a floating path across an unsupported seabed gap");
  });
  h.runAfterDelay(100,()->{
   h.assertTrue(c.getX()<h.absolutePos(new net.minecraft.core.BlockPos(13,4,0)).getX(),"Crab crossed the unsupported gap");
   h.assertTrue(c.isInWater()&&c.isAlive()&&h.getLevel().noCollision(c),"Crab left water or clipped while avoiding the gap");h.succeed();
  });
 }

}
