package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("tidalterror")
@PrefixGameTestTemplate(false)
public class VeilglowTests {
    @GameTest(template="coral_crusher_pool",timeoutTicks=100)
    public static void contactStingIsMildAndNeverPursues(GameTestHelper helper) {
        var level=helper.getLevel();level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var center=helper.absolutePos(new BlockPos(10,5,10));
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-1;y<=4;y++)
            level.setBlock(center.offset(x,y,z),Blocks.WATER.defaultBlockState(),2);
        var jelly=helper.spawn(ModEntities.VEILGLOW.get(),10,5,10);
        jelly.setNoAi(true);jelly.setNoGravity(true);
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"veilglow-test"));
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        level.addNewPlayer(player);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setNoGravity(true);player.setPos(jelly.position().add(5,0,0));
        helper.runAfterDelay(3,()->{
            // ServerPlayer starts with 60 ticks of native login damage protection.
            // These directly registered fixtures need its server tick explicitly.
            for(int i=0;i<65;i++)player.tick();
            player.doTick();jelly.tick();player.setPos(jelly.position());player.setHealth(20);player.invulnerableTime=0;
            jelly.playerTouch(player);
            helper.assertTrue(player.getHealth()==18,"Contact sting must deal one heart; health="+player.getHealth()+", water="+jelly.isInWater());
            jelly.playerTouch(player);
            helper.assertTrue(player.getHealth()==18,"Repeated contact must respect cooldown");
            helper.assertTrue(jelly.getTarget()==null,"Jellyfish must never pursue its victim");
            player.setPos(jelly.position().add(5,0,0));
        });
        helper.runAfterDelay(48,()->{
            player.setPos(jelly.position());player.invulnerableTime=0;player.setHealth(20);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);jelly.playerTouch(player);
            helper.assertTrue(player.getHealth()==20,"Creative player was stung");
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);jelly.playerTouch(player);
            helper.assertTrue(player.getHealth()==20,"Spectator player was stung");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(jelly.position().add(5,0,0));jelly.playerTouch(player);
            helper.assertTrue(player.getHealth()==20,"Sting applied without contact");
            player.setPos(jelly.position());jelly.playerTouch(player);
            helper.assertTrue(player.getHealth()==18,"Contact sting did not resume after cooldown");
            player.discard();helper.succeed();
        });
    }
    @GameTest(template="coral_crusher_pool", timeoutTicks=60)
    public static void habitatCollisionAndEgg(GameTestHelper helper) {
        var level=helper.getLevel(); var type=ModEntities.VEILGLOW.get();
        var pos=helper.absolutePos(new BlockPos(10,5,10));
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-2;y<=4;y++)
            level.setBlock(pos.offset(x,y,z),Blocks.WATER.defaultBlockState(),2);
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        var reef=registry.getHolderOrThrow(ReefWorldgen.BIOME);
        // Override only biome and sea level to isolate deep-water habitat logic.
        var view=(ServerLevelAccessor)java.lang.reflect.Proxy.newProxyInstance(
                VeilglowTests.class.getClassLoader(),new Class[]{ServerLevelAccessor.class},
                (proxy,method,args)->switch(method.getName()) {
                    case "getBiome" -> reef;
                    case "getSeaLevel" -> pos.getY()+80;
                    default -> method.invoke(level,args);
                });
        helper.assertTrue(SpawnPlacements.checkSpawnRules(type,view,MobSpawnType.NATURAL,pos,level.random),"Veilglow rejected deep reef water");
        helper.assertTrue(!SpawnPlacements.checkSpawnRules(type,view,MobSpawnType.NATURAL,pos.above(77),level.random),"Veilglow accepted surface");
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);
        helper.assertTrue(!SpawnPlacements.checkSpawnRules(type,view,MobSpawnType.NATURAL,pos,level.random),"Veilglow accepted blocked water below");
        level.setBlock(pos.below(),Blocks.WATER.defaultBlockState(),2);
        for(var key:registry.registryKeySet()) {
            if(!key.location().getNamespace().equals("minecraft"))continue;
            var holder=registry.getHolderOrThrow(key);
            var vanilla=(ServerLevelAccessor)java.lang.reflect.Proxy.newProxyInstance(
                    VeilglowTests.class.getClassLoader(),new Class[]{ServerLevelAccessor.class},
                    (proxy,method,args)->method.getName().equals("getBiome")?holder:method.invoke(view,args));
            helper.assertTrue(!SpawnPlacements.checkSpawnRules(type,vanilla,MobSpawnType.NATURAL,pos,level.random),"Veilglow spawn leaked into "+key);
        }
        var entries=reef.value().getMobSettings().getMobs(type.getCategory()).unwrap();
        helper.assertTrue(entries.stream().anyMatch(e->e.type==type && e.minCount==2 && e.maxCount==4),"Missing native Veilglow group spawn entry");
        var ray=type.create(level);ray.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        helper.assertTrue(level.noCollision(ray),"Open pool collision failed");
        level.setBlock(pos.east(),Blocks.STONE.defaultBlockState(),2);
        helper.assertTrue(!level.noCollision(ray),"Wide Veilglow can overlap coral");
        level.setBlock(pos.east(),Blocks.WATER.defaultBlockState(),2);
        var egg=TidalTerror.VEILGLOW_SPAWN_EGG.get();
        helper.assertTrue(egg.getType(null)==type && egg.getColor(0)==0xffffff && egg.getColor(1)==0xffffff,"Wrong egg entity or tint");
        var spawned=type.spawn(level,egg.getDefaultInstance(),null,pos,MobSpawnType.SPAWN_EGG,false,false);
        helper.assertTrue(spawned!=null && spawned.isAlive() && spawned.getTarget()==null,"Native egg spawn failed or Veilglow hostile");
        System.out.println("VEILGLOW_TEST habitatCollisionAndEgg PASS");
        helper.succeed();
    }

    @GameTest(template="coral_crusher_pool", timeoutTicks=240)
    public static void swimmingUsesNavigation(GameTestHelper helper) {
        var ray=helper.spawn(ModEntities.VEILGLOW.get(),10,5,10);
        var destination=helper.absolutePos(new BlockPos(10,6,15));
        ray.getNavigation().moveTo(destination.getX()+.5,destination.getY(),destination.getZ()+.5,1);
        var start=ray.position();
        helper.runAfterDelay(140,()->{
            helper.assertTrue(ray.position().distanceTo(start)>0.5,"Veilglow did not swim using native navigation");
            helper.assertTrue(ray.isAlive() && ray.getTarget()==null,"Veilglow must remain peaceful and alive underwater");
            System.out.println("VEILGLOW_TEST swimmingUsesNavigation PASS distance="+ray.position().distanceTo(start));
            helper.succeed();
        });
    }
}
