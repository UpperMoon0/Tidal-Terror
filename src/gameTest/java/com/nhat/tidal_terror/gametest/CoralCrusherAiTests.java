package com.nhat.tidal_terror.gametest;

import com.mojang.authlib.GameProfile;
import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
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

@GameTestHolder("tidalterror")
@PrefixGameTestTemplate(false)
public class CoralCrusherAiTests {
    static ServerPlayer player(GameTestHelper helper) {
        // Forge 47.2.0's login hook requires a real Netty channel; register the
        // native ServerPlayer directly in this isolated level instead of logging in.
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "crusher-test"));
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(),
                new Connection(PacketFlow.SERVERBOUND), player) {
            @Override public void send(Packet<?> packet) { }
            @Override public void send(Packet<?> packet, PacketSendListener listener) { }
        };
        helper.getLevel().addNewPlayer(player);
        return player;
    }

    static void pool(GameTestHelper helper) {
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 12; y++) {
                for (int z = 0; z < 20; z++) {
                    helper.setBlock(new BlockPos(x, y, z),
                            x == 0 || x == 19 || y == 0 || y == 11 || z == 0 || z == 19
                                    ? Blocks.STONE : Blocks.WATER);
                }
            }
        }
    }

    private static void chase(GameTestHelper helper, int startY, int targetY) {
        pool(helper);
        CoralCrusherEntity crusher = helper.spawn(ModEntities.CORAL_CRUSHER.get(), 4, startY, 10);
        crusher.setPersistenceRequired();
        ServerPlayer player = player(helper);
        player.setGameMode(GameType.SURVIVAL);
        Vec3 target = helper.absoluteVec(new Vec3(15.5, targetY, 10.5));
        player.setPos(target);
        player.setNoGravity(true);
        double initialDistance = crusher.distanceToSqr(player);
        float initialHealth = player.getHealth();
        java.util.Set<CoralCrusherEntity.Behavior> observed = java.util.EnumSet.noneOf(CoralCrusherEntity.Behavior.class);
        for (int tick = 1; tick <= 480; tick++) {
            helper.runAtTickTime(tick, () -> {
                observed.add(crusher.getBehavior());
                player.doTick();
                player.setPos(target);
                player.setDeltaMovement(Vec3.ZERO);
                player.setAirSupply(300);
            });
        }
        helper.runAtTickTime(40, () -> helper.assertTrue(crusher.getTarget() == player,
                "Crusher must acquire a survival player through its target selector"));
        for (int tick = 20; tick <= 280; tick += 20) {
            int sampleTick = tick;
            helper.runAtTickTime(tick, () -> System.out.println("CRUSHER_CHASE startY=" + startY
                    + " tick=" + sampleTick + " pos=" + crusher.position() + " target=" + target
                    + " distance=" + crusher.distanceToSqr(player) + " health=" + player.getHealth()
                    + " pathDone=" + crusher.getNavigation().isDone()
                    + " next=" + (crusher.getNavigation().isDone() ? "none"
                    : crusher.getNavigation().getPath().getNextNodePos())));
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(player.getHealth() < initialHealth, "Crusher must inflict melee damage");
            helper.assertTrue(observed.containsAll(java.util.EnumSet.of(CoralCrusherEntity.Behavior.INVESTIGATE,
                    CoralCrusherEntity.Behavior.CIRCLE)) && (observed.contains(CoralCrusherEntity.Behavior.MELEE_WINDUP)
                    || observed.containsAll(java.util.EnumSet.of(CoralCrusherEntity.Behavior.WINDUP,
                    CoralCrusherEntity.Behavior.CHARGE))), "Bite must follow investigation, circling, and a telegraphed attack: " + observed);
            helper.assertTrue(crusher.getTarget() == player, "Damage must occur while targeting the player");
            helper.assertTrue(crusher.distanceToSqr(player) < initialDistance * 0.6,
                    "Melee must follow pursuit");
            helper.assertTrue(startY < targetY ? crusher.getY() > helper.absolutePos(new BlockPos(0, startY, 0)).getY() + 1
                            : crusher.getY() < helper.absolutePos(new BlockPos(0, startY, 0)).getY() - 1,
                    "Melee must follow vertical pursuit");
            player.discard();
            crusher.discard();
        });
    }

    @GameTest(template = "coral_crusher_pool", timeoutTicks = 500)
    public static void pursuesPlayerAboveAndAttacks(GameTestHelper helper) {
        chase(helper, 3, 8);
    }

    @GameTest(template = "coral_crusher_pool", timeoutTicks = 500)
    public static void pursuesPlayerBelowAndAttacks(GameTestHelper helper) {
        chase(helper, 8, 3);
    }

    @GameTest(template = "coral_crusher_pool", timeoutTicks = 120)
    public static void ignoresCreativeAndSpectator(GameTestHelper helper) {
        pool(helper);
        CoralCrusherEntity crusher = helper.spawn(ModEntities.CORAL_CRUSHER.get(), 10, 5, 10);
        ServerPlayer player = player(helper);
        player.setPos(helper.absoluteVec(new Vec3(12, 5, 10)));
        player.setNoGravity(true);
        player.setGameMode(GameType.CREATIVE);
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(crusher.getTarget() == null, "Creative player must not be targeted");
            player.setGameMode(GameType.SPECTATOR);
        });
        helper.runAtTickTime(80, () -> {
            helper.assertTrue(crusher.getTarget() == null, "Spectator must not be targeted");
            player.discard();
            crusher.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "coral_crusher_pool", timeoutTicks = 200)
    public static void wandersUsingWaterNavigation(GameTestHelper helper) {
        pool(helper);
        CoralCrusherEntity crusher = helper.spawn(ModEntities.CORAL_CRUSHER.get(), 10, 5, 10);
        Vec3 start = crusher.position();
        for (int tick = 20; tick <= 180; tick += 20) helper.runAtTickTime(tick, () ->
                System.out.println("CRUSHER_PATROL position=" + crusher.position() + " home=" + crusher.getTerritory()
                        + " state=" + crusher.getBehavior() + " pathDone=" + crusher.getNavigation().isDone()));
        helper.succeedWhen(() -> {
            helper.assertTrue(crusher.getTarget() == null, "Wandering must not require a target");
            helper.assertTrue(crusher.position().distanceToSqr(start) > 4,
                    "Crusher must swim away from its start");
            helper.assertTrue(!crusher.getNavigation().isDone(), "Wandering must use a water path");
            crusher.discard();
        });
    }
}
