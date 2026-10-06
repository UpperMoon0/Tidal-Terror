package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherHuntGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("tidalterror")
@PrefixGameTestTemplate(false)
public class CoralCrusherSurvivalTests {
    @GameTest(template = "coral_crusher_pool", batch = "crusher_prey", timeoutTicks = 460)
    public static void acquiresAndHuntsFish(GameTestHelper helper) {
        CoralCrusherAiTests.pool(helper);
        var shark = helper.spawn(ModEntities.CORAL_CRUSHER.get(), 5, 5, 10);
        var fish = helper.spawn(net.minecraft.world.entity.EntityType.COD, 14, 5, 10);
        fish.setNoAi(true); fish.setNoGravity(true);
        helper.runAtTickTime(10, () -> {
            // Accelerate only the native random acquisition checks with a fixed
            // seed, rather than assigning a target or simulating the controller.
            shark.getRandom().setSeed(7654321);
            for (int attempt = 0; attempt < 256 && shark.getTarget() != fish; attempt++)
                for (var wrapped : shark.targetSelector.getAvailableGoals()) {
                    var goal = wrapped.getGoal();
                    if (goal.canUse()) goal.start();
                }
            helper.assertTrue(shark.getTarget() == fish, "Native target goal must acquire fish prey");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(!fish.isAlive(), "Fish hunting must result in actual native bite damage");
            shark.discard();
        });
    }

    @GameTest(template = "coral_crusher_pool", batch = "crusher_disengage", timeoutTicks = 360)
    public static void escapedTargetGetsReacquisitionCooldown(GameTestHelper helper) {
        CoralCrusherAiTests.pool(helper);
        var shark = helper.spawn(ModEntities.CORAL_CRUSHER.get(), 5, 5, 10);
        var player = CoralCrusherAiTests.player(helper);
        player.setGameMode(GameType.SURVIVAL);
        var nearby = helper.absoluteVec(new Vec3(14, 5, 10));
        var far = helper.absoluteVec(new Vec3(100, 5, 10));
        player.setPos(nearby); player.setNoGravity(true);
        for (int tick = 1; tick <= 350; tick++) {
            int now = tick;
            helper.runAtTickTime(tick, () -> {
                player.doTick(); player.setAirSupply(300); player.setDeltaMovement(Vec3.ZERO);
                player.setPos(now >= 60 && now <= 100 ? far : nearby);
                if (now == 50) helper.assertTrue(shark.getTarget() == player, "Must first acquire the swimmer");
                if (now == 150) helper.assertTrue(shark.getTarget() == null
                        && shark.getBehavior() == CoralCrusherEntity.Behavior.PATROL,
                        "Escaping and returning must not bypass the disengage cooldown");
                if (now == 340) {
                    helper.assertTrue(shark.getTarget() == player, "Must allow reacquisition after cooldown");
                    player.discard(); shark.discard(); helper.succeed();
                }
            });
        }
    }

    private static void pin(GameTestHelper helper, net.minecraft.server.level.ServerPlayer player,
            Vec3 position, int until) {
        player.setNoGravity(true);
        for (int tick = 1; tick <= until; tick++) helper.runAtTickTime(tick, () -> {
            player.doTick(); player.setPos(position); player.setDeltaMovement(Vec3.ZERO); player.setAirSupply(300);
        });
    }

    @GameTest(template = "coral_crusher_pool", batch = "crusher_telegraph", timeoutTicks = 460)
    public static void telegraphsOneBiteAndRecovers(GameTestHelper helper) {
        CoralCrusherAiTests.pool(helper);
        var shark = helper.spawn(ModEntities.CORAL_CRUSHER.get(), 5, 5, 10);
        var player = CoralCrusherAiTests.player(helper);
        player.setGameMode(GameType.SURVIVAL);
        var position = helper.absoluteVec(new Vec3(14, 5, 10));
        player.setPos(position); pin(helper, player, position, 450);
        final int[] windup = {0}, hit = {-1};
        final boolean[] bled = {false};
        final float[] health = {player.getHealth()};
        final double[] circleTravel = {0};
        final Vec3[] circleStart = {null};
        for (int tick = 1; tick <= 450; tick++) {
            int now = tick;
            helper.runAtTickTime(tick, () -> {
                if (shark.getBehavior() == CoralCrusherEntity.Behavior.WINDUP
                        || shark.getBehavior() == CoralCrusherEntity.Behavior.MELEE_WINDUP) windup[0]++;
                if (shark.getBehavior() == CoralCrusherEntity.Behavior.CIRCLE) {
                    if (circleStart[0] == null) circleStart[0] = shark.position();
                    circleTravel[0] = Math.max(circleTravel[0], shark.position().distanceToSqr(circleStart[0]));
                }
                if (player.getHealth() < health[0] && player.getLastDamageSource()!=null
                        && player.getLastDamageSource().getEntity()==shark
                        && player.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC)) bled[0] = true;
                // Credited bleed damage during recovery is not another physical bite.
                if (player.getHealth() < health[0] && player.getLastDamageSource()!=null
                        && player.getLastDamageSource().getEntity()==shark
                        && player.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)) {
                    System.out.println("CRUSHER_BITE tick="+now+" state="+shark.getBehavior()+" health="+player.getHealth()
                            +" previous="+health[0]+" firstHit="+hit[0]);
                    helper.assertTrue(windup[0] >= CoralCrusherHuntGoal.WINDUP_TICKS - 1,
                            "Damage occurred without a full telegraph");
                    helper.assertTrue(circleTravel[0] > 1, "Circling must actually move the shark");
                    helper.assertTrue(player.hasEffect(com.nhat.tidal_terror.effects.ModEffects.REEF_BLEEDING.get()), "Telegraphed bite did not cause bleeding");
                    if (hit[0] < 0) hit[0] = now;
                    else helper.assertTrue(now - hit[0] >= CoralCrusherHuntGoal.RECOVERY_TICKS,
                            "Multiple bites during recovery");
                }
                health[0] = player.getHealth();
                if (hit[0] >= 0 && now == hit[0] + 40) {
                    helper.assertTrue(bled[0], "The bite did not deal delayed bleeding during recovery");
                    helper.assertTrue(shark.getBehavior() == CoralCrusherEntity.Behavior.RECOVER,
                            "The committed bite must be followed by recovery");
                    player.discard(); shark.discard(); helper.succeed();
                }
            });
        }
    }

    @GameTest(template = "coral_crusher_pool", batch = "crusher_healing", timeoutTicks = 360)
    public static void cannotRegenerateOutOfWater(GameTestHelper helper) {
        CoralCrusherAiTests.pool(helper);
        var shark = helper.spawn(ModEntities.CORAL_CRUSHER.get(), 10, 5, 10);
        shark.setHealth(35);
        shark.setNoGravity(true);
        var dryPosition = helper.absoluteVec(new Vec3(10, 15, 10));
        for (int x = 8; x <= 12; x++) for (int y = 13; y <= 18; y++) for (int z = 8; z <= 12; z++)
            helper.setBlock(new BlockPos(x, y, z), x == 8 || x == 12 || y == 13 || y == 18 || z == 8 || z == 12
                    ? net.minecraft.world.level.block.Blocks.STONE : net.minecraft.world.level.block.Blocks.AIR);
        shark.setPos(dryPosition);
        // Keep the fixture dry; normal escape navigation may seek water.
        for (int tick = 1; tick <= 330; tick++) helper.runAtTickTime(tick, () -> {
            shark.setPos(dryPosition); shark.setDeltaMovement(Vec3.ZERO);
        });
        helper.runAtTickTime(320, () -> {
            helper.assertTrue(!shark.isInWaterOrBubble(), "Dry test must remain outside water");
            helper.assertTrue(shark.getHealth() <= 35, "Dry refuge cannot regenerate");
            shark.discard(); helper.succeed();
        });
    }

    @GameTest(template = "coral_crusher_pool", batch = "crusher_overhead_escape", timeoutTicks = 180)
    public static void fleesFromThreatDirectlyAbove(GameTestHelper helper) {
        CoralCrusherAiTests.pool(helper);
        var shark = helper.spawn(ModEntities.CORAL_CRUSHER.get(), 10, 2, 10);
        var player = CoralCrusherAiTests.player(helper);
        player.setGameMode(GameType.SURVIVAL);
        var position = helper.absoluteVec(new Vec3(10, 7, 10));
        player.setPos(position); pin(helper, player, position, 170);
        shark.setHealth(35);
        double distance = shark.distanceToSqr(player);
        helper.runAtTickTime(140, () -> {
            helper.assertTrue(shark.isRetreating() && shark.distanceToSqr(player) > distance + 4,
                    "A threat above the seabed must allow horizontal escape, not trap the shark against the floor");
            player.discard(); shark.discard(); helper.succeed();
        });
    }

    @GameTest(template = "coral_crusher_pool", batch = "crusher_low_health", timeoutTicks = 440)
    public static void lowHealthFleesWithoutAttackingOrHealing(GameTestHelper helper) {
        CoralCrusherAiTests.pool(helper);
        var shark = helper.spawn(ModEntities.CORAL_CRUSHER.get(), 10, 5, 10);
        var player = CoralCrusherAiTests.player(helper);
        player.setGameMode(GameType.SURVIVAL);
        var position = helper.absoluteVec(new Vec3(13, 5, 10));
        player.setPos(position); pin(helper, player, position, 430);
        shark.setHealth(35);
        double startDistance = shark.distanceToSqr(player);
        float health = player.getHealth();
        helper.runAtTickTime(400, () -> {
            helper.assertTrue(shark.isRetreating(), "Low health must preempt hunting");
            helper.assertTrue(shark.getTarget() == null, "Retreat must suppress reacquisition");
            helper.assertTrue(player.getHealth() == health, "Retreating shark must not bite");
            helper.assertTrue(shark.getHealth() == 35, "Nearby threats must prevent healing");
            helper.assertTrue(shark.distanceToSqr(player) > startDistance + 9, "Must actually swim away");
            player.discard(); shark.discard(); helper.succeed();
        });
    }

    @GameTest(template = "coral_crusher_pool", batch = "crusher_injury_interrupt", timeoutTicks = 420)
    public static void injuryInterruptsWindupAndEscapes(GameTestHelper helper) {
        CoralCrusherAiTests.pool(helper);
        var shark = helper.spawn(ModEntities.CORAL_CRUSHER.get(), 5, 5, 10);
        var player = CoralCrusherAiTests.player(helper);
        player.setGameMode(GameType.SURVIVAL);
        var position = helper.absoluteVec(new Vec3(14, 5, 10));
        player.setPos(position); pin(helper, player, position, 410);
        final int[] injuredAt = {-1};
        final double[] distance = {0};
        for (int tick = 1; tick <= 410; tick++) {
            int now = tick;
            helper.runAtTickTime(tick, () -> {
                if (injuredAt[0] < 0 && (shark.getBehavior() == CoralCrusherEntity.Behavior.WINDUP
                        || shark.getBehavior() == CoralCrusherEntity.Behavior.MELEE_WINDUP)) {
                    distance[0] = shark.distanceToSqr(player);
                    shark.setHealth(38);
                    helper.assertTrue(shark.hurt(helper.getLevel().damageSources().playerAttack(player), 4),
                            "Test injury must be accepted by native damage");
                    injuredAt[0] = now;
                    helper.assertTrue(shark.isRetreating(), "Injury must immediately cancel windup");
                }
                if (injuredAt[0] >= 0 && now == injuredAt[0] + 80) {
                    helper.assertTrue(shark.isRetreating() && shark.getTarget() == null, "Cannot reacquire during retreat");
                    helper.assertTrue(player.getHealth() == 20, "Cancelled attack must not deal damage");
                    helper.assertTrue(shark.distanceToSqr(player) > distance[0] + 4, "Interrupted hunter must move away");
                    player.discard(); shark.discard(); helper.succeed();
                }
            });
        }
    }

    @GameTest(template = "coral_crusher_pool", batch = "crusher_healing", timeoutTicks = 760)
    public static void safeHealingIsSlowAndNewDamageResetsIt(GameTestHelper helper) {
        CoralCrusherAiTests.pool(helper);
        var shark = helper.spawn(ModEntities.CORAL_CRUSHER.get(), 10, 5, 10);
        shark.setHealth(35);
        helper.runAtTickTime(190, () -> helper.assertTrue(shark.getHealth() == 35,
                "No healing before safe delay"));
        helper.runAtTickTime(400, () -> {
            helper.assertTrue(shark.getHealth() >= 36 && shark.getHealth() <= 37,
                    "Safe healing must restore only 1 HP per four seconds; health=" + shark.getHealth());
            helper.assertTrue(shark.isRetreating(), "Crossing 30% must not resume aggression before 60%");
            // Apply actual native damage, not just a health assignment.
            shark.hurt(helper.getLevel().damageSources().generic(), 2);
        });
        final float[] afterDamage = {0};
        helper.runAtTickTime(401, () -> afterDamage[0] = shark.getHealth());
        helper.runAtTickTime(650, () -> helper.assertTrue(shark.getHealth() == afterDamage[0],
                "Fresh damage must restart the quiet delay and heal interval"));
        helper.runAtTickTime(730, () -> {
            helper.assertTrue(shark.getHealth() == afterDamage[0] + 1, "Healing must resume slowly after new safe period");
            shark.discard(); helper.succeed();
        });
    }

    @GameTest(template = "coral_crusher_pool", batch = "crusher_healing", timeoutTicks = 360)
    public static void retreatPersistsAndResumesOnlyWhenSafe(GameTestHelper helper) {
        CoralCrusherAiTests.pool(helper);
        var shark = helper.spawn(ModEntities.CORAL_CRUSHER.get(), 10, 5, 10);
        shark.setHealth(35);
        helper.runAtTickTime(10, () -> {
            var saved = new net.minecraft.nbt.CompoundTag(); shark.saveWithoutId(saved);
            var restored = ModEntities.CORAL_CRUSHER.get().create(helper.getLevel());
            restored.load(saved);
            helper.assertTrue(restored.isRetreating(), "Reload must retain retreat hysteresis");
            helper.assertTrue(restored.getTerritory().equals(shark.getTerritory()), "Reload must retain territory");
            restored.discard();
            shark.setHealth(72);
        });
        helper.runAtTickTime(100, () -> helper.assertTrue(shark.isRetreating(), "Health alone cannot bypass safety delay"));
        helper.runAtTickTime(300, () -> {
            helper.assertTrue(!shark.isRetreating(), "Safe recovered shark must return to patrol");
            shark.discard(); helper.succeed();
        });
    }
}
