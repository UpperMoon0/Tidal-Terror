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
public class CathedralRayTests {
    @GameTest(template="coral_crusher_pool", timeoutTicks=60)
    public static void habitatCollisionAndEgg(GameTestHelper helper) {
        var level=helper.getLevel(); var type=ModEntities.CATHEDRAL_RAY.get();
        var pos=helper.absolutePos(new BlockPos(10,5,10));
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-2;y<=2;y++)
            level.setBlock(pos.offset(x,y,z),Blocks.WATER.defaultBlockState(),2);
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        var reef=registry.getHolderOrThrow(ReefWorldgen.BIOME);
        // Override only biome and sea level to isolate deep-water habitat logic.
        var view=(ServerLevelAccessor)java.lang.reflect.Proxy.newProxyInstance(
                CathedralRayTests.class.getClassLoader(),new Class[]{ServerLevelAccessor.class},
                (proxy,method,args)->switch(method.getName()) {
                    case "getBiome" -> reef;
                    case "getSeaLevel" -> pos.getY()+80;
                    default -> method.invoke(level,args);
                });
        helper.assertTrue(SpawnPlacements.checkSpawnRules(type,view,MobSpawnType.NATURAL,pos,level.random),"Ray rejected deep reef water");
        helper.assertTrue(!SpawnPlacements.checkSpawnRules(type,view,MobSpawnType.NATURAL,pos.above(77),level.random),"Ray accepted surface");
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);
        helper.assertTrue(!SpawnPlacements.checkSpawnRules(type,view,MobSpawnType.NATURAL,pos,level.random),"Ray accepted blocked water below");
        level.setBlock(pos.below(),Blocks.WATER.defaultBlockState(),2);
        for(var key:registry.registryKeySet()) {
            if(!key.location().getNamespace().equals("minecraft"))continue;
            var holder=registry.getHolderOrThrow(key);
            var vanilla=(ServerLevelAccessor)java.lang.reflect.Proxy.newProxyInstance(
                    CathedralRayTests.class.getClassLoader(),new Class[]{ServerLevelAccessor.class},
                    (proxy,method,args)->method.getName().equals("getBiome")?holder:method.invoke(view,args));
            helper.assertTrue(!SpawnPlacements.checkSpawnRules(type,vanilla,MobSpawnType.NATURAL,pos,level.random),"Ray spawn leaked into "+key);
        }
        var entries=reef.value().getMobSettings().getMobs(type.getCategory()).unwrap();
        helper.assertTrue(entries.stream().anyMatch(e->e.type==type && e.minCount==2 && e.maxCount==3),"Missing native ray group spawn entry");
        var ray=type.create(level);ray.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        helper.assertTrue(level.noCollision(ray),"Open pool collision failed");
        level.setBlock(pos.east(2),Blocks.STONE.defaultBlockState(),2);
        helper.assertTrue(!level.noCollision(ray),"Wide ray can overlap coral");
        level.setBlock(pos.east(2),Blocks.WATER.defaultBlockState(),2);
        var egg=TidalTerror.CATHEDRAL_RAY_SPAWN_EGG.get();
        helper.assertTrue(egg.getType(null)==type && egg.getColor(0)==0xffffff && egg.getColor(1)==0xffffff,"Wrong egg entity or tint");
        var spawned=type.spawn(level,egg.getDefaultInstance(),null,pos,MobSpawnType.SPAWN_EGG,false,false);
        helper.assertTrue(spawned!=null && spawned.isAlive() && spawned.getTarget()==null,"Native egg spawn failed or ray hostile");
        System.out.println("RAY_TEST habitatCollisionAndEgg PASS");
        helper.succeed();
    }

    @GameTest(template="coral_crusher_pool", timeoutTicks=240)
    public static void swimmingUsesNavigation(GameTestHelper helper) {
        var ray=helper.spawn(ModEntities.CATHEDRAL_RAY.get(),10,5,10);
        var destination=helper.absolutePos(new BlockPos(10,9,15));
        ray.getNavigation().moveTo(destination.getX()+.5,destination.getY(),destination.getZ()+.5,1);
        var start=ray.position();
        helper.runAfterDelay(80,()->{
            helper.assertTrue(ray.position().distanceTo(start)>1,"Ray did not swim using native navigation");
            helper.assertTrue(ray.isAlive() && ray.getTarget()==null,"Ray must remain peaceful and alive underwater");
            System.out.println("RAY_TEST swimmingUsesNavigation PASS distance="+ray.position().distanceTo(start));
            helper.succeed();
        });
    }
}
