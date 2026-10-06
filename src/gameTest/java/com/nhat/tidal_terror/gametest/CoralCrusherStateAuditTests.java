package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherHuntGoal;
import java.util.EnumMap;
import java.util.EnumSet;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import static com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity.Behavior.*;

/** Audit real per-tick movement, controls, transitions and damage, not state labels alone. */
@GameTestHolder("tidalterror")
@PrefixGameTestTemplate(false)
public class CoralCrusherStateAuditTests {
    private static void sampleAt(GameTestHelper h, int tick, CoralCrusherEntity shark, ServerPlayer player, Runnable sample) {
        h.runAtTickTime(tick,()->{
            try { sample.run(); }
            catch(RuntimeException failure) {
                // Failed fixtures must not hunt swimmers in later test batches.
                shark.discard();player.discard();throw failure;
            }
        });
    }
    private static ServerPlayer swimmer(GameTestHelper h, Vec3 position, int ticks) {
        return swimmer(h, new Vec3[]{position}, ticks);
    }
    private static ServerPlayer swimmer(GameTestHelper h, Vec3[] position, int ticks) {
        var player = CoralCrusherAiTests.player(h);
        player.setGameMode(GameType.SURVIVAL);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        player.setHealth(200); player.setNoGravity(true); player.setPos(position[0]);
        for (int tick=1; tick<=ticks; tick++) h.runAtTickTime(tick, () -> {
            if (player.isRemoved()) return;
            player.doTick(); player.setPos(position[0]); player.setDeltaMovement(Vec3.ZERO); player.setAirSupply(300);
        });
        return player;
    }

    private static class Trace {
        final EnumMap<CoralCrusherEntity.Behavior, Integer> ticks = new EnumMap<>(CoralCrusherEntity.Behavior.class);
        final EnumMap<CoralCrusherEntity.Behavior, Double> travel = new EnumMap<>(CoralCrusherEntity.Behavior.class);
        CoralCrusherEntity.Behavior previous;
        Vec3 position;
        int age;
        void sample(CoralCrusherEntity shark) {
            var state=shark.getBehavior();
            if (state!=previous) {
                System.out.println("CRUSHER_STATE_TRANSITION "+previous+" -> "+state+" previousTicks="+age);
                age=0;
            }
            age++;
            ticks.merge(state,1,Integer::sum);
            if (position!=null && state==previous) travel.merge(state,position.distanceTo(shark.position()),Double::sum);
            previous=state; position=shark.position();
        }
        void report() {
            var rates=new EnumMap<CoralCrusherEntity.Behavior, Double>(CoralCrusherEntity.Behavior.class);
            travel.forEach((state,distance)->rates.put(state,distance/ticks.get(state)));
            System.out.println("CRUSHER_STATE_AUDIT ticks="+ticks+" travel="+travel+" blocksPerTick="+rates);
        }
    }

    @GameTest(template="crusher_combat_pool", batch="crusher_melee_motion_audit", timeoutTicks=320)
    public static void meleeRecoveryKeepsSwimmingWithoutExtraBites(GameTestHelper h) {
        meleeRecovery(h,false);
    }
    @GameTest(template="crusher_combat_pool", batch="crusher_blocked_recovery_audit", timeoutTicks=320)
    public static void meleeRecoveryDetoursAroundSolidReef(GameTestHelper h) {
        meleeRecovery(h,true);
    }
    private static void meleeRecovery(GameTestHelper h, boolean blocked) {
        if(blocked)for(int y=1;y<31;y++)for(int z=8;z<=12;z++)
            h.setBlock(new net.minecraft.core.BlockPos(24,y,z),net.minecraft.world.level.block.Blocks.STONE);
        var shark=h.spawn(ModEntities.CORAL_CRUSHER.get(),20,12,10);
        var player=swimmer(h,h.absoluteVec(new Vec3(22,12,10)),310);
        h.runAtTickTime(20,()->shark.hurt(h.getLevel().damageSources().playerAttack(player),2));
        final int[] recovery={0}, routeTicks={0}, idle={0}, longestIdle={0};
        final Vec3[] previous={null}; final double[] travel={0}, lateTravel={0};
        final float[] health={200}; final boolean[] melee={false};
        for(int tick=1;tick<=300;tick++) sampleAt(h,tick,shark,player,()->{
            if(shark.isRemoved())return;
            if(blocked)h.assertTrue(h.getLevel().noCollision(shark,shark.getBoundingBox()),"Recovery steered the shark into a solid reef wall");
            if(shark.getBehavior()==MELEE_WINDUP) melee[0]=true;
            if(melee[0] && shark.getBehavior()==RECOVER) {
                recovery[0]++;
                if(recovery[0]==1)health[0]=player.getHealth();
                double moved=previous[0]==null?0:previous[0].distanceTo(shark.position());
                travel[0]+=moved; if(recovery[0]>30)lateTravel[0]+=moved;
                if(!shark.getNavigation().isDone())routeTicks[0]++;
                if(recovery[0]>5 && moved<.01) idle[0]++; else idle[0]=0;
                longestIdle[0]=Math.max(longestIdle[0],idle[0]);
                h.assertTrue(player.getHealth()>=health[0],"Recovery dealt an extra bite");
                if(recovery[0]==55) {
                    System.out.println("CRUSHER_MELEE_RECOVERY travel="+travel[0]+" lateTravel="+lateTravel[0]
                            +" liveRoutes="+routeTicks[0]+" longestIdle="+longestIdle[0]);
                    h.assertTrue(travel[0]>1.5 && lateTravel[0]>.5,"Melee recovery must swim throughout the cooldown, not coast then freeze");
                    h.assertTrue(routeTicks[0]>=40 && longestIdle[0]<=12,"Recovery has an extended idle gap");
                    player.discard();shark.discard();h.succeed();
                }
                previous[0]=shark.position();
            }
        });
    }

    @GameTest(template="crusher_combat_pool", batch="crusher_charge_speed_audit", timeoutTicks=600)
    public static void chargeRecoveryUsesSlowerNativeSteering(GameTestHelper h) {
        var shark=h.spawn(ModEntities.CORAL_CRUSHER.get(),10,12,10);
        var player=swimmer(h,h.absoluteVec(new Vec3(32,12,10)),590);
        h.runAtTickTime(20,()->shark.hurt(h.getLevel().damageSources().playerAttack(player),2));
        var trace=new Trace(); final int[] chargeSamples={0}, recoverSamples={0};
        final double[] chargeSpeed={0}, recoverSpeed={0};
        for(int tick=1;tick<=580;tick++)sampleAt(h,tick,shark,player,()->{
            if(shark.isRemoved())return;
            trace.sample(shark);
            if(trace.age>3 && !shark.getNavigation().isDone()) {
                if(shark.getBehavior()==CHARGE) {chargeSamples[0]++;chargeSpeed[0]+=shark.getMoveControl().getSpeedModifier();}
                if(shark.getBehavior()==RECOVER) {recoverSamples[0]++;recoverSpeed[0]+=shark.getMoveControl().getSpeedModifier();}
            }
            if(recoverSamples[0]>=20) {
                trace.report();
                h.assertTrue(chargeSamples[0]>=5,"Audit did not observe a sustained charge");
                double fast=chargeSpeed[0]/chargeSamples[0], slow=recoverSpeed[0]/recoverSamples[0];
                System.out.println("CRUSHER_NATIVE_SPEED charge="+fast+" recovery="+slow);
                h.assertTrue(fast>slow*1.4 && Math.abs(slow-.85)<.001,"Recovery retained charge speed instead of easing into cruise");
                h.assertTrue(trace.travel.getOrDefault(RECOVER,0.0)>1,"Slower recovery did not actually swim");
                player.discard();shark.discard();h.succeed();
            }
        });
    }

    @GameTest(template="crusher_combat_pool", batch="crusher_cycle_audit", timeoutTicks=1400)
    public static void auditsCompleteEncounterAndRetreat(GameTestHelper h) {
        var shark=h.spawn(ModEntities.CORAL_CRUSHER.get(),12,12,10);
        var position=new Vec3[]{h.absoluteVec(new Vec3(25,12,10))};
        var player=swimmer(h,position,1390);
        // Let patrol actually run before revealing a survival swimmer.
        player.setGameMode(GameType.CREATIVE);
        h.runAtTickTime(80,()->player.setGameMode(GameType.SURVIVAL));
        var trace=new Trace(); final float[] health={200};
        final boolean[] bitten={false}, fleeing={false}, closedIn={false}, spacedOut={false};
        final int[] fleeTicks={0}; final double[] fleeDistance={0};
        for(int tick=1;tick<=1380;tick++)sampleAt(h,tick,shark,player,()->{
            if(shark.isRemoved())return;
            var previous=trace.previous;
            int previousAge=trace.age;
            trace.sample(shark);
            if(previous!=shark.getBehavior()) {
                if(previous==WINDUP || previous==MELEE_WINDUP)
                    h.assertTrue(previousAge>=CoralCrusherHuntGoal.WINDUP_TICKS-1,"Attack skipped its full telegraph");
                if(previous==RECOVER && shark.getBehavior()!=FLEE)
                    h.assertTrue(previousAge>=CoralCrusherHuntGoal.RECOVERY_TICKS-1,"Recovery skipped its damage cooldown");
            }
            if(trace.age>3 && !shark.getNavigation().isDone()) {
                double expected=switch(shark.getBehavior()) {
                    case PATROL -> .8;
                    case INVESTIGATE -> .95;
                    case CIRCLE -> .9;
                    case CHARGE -> 1.5;
                    case RECOVER -> .85;
                    case REPOSITION -> 1.0;
                    case FLEE -> 1.25;
                    default -> 0;
                };
                h.assertTrue(Math.abs(shark.getMoveControl().getSpeedModifier()-expected)<.001,
                        "Wrong native steering speed for "+shark.getBehavior());
                h.assertTrue(Math.abs(shark.getSpeed()-expected*shark.getAttributeValue(Attributes.MOVEMENT_SPEED)*.02)<.00001,
                        "Swimming control did not apply the state's speed");
            }
            if(player.getHealth()<health[0] && player.getLastDamageSource()!=null
                    && player.getLastDamageSource().getEntity()==shark
                    && player.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)) {
                h.assertTrue(previous==CHARGE || previous==MELEE_WINDUP,"Damage outside an attack state: "+previous);
                h.assertTrue(shark.getBehavior()==RECOVER,"A bite did not transition to recovery"); bitten[0]=true;
            }
            health[0]=player.getHealth();
            if(!spacedOut[0] && shark.getBehavior()==WINDUP) {
                // Require a real run-up rather than judging a valid point-blank
                // charge by a distance threshold intended for sustained swimming.
                position[0]=shark.position().add(8,0,0);spacedOut[0]=true;
            }
            if(!closedIn[0] && trace.ticks.containsKey(CHARGE) && shark.getBehavior()==RECOVER && trace.age==45) {
                // A swimmer closing into native reach late in a charge recovery
                // should provoke the alternate close bite, without forcing AI state.
                position[0]=shark.position().add(shark.getDeltaMovement().normalize().scale(2));
                closedIn[0]=true;
            }
            if(!fleeing[0] && bitten[0] && trace.travel.getOrDefault(REPOSITION,0.0)>.5
                    && trace.ticks.keySet().containsAll(EnumSet.of(PATROL,INVESTIGATE,CIRCLE,WINDUP,CHARGE,RECOVER,MELEE_WINDUP,REPOSITION))) {
                shark.setHealth(38);shark.hurt(h.getLevel().damageSources().playerAttack(player),4);
                fleeing[0]=true;fleeDistance[0]=shark.distanceToSqr(player);
            }
            if(fleeing[0] && ++fleeTicks[0]>=80) {
                trace.report();
                h.assertTrue(shark.getBehavior()==FLEE && shark.getTarget()==null,"Retreat did not override the combat cycle");
                h.assertTrue(shark.distanceToSqr(player)>fleeDistance[0]+4,"Flee state did not create actual separation");
                for(var state:EnumSet.of(PATROL,INVESTIGATE,CIRCLE,CHARGE,RECOVER,REPOSITION,FLEE))
                    h.assertTrue(trace.travel.getOrDefault(state,0.0)>.5,"State entered but did not swim: "+state);
                player.discard();shark.discard();h.succeed();
            }
        });
    }

    @GameTest(template="crusher_combat_pool", batch="crusher_moving_target_audit", timeoutTicks=500)
    public static void cancelledCloseBitePursuesMovingSwimmer(GameTestHelper h) {
        var shark=h.spawn(ModEntities.CORAL_CRUSHER.get(),20,12,10);
        var position=new Vec3[]{h.absoluteVec(new Vec3(22,12,10))};
        var player=swimmer(h,position[0],1);
        h.runAtTickTime(20,()->shark.hurt(h.getLevel().damageSources().playerAttack(player),2));
        final boolean[] dodged={false}; final int[] afterDodge={0};
        for(int tick=2;tick<=480;tick++)sampleAt(h,tick,shark,player,()->{
            if(shark.isRemoved())return;
            player.doTick();player.setPos(position[0]);player.setDeltaMovement(Vec3.ZERO);player.setAirSupply(300);
            if(!dodged[0] && shark.getBehavior()==MELEE_WINDUP) {
                position[0]=position[0].add(12,6,0);player.setPos(position[0]);dodged[0]=true;
            }
            if(dodged[0] && ++afterDodge[0]<20)h.assertTrue(player.getHealth()==200,"Close bite hit a swimmer who left its range");
            if(dodged[0] && player.getHealth()<200) {
                h.assertTrue(shark.getTarget()==player,"Lost the moving human target");
                h.assertTrue(shark.getY()>h.absoluteVec(new Vec3(0,15,0)).y,"Reposition did not follow the swimmer vertically");
                player.discard();shark.discard();h.succeed();
            }
        });
    }
}
