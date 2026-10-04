package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("tidalterror")
@PrefixGameTestTemplate(false)
public class CoralCrusherHeightTests {
    // Keep effective AI, native swimming controls and travel active, but supply
    // no height command. NoAI would bypass the very buoyancy being tested.
    private static class HoldingCrusher extends CoralCrusherEntity {
        HoldingCrusher(Level level) { super(ModEntities.CORAL_CRUSHER.get(), level); }
        @Override protected void registerGoals() { }
    }

    private static void holdDepth(GameTestHelper helper, boolean targeting) {
        CoralCrusherAiTests.pool(helper);
        var shark = new HoldingCrusher(helper.getLevel());
        shark.setPos(helper.absoluteVec(new Vec3(10.5, 5, 10.5)));
        shark.setPersistenceRequired();
        helper.getLevel().addFreshEntity(shark);
        if (targeting) {
            var target = helper.spawn(net.minecraft.world.entity.EntityType.DROWNED, 15, 5, 10);
            target.setNoAi(true);
            target.setNoGravity(true);
            shark.setTarget(target);
        }
        double startY = shark.getY();
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(shark.isInWater() && !shark.isNoAi(), "Height check bypassed aquatic AI");
            helper.assertTrue(Math.abs(shark.getY() - startY) < .025,
                    "Uncommanded vertical drift; targeting=" + targeting + ", dy=" + (shark.getY() - startY));
            helper.assertTrue(Math.abs(shark.getDeltaMovement().y) < .001,
                    "Height bias remains without a navigation command");
            if (shark.getTarget() != null) shark.getTarget().discard();
            shark.discard();
            helper.succeed();
        });
    }

    @GameTest(template="coral_crusher_pool", batch="crusher_height", timeoutTicks=100)
    public static void holdsDepthWithoutTarget(GameTestHelper helper) { holdDepth(helper, false); }

    @GameTest(template="coral_crusher_pool", batch="crusher_height", timeoutTicks=100)
    public static void holdsDepthWithTarget(GameTestHelper helper) { holdDepth(helper, true); }
}
