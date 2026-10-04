package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("tidalterror")
@PrefixGameTestTemplate(false)
public class CoralCrusherSkinTests {
    @GameTest(template = "coral_crusher_pool", timeoutTicks = 40)
    public static void spawnDepthAndPersistentSkin(GameTestHelper helper) {
        var level = helper.getLevel();
        var reef = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ReefWorldgen.BIOME);
        // Real native blocks / heightmap, with only the biome replaced. The
        // GameTest template's flat biome does not naturally contain this reef.
        var view = (ServerLevelAccessor) java.lang.reflect.Proxy.newProxyInstance(
                CoralCrusherSkinTests.class.getClassLoader(), new Class[]{ServerLevelAccessor.class},
                (proxy, method, args) -> method.getName().equals("getBiome") ? reef : method.invoke(level, args));
        BlockPos floor = helper.absolutePos(new BlockPos(10, 1, 10));
        level.setBlock(floor, Blocks.SAND.defaultBlockState(), 2);
        for (int y = 1; y <= 50; y++) level.setBlock(floor.above(y), Blocks.WATER.defaultBlockState(), 2);
        // A high coral is not the seabed, even though OCEAN_FLOOR sees it.
        level.setBlock(floor.above(30), Blocks.TUBE_CORAL_BLOCK.defaultBlockState(), 2);
        helper.assertTrue(CoralCrusherEntity.sandyAtSpawn(view, floor.above(24)), "24m clearance must be sandy");
        helper.assertTrue(!CoralCrusherEntity.sandyAtSpawn(view, floor.above(25)), "25m clearance must be blue");
        helper.assertTrue(!CoralCrusherEntity.sandyAtSpawn(view, floor.above(40)), "High coral must not count as seabed");

        var sandy = ModEntities.CORAL_CRUSHER.get().create(level);
        sandy.moveTo(floor.getX() + .5, floor.getY() + 10, floor.getZ() + .5, 0, 0);
        sandy.finalizeSpawn(view, level.getCurrentDifficultyAt(sandy.blockPosition()), MobSpawnType.NATURAL, null, null);
        helper.assertTrue(sandy.isSandy(), "Bottom spawn must assign sandy skin");
        helper.assertTrue(sandy.getEntityData().getNonDefaultValues() != null, "Variant must use synchronized entity data");
        sandy.setPos(sandy.getX(), floor.getY() + 40, sandy.getZ());
        helper.assertTrue(sandy.isSandy(), "Swimming higher must not recolor the shark");
        CompoundTag saved = new CompoundTag();
        sandy.saveWithoutId(saved);
        var restored = ModEntities.CORAL_CRUSHER.get().create(level);
        restored.load(saved);
        helper.assertTrue(restored.isSandy(), "Save/load must preserve sandy skin at higher altitude");

        var blue = ModEntities.CORAL_CRUSHER.get().create(level);
        blue.moveTo(floor.getX() + .5, floor.getY() + 40, floor.getZ() + .5, 0, 0);
        blue.finalizeSpawn(view, level.getCurrentDifficultyAt(blue.blockPosition()), MobSpawnType.NATURAL, null, null);
        helper.assertTrue(!blue.isSandy(), "Higher spawn must assign blue skin");
        saved.remove("Skin");
        restored.load(saved);
        helper.assertTrue(!restored.isSandy(), "Existing untagged sharks must stay blue");
        saved.putString("Skin", "unknown");
        restored.load(saved);
        helper.assertTrue(!restored.isSandy(), "Unknown saved skin must safely fall back to blue");
        helper.succeed();
    }
}
