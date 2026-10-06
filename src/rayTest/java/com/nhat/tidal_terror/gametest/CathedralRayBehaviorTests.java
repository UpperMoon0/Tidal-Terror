package com.nhat.tidal_terror.gametest;

import com.mojang.authlib.GameProfile;
import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;
import static com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayEntity.Behavior.*;

@GameTestHolder("tidalterror")
@PrefixGameTestTemplate(false)
public class CathedralRayBehaviorTests {
    private static void pool(GameTestHelper h) {
        for (int x=0;x<20;x++) for (int y=0;y<12;y++) for (int z=0;z<20;z++)
            h.setBlock(new BlockPos(x,y,z), x==0 || x==19 || y==0 || y==11 || z==0 || z==19 ? Blocks.STONE : Blocks.WATER);
    }
    private static CathedralRayEntity ray(GameTestHelper h, int x, int z) {
        var ray=h.spawn(ModEntities.CATHEDRAL_RAY.get(),x,5,z);
        ray.setPersistenceRequired(); ray.getRandom().setSeed(19042);
        return ray;
    }
    private static ServerPlayer player(GameTestHelper h, Vec3 position, int ticks) {
        var player=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"ray-test"));
        player.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),player) {
            @Override public void send(Packet<?> packet) { }
            @Override public void send(Packet<?> packet, PacketSendListener listener) { }
        };
        player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setPos(position);
        h.getLevel().addNewPlayer(player);
        for(int tick=1;tick<=ticks;tick++) h.runAtTickTime(tick,()->{
            if (player.isRemoved()) return;
            player.doTick(); player.setPos(position); player.setDeltaMovement(Vec3.ZERO); player.setAirSupply(300);
        });
        return player;
    }
    @GameTest(template="coral_crusher_pool", batch="ray_cruise", timeoutTicks=240)
    public static void autonomousCruise(GameTestHelper h) {
        pool(h); var ray=ray(h,10,10); var start=ray.position();
        h.runAtTickTime(200,()->{
            h.assertTrue(ray.position().distanceToSqr(start)>4,"Autonomous cruising did not move");
            h.assertTrue(ray.getBehavior()==CRUISE && ray.getTarget()==null,"Solo ray must cruise peacefully");
            h.assertTrue(h.getLevel().noCollision(ray) && ray.isInWater(),"Cruise clipped solid reef or left water");
            h.succeed();
        });
    }
    @GameTest(template="coral_crusher_pool", batch="ray_school", timeoutTicks=240)
    public static void looseSchoolAndSeparation(GameTestHelper h) {
        pool(h); var leader=ray(h,8,10); leader.setNoAi(true); leader.setNoGravity(true);
        var follower=ray(h,9,10); var start=follower.position();
        h.runAtTickTime(180,()->{
            h.assertTrue(follower.getBehavior()==SCHOOL,"Ray did not follow local leader");
            h.assertTrue(follower.position().distanceToSqr(start)>4,"School follower did not swim");
            h.assertTrue(follower.distanceToSqr(leader)>25 && follower.distanceToSqr(leader)<400,"School spacing failed");
            h.assertTrue(follower.getTarget()==null,"Schooling must remain peaceful"); h.succeed();
        });
    }
    @GameTest(template="coral_crusher_pool", batch="ray_curiosity", timeoutTicks=260)
    public static void curiosityTimesOutAndIgnoresCreative(GameTestHelper h) {
        pool(h); var ray=ray(h,5,10);
        var player=player(h,h.absoluteVec(new Vec3(13,5,10)),240); float health=player.getHealth();
        player.setGameMode(GameType.CREATIVE);
        var start=ray.position();
        h.runAtTickTime(20,()->{
            h.assertTrue(ray.getBehavior()==CRUISE,"Creative player attracted curiosity without cooldown");
            player.setGameMode(GameType.SURVIVAL);
        });
        // Allow the glider to finish turning after the creative visitor becomes curious.
        h.runAtTickTime(110,()->{
            h.assertTrue(ray.getBehavior()==CURIOUS,"Calm swimmer did not attract curiosity");
            h.assertTrue(ray.position().distanceToSqr(start)>.25,"Curious ray did not approach: start="+start+" now="+ray.position()+" path="+ray.getNavigation().getPath()+" done="+ray.getNavigation().isDone()+" delta="+ray.getDeltaMovement()+" wanted="+ray.getMoveControl().getWantedX()+","+ray.getMoveControl().getWantedY()+","+ray.getMoveControl().getWantedZ());
            h.assertTrue(ray.distanceToSqr(player)>=25,"Curiosity crowded the player");
            h.assertTrue(player.getHealth()==health && ray.getTarget()==null,"Ray attacked swimmer");
        });
        h.runAtTickTime(160,()->{
            h.assertTrue(ray.getBehavior()!=CURIOUS,"Curiosity did not expire"); player.setGameMode(GameType.CREATIVE);
        });
        h.runAtTickTime(230,()->{ h.assertTrue(ray.getBehavior()==CRUISE,"Creative player attracted curiosity"); player.discard(); h.succeed(); });
    }
    @GameTest(template="coral_crusher_pool", batch="ray_predator", timeoutTicks=640)
    public static void fleesPredatorAndCannotHeal(GameTestHelper h) {
        pool(h); var shark=h.spawn(ModEntities.CORAL_CRUSHER.get(),4,5,10); shark.setNoAi(true); shark.setNoGravity(true);
        var ray=ray(h,9,10); ray.setHealth(12); double start=ray.distanceToSqr(shark);
        for(int tick=20;tick<=140;tick+=20)h.runAtTickTime(tick,()->System.out.println("RAY_ESCAPE pos="+ray.position()
                +" shark="+shark.position()+" distance="+ray.distanceToSqr(shark)+" pathDone="+ray.getNavigation().isDone()));
        h.runAtTickTime(140,()->{
            h.assertTrue(ray.getBehavior()==FLEE,"Predator did not preempt cruise");
            h.assertTrue(ray.distanceToSqr(shark)>start+9,"Ray did not swim away from shark");
            h.assertTrue(ray.getTarget()==null,"Ray fought a predator");
        });
        h.runAtTickTime(600,()->{
            h.assertTrue(ray.getBehavior()==FLEE && ray.getHealth()==12,"Ray healed or stopped fleeing with predator still nearby");
            shark.discard(); h.succeed();
        });
    }
    @GameTest(template="coral_crusher_pool", batch="ray_damage", timeoutTicks=180)
    public static void damageInterruptsCuriosity(GameTestHelper h) {
        pool(h); var ray=ray(h,10,10); var player=player(h,h.absoluteVec(new Vec3(6,5,10)),160);
        h.runAtTickTime(20,()->{
            h.assertTrue(ray.getBehavior()==CURIOUS,"Expected curiosity before damage");
            h.assertTrue(ray.hurt(h.getLevel().damageSources().playerAttack(player),2),"Damage was not accepted");
            h.assertTrue(ray.getBehavior()==FLEE,"Damage did not immediately interrupt curiosity");
        });
        h.runAtTickTime(120,()->{
            h.assertTrue(ray.getBehavior()==FLEE && ray.distanceToSqr(player)>25,"Damaged ray failed to retreat");
            h.assertTrue(ray.getHealth()==22 && ray.getTarget()==null,"Unsafe healing or retaliation"); player.discard(); h.succeed();
        });
    }
    @GameTest(template="coral_crusher_pool", batch="ray_healing", timeoutTicks=850)
    public static void slowHealingAndDamageReset(GameTestHelper h) {
        pool(h); var ray=ray(h,10,10); ray.hurt(h.getLevel().damageSources().generic(),4);
        h.runAtTickTime(450,()->h.assertTrue(ray.getHealth()==20,"Healed before danger memory and quiet delay"));
        h.runAtTickTime(500,()->{
            h.assertTrue(ray.getHealth()==21 && ray.getBehavior()==CRUISE,"Safe recovery did not heal slowly and resume cruising");
            ray.hurt(h.getLevel().damageSources().generic(),2);
        });
        h.runAtTickTime(800,()->{ h.assertTrue(ray.getHealth()==19,"New damage failed to reset recovery"); h.succeed(); });
    }
    @GameTest(template="coral_crusher_pool", batch="ray_land", timeoutTicks=360)
    public static void noHealingOnLand(GameTestHelper h) {
        pool(h);
        for(int x=6;x<=14;x++)for(int z=6;z<=14;z++)for(int y=2;y<=9;y++)
            h.setBlock(new BlockPos(x,y,z),x==6||x==14||z==6||z==14||y==2||y==9?Blocks.STONE:Blocks.AIR);
        var ray=ray(h,10,10); ray.setHealth(12); ray.setNoGravity(true);
        h.runAtTickTime(320,()->{
            h.assertTrue(!ray.isInWater(),"Dry fixture flooded"); h.assertTrue(ray.getHealth()<=12,"Ray healed out of water"); h.succeed();
        });
    }
    @GameTest(template="coral_crusher_pool", batch="ray_clearance", timeoutTicks=240)
    public static void rejectsNarrowCoralPassage(GameTestHelper h) {
        pool(h);
        for(int y=1;y<11;y++)for(int z=1;z<19;z++)
            if(z<9||z>11) h.setBlock(new BlockPos(10,y,z),Blocks.STONE);
        var ray=ray(h,5,10); var player=player(h,h.absoluteVec(new Vec3(14,5,10)),220);
        for(int tick=20;tick<=200;tick+=20)h.runAtTickTime(tick,()->{
            h.assertTrue(h.getLevel().noCollision(ray),"Wide wings entered narrow coral passage");
            h.assertTrue(ray.getX()<h.absolutePos(new BlockPos(8,0,0)).getX(),"Ray crossed a gap narrower than its wingspan");
        });
        h.runAtTickTime(210,()->{ player.discard(); h.succeed(); });
    }
 @GameTest(template="coral_crusher_pool", batch="ray_obstacles_floor",timeoutTicks=220)
 public static void rayLeavesSeabed(GameTestHelper h) {
  pool(h);for(int x=1;x<19;x++)for(int z=1;z<19;z++)h.setBlock(new BlockPos(x,3,z),Blocks.STONE);var r=h.spawn(ModEntities.CATHEDRAL_RAY.get(),10,4,10);
  r.setNoAi(true);r.setNoGravity(true);var start=r.position();
  h.runAfterDelay(5,()->{
   try {
    var goal=new com.nhat.tidal_terror.entities.cathedral_ray.CathedralRaySwimGoal(r);
    var move=goal.getClass().getDeclaredMethod("navigate",net.minecraft.world.phys.Vec3.class,double.class);move.setAccessible(true);
    h.assertTrue((boolean)move.invoke(goal,h.absoluteVec(new net.minecraft.world.phys.Vec3(10.5,8,15.5)),1.0),"Ray rejected a clear escape from the seabed");
   }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
   r.setNoAi(false);
  });
  h.runAfterDelay(160,()->{
   h.assertTrue(r.position().distanceToSqr(start)>4,"Ray remained stuck on seabed");
   h.assertTrue(r.isInWater()&&h.getLevel().noCollision(r)&&r.isAlive(),"Ray escape clipped or left water");h.succeed();
  });
 }
 @GameTest(template="coral_crusher_pool", batch="ray_obstacles_wings",timeoutTicks=220)
 public static void rayShortcutChecksWings(GameTestHelper h) {
  pool(h);var r=h.spawn(ModEntities.CATHEDRAL_RAY.get(),10,6,6);r.setNoAi(true);r.setNoGravity(true);
  h.setBlock(new BlockPos(12,6,11),Blocks.STONE);
  h.runAfterDelay(5,()->{
   try {
    var nav=r.getNavigation();Class<?> owner=nav.getClass();java.lang.reflect.Method shortcut=null;
    while(shortcut==null&&owner!=null){try{shortcut=owner.getDeclaredMethod("canMoveDirectly",net.minecraft.world.phys.Vec3.class,net.minecraft.world.phys.Vec3.class);}catch(NoSuchMethodException e){owner=owner.getSuperclass();}}
    shortcut.setAccessible(true);
    var start=r.position().add(0,r.getBbHeight()*.5,0);
    var blocked=h.absoluteVec(new net.minecraft.world.phys.Vec3(10.5,6,15.5));
    h.assertTrue(!(boolean)shortcut.invoke(nav,start,blocked),"Ray shortcut ignores coral under its wings");
    h.setBlock(new BlockPos(12,6,11),Blocks.WATER);
    h.assertTrue((boolean)shortcut.invoke(nav,start,blocked),"Ray rejected a clear wide-water shortcut");h.succeed();
   }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
  });
 }


}
