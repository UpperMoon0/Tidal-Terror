package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.EnumSet;
import static com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity.Behavior.*;

@GameTestHolder("tidalterror")
@PrefixGameTestTemplate(false)
public class CoralCrusherCombatTests {
    private static void pin(GameTestHelper h, net.minecraft.server.level.ServerPlayer player, Vec3 position, int ticks) {
        player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setPos(position);
        for(int tick=1;tick<=ticks;tick++) h.runAtTickTime(tick,()->{
            if(player.isRemoved())return;
            player.doTick(); player.setPos(position); player.setDeltaMovement(Vec3.ZERO); player.setAirSupply(300);
        });
    }
    private static void vertical(GameTestHelper h, int startY, int targetY) {
        var shark=h.spawn(ModEntities.CORAL_CRUSHER.get(),10,startY,10);
        var player=CoralCrusherAiTests.player(h); var position=h.absoluteVec(new Vec3(10.5,targetY,10.5));
        pin(h,player,position,700);
        h.runAtTickTime(20,()->shark.hurt(h.getLevel().damageSources().playerAttack(player),2));
        final double[] chargeStart={Double.NaN}, verticalTravel={0};
        final boolean[] charged={false};
        for(int tick=1;tick<=680;tick++) {
            int now=tick;
            h.runAtTickTime(tick,()->{
                if(shark.getBehavior()==CHARGE) {
                    charged[0]=true;
                    if(Double.isNaN(chargeStart[0])) chargeStart[0]=shark.getY();
                    verticalTravel[0]=Math.max(verticalTravel[0],Math.abs(shark.getY()-chargeStart[0]));
                }
                if(charged[0] && shark.getBehavior()==RECOVER)
                    h.assertTrue(player.getHealth()<20,"First steep charge ended before reaching the stationary target");
                if(now%40==0)System.out.println("CRUSHER_VERTICAL y="+shark.getY()+" state="+shark.getBehavior()
                        +" playerY="+player.getY()+" health="+player.getHealth()+" chargeTravel="+verticalTravel[0]);
            });
        }
        h.succeedWhen(()->{
            h.assertTrue(player.getHealth()<20,"Steep vertical pursuit never landed a bite");
            h.assertTrue(charged[0] && verticalTravel[0]>=4,"Charge did not cover real vertical distance");
            h.assertTrue(Math.abs(shark.getY()-h.absoluteVec(new Vec3(0,startY,0)).y)>10,"Vertical pursuit did not climb/dive");
            player.discard();shark.discard();
        });
    }
    @GameTest(template="crusher_combat_pool", batch="crusher_vertical_up", timeoutTicks=720)
    public static void chargesSteeplyUpward(GameTestHelper h) { vertical(h,4,26); }
    @GameTest(template="crusher_combat_pool", batch="crusher_vertical_down", timeoutTicks=720)
    public static void chargesSteeplyDownward(GameTestHelper h) { vertical(h,28,6); }

    @GameTest(template="crusher_combat_pool", batch="crusher_retaliation", timeoutTicks=280)
    public static void retaliatesBeyondPassivePlayerRange(GameTestHelper h) {
        var shark=h.spawn(ModEntities.CORAL_CRUSHER.get(),5,5,10);
        var player=CoralCrusherAiTests.player(h); var position=h.absoluteVec(new Vec3(45,5,10));
        pin(h,player,position,260); double initial=shark.distanceToSqr(player);
        h.runAtTickTime(20,()->{
            h.assertTrue(shark.getTarget()==null,"Passive player range unexpectedly exceeds 32");
            h.assertTrue(shark.hurt(h.getLevel().damageSources().playerAttack(player),2),"Retaliation fixture damage rejected");
            h.assertTrue(shark.getTarget()==player,"Accepted attack did not immediately target attacker");
        });
        h.runAtTickTime(220,()->{
            h.assertTrue(shark.getTarget()==player && shark.distanceToSqr(player)<initial*.6,"Retaliation did not actively pursue distant attacker");
            player.discard();shark.discard();h.succeed();
        });
    }
    @GameTest(template="crusher_combat_pool", batch="crusher_drowned", timeoutTicks=1000)
    public static void detectsAndHuntsDistantDrownedInThreeDimensions(GameTestHelper h) {
        var shark=h.spawn(ModEntities.CORAL_CRUSHER.get(),5,5,10);
        var drowned=h.spawn(EntityType.DROWNED,35,24,10); drowned.setNoAi(true); drowned.setNoGravity(true);
        h.runAtTickTime(40,()->{
            System.out.println("DROWNED_DETECTION target="+shark.getTarget()+" intended="+drowned
                    +" alive="+drowned.isAlive()+" water="+drowned.isInWater()+" distance="+shark.distanceToSqr(drowned));
            h.assertTrue(shark.getTarget()==drowned,"Drowned outside vanilla four-block vertical search was not detected");
        });
        h.succeedWhen(()->{
            h.assertTrue(drowned.getHealth()<20,"Detected distant drowned was never actually bitten");
            drowned.discard();shark.discard();
        });
    }
    @GameTest(template="crusher_combat_pool", batch="crusher_player_priority", timeoutTicks=200)
    public static void playerPreemptsDrownedAndNpcRetaliation(GameTestHelper h) {
        var shark=h.spawn(ModEntities.CORAL_CRUSHER.get(),5,5,10);
        var drowned=h.spawn(EntityType.DROWNED,30,20,10); drowned.setNoAi(true); drowned.setNoGravity(true);
        var player=CoralCrusherAiTests.player(h); var position=h.absoluteVec(new Vec3(12,5,10));
        pin(h,player,position,180);player.setGameMode(GameType.CREATIVE);
        h.runAtTickTime(40,()->{
            h.assertTrue(shark.getTarget()==drowned,"Expected drowned before survival player appears");
            player.setGameMode(GameType.SURVIVAL);
        });
        h.runAtTickTime(70,()->{
            h.assertTrue(shark.getTarget()==player,"Human player did not preempt drowned hunting");
            shark.hurt(h.getLevel().damageSources().mobAttack(drowned),2);
            h.assertTrue(shark.getTarget()==player,"Drowned retaliation demoted player priority");
        });
        h.runAtTickTime(90,()->{
            h.assertTrue(shark.getTarget()==player,"Player priority was not retained");player.discard();drowned.discard();shark.discard();h.succeed();
        });
    }
    @GameTest(template="coral_crusher_pool", batch="crusher_attack_choice", timeoutTicks=480)
    public static void closeBiteThenRepositionsForCharge(GameTestHelper h) {
        CoralCrusherAiTests.pool(h);
        var shark=h.spawn(ModEntities.CORAL_CRUSHER.get(),10,5,10);
        var player=CoralCrusherAiTests.player(h);pin(h,player,h.absoluteVec(new Vec3(12,5,10)),460);
        h.runAtTickTime(20,()->shark.hurt(h.getLevel().damageSources().playerAttack(player),2));
        var observed=EnumSet.noneOf(CoralCrusherEntity.Behavior.class);
        for(int tick=1;tick<=450;tick++)h.runAtTickTime(tick,()->observed.add(shark.getBehavior()));
        h.succeedWhen(()->{
            h.assertTrue(observed.containsAll(EnumSet.of(MELEE_WINDUP,RECOVER,REPOSITION,WINDUP,CHARGE)),"Missing close bite/reposition/charge decision: "+observed);
            h.assertTrue(player.getHealth()<20,"Attack choices did not inflict damage");player.discard();shark.discard();
        });
    }
}
