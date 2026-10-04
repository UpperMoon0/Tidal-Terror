package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("tidalterror")
@PrefixGameTestTemplate(false)
public class ReefAnimalSpawnTests {
    @GameTest(template="coral_crusher_pool", timeoutTicks=40)
    public static void deepReefAndVanillaRules(GameTestHelper helper) {
        var level=helper.getLevel();
        BlockPos pos=helper.absolutePos(new BlockPos(10,5,10));
        for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++)
            level.setBlock(pos.offset(x,y,z),Blocks.WATER.defaultBlockState(),2);
        var biomes=level.registryAccess().registryOrThrow(Registries.BIOME);
        var reef=biomes.getHolderOrThrow(ReefWorldgen.BIOME);
        var view=(ServerLevelAccessor)java.lang.reflect.Proxy.newProxyInstance(
                ReefAnimalSpawnTests.class.getClassLoader(),new Class[]{ServerLevelAccessor.class},
                (proxy,method,args)->method.getName().equals("getBiome")?reef:method.invoke(level,args));
        for(var type:java.util.List.of(EntityType.DOLPHIN,EntityType.TURTLE,EntityType.TROPICAL_FISH))
            helper.assertTrue(SpawnPlacements.checkSpawnRules(type,view,MobSpawnType.NATURAL,pos,level.random),"Deep reef rejects "+type);
        helper.assertTrue(NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND,view,pos,EntityType.TURTLE),"Native placement still blocks deep turtles");
        var turtle=EntityType.TURTLE.create(level);
        turtle.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        helper.assertTrue(ForgeEventFactory.checkSpawnPosition(turtle,view,MobSpawnType.NATURAL),"Native obstruction still rejects reef turtle");
        turtle.setPos(pos.getX()+.95,pos.getY(),pos.getZ()+.5);
        level.setBlock(pos.east(),Blocks.STONE.defaultBlockState(),2);
        helper.assertTrue(!ForgeEventFactory.checkSpawnPosition(turtle,view,MobSpawnType.NATURAL),"Reef turtle must not spawn inside solid coral/terrain");
        level.setBlock(pos.east(),Blocks.WATER.defaultBlockState(),2);
        for(var key:biomes.registryKeySet()) {
            if(!key.location().getNamespace().equals("minecraft"))continue;
            var holder=biomes.getHolderOrThrow(key);
            var vanilla=(ServerLevelAccessor)java.lang.reflect.Proxy.newProxyInstance(
                    ReefAnimalSpawnTests.class.getClassLoader(),new Class[]{ServerLevelAccessor.class},
                    (proxy,method,args)->method.getName().equals("getBiome")?holder:method.invoke(level,args));
            helper.assertTrue(SpawnPlacements.checkSpawnRules(EntityType.DOLPHIN,vanilla,MobSpawnType.NATURAL,pos,level.random)==
                    WaterAnimal.checkSurfaceWaterAnimalSpawnRules(EntityType.DOLPHIN,vanilla,MobSpawnType.NATURAL,pos,level.random),"Changed vanilla dolphin habitat "+key);
            helper.assertTrue(SpawnPlacements.checkSpawnRules(EntityType.TROPICAL_FISH,vanilla,MobSpawnType.NATURAL,pos,level.random)==
                    TropicalFish.checkTropicalFishSpawnRules(EntityType.TROPICAL_FISH,vanilla,MobSpawnType.NATURAL,pos,level.random),"Changed vanilla tropical fish habitat "+key);
            helper.assertTrue(SpawnPlacements.checkSpawnRules(EntityType.TURTLE,vanilla,MobSpawnType.NATURAL,pos,level.random)==
                    Turtle.checkTurtleSpawnRules(EntityType.TURTLE,vanilla,MobSpawnType.NATURAL,pos,level.random),"Changed vanilla turtle habitat "+key);
            helper.assertTrue(!NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND,vanilla,pos,EntityType.TURTLE),"Underwater turtle placement leaked into "+key);
            helper.assertTrue(!ForgeEventFactory.checkSpawnPosition(turtle,vanilla,MobSpawnType.NATURAL),"Underwater turtle obstruction leaked into "+key);
        }
        helper.assertTrue(com.nhat.tidal_terror.TidalTerror.CORAL_CRUSHER_SPAWN_EGG.get().getType(null)==
                com.nhat.tidal_terror.entities.ModEntities.CORAL_CRUSHER.get(),"Spawn egg targets wrong entity");
        helper.succeed();
    }
}
