package com.nhat.tidal_terror.gametest;
import com.nhat.tidal_terror.effects.ModEffects;
import com.nhat.tidal_terror.items.ModEquipment;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public final class PortEquipmentTests implements net.fabricmc.fabric.api.gametest.v1.FabricGameTest {
    private static net.minecraft.world.entity.monster.Zombie victim(GameTestHelper h) {
        var target=h.spawn(EntityType.ZOMBIE,10,4,10);target.setNoAi(true);target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);return target;
    }
    @GameTest(template="tidalterror:reef_life_pool",timeoutTicks=130)
    public static void refreshRetainsClockAndUpdatedDamage(GameTestHelper h) {
        var target=victim(h);ModEffects.applyBleeding(target,null,80,0);
        h.runAfterDelay(18,()->ModEffects.applyBleeding(target,null,80,2));
        h.runAfterDelay(39,()->h.assertTrue(target.getHealth()==100,"Early bleed"));
        h.runAfterDelay(41,()->h.assertTrue(target.getHealth()==98,"Refresh reset the clock or ignored power"));
        h.runAfterDelay(81,()->h.assertTrue(target.getHealth()==96,"Second scheduled hit missing"));
        h.runAfterDelay(105,()->{
            h.assertTrue(!target.hasEffect(ModEffects.REEF_BLEEDING.get()),"Refreshed effect did not expire");
            h.assertTrue(com.nhat.tidal_terror.platform.ReefEntityData.get(target).isEmpty(),"Expired bleed retained clock/owner");h.succeed();
        });
    }
    @GameTest(template="tidalterror:reef_life_pool",timeoutTicks=100)
    public static void independentCountdownSurvivesReload(GameTestHelper h) {
        var target=victim(h);ModEffects.applyBleeding(target,null,80,0);
        net.minecraft.world.entity.monster.Zombie[] restored={null};
        h.runAfterDelay(15,()->{
            var saved=target.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            int before=com.nhat.tidal_terror.platform.ReefEntityData.get(target).getInt("tidalterror:bleeding_pulse_ticks");
            target.discard();restored[0]=EntityType.ZOMBIE.create(h.getLevel());restored[0].load(saved);h.getLevel().addFreshEntity(restored[0]);
            h.assertTrue(com.nhat.tidal_terror.platform.ReefEntityData.get(restored[0]).getInt("tidalterror:bleeding_pulse_ticks")==before,"Reload reset independent clock");
        });
        h.runAfterDelay(45,()->{h.assertTrue(restored[0].getHealth()==99,"Reload postponed first hit");h.succeed();});
    }
    @GameTest(template="tidalterror:reef_life_pool",timeoutTicks=110)
    public static void fangArrowBleedsOnLand(GameTestHelper h) {
        var target=victim(h);var arrow=new com.nhat.tidal_terror.entities.FangArrowEntity(com.nhat.tidal_terror.entities.ModEntities.FANG_ARROW.get(),h.getLevel());
        try {
            var hit=com.nhat.tidal_terror.entities.FangArrowEntity.class.getDeclaredMethod("doPostHurtEffects",net.minecraft.world.entity.LivingEntity.class);hit.setAccessible(true);hit.invoke(arrow,target);
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        h.assertTrue(!target.isInWater()&&target.hasEffect(ModEffects.REEF_BLEEDING.get()),"Dry arrow victim did not bleed");
        h.runAfterDelay(85,()->{h.assertTrue(target.getHealth()==98,"Base arrow bleeding is not two damage");h.succeed();});
    }
    @GameTest(template="tidalterror:reef_life_pool",timeoutTicks=60)
    public static void armorResistanceAppliesOnLand(GameTestHelper h) {
        var target=victim(h);
        for(var pair:java.util.Map.of(net.minecraft.world.entity.EquipmentSlot.HEAD,ModEquipment.REEF_HELMET.get(),net.minecraft.world.entity.EquipmentSlot.CHEST,ModEquipment.REEF_CHESTPLATE.get(),net.minecraft.world.entity.EquipmentSlot.LEGS,ModEquipment.REEF_LEGGINGS.get(),net.minecraft.world.entity.EquipmentSlot.FEET,ModEquipment.REEF_BOOTS.get()).entrySet())target.setItemSlot(pair.getKey(),new ItemStack(pair.getValue()));
        ModEffects.applyBleeding(target,null,80,0);
        h.runAfterDelay(45,()->{h.assertTrue(target.getHealth()==99.25F,"Full armor did not reduce bleed by 25 percent on land");h.succeed();});
    }
    @GameTest(template="tidalterror:reef_life_pool",timeoutTicks=20)
    public static void equipmentRepairsAndRestrictions(GameTestHelper h) {
        for(var armor:java.util.List.of(ModEquipment.REEF_HELMET.get(),ModEquipment.REEF_CHESTPLATE.get(),ModEquipment.REEF_LEGGINGS.get(),ModEquipment.REEF_BOOTS.get()))
            h.assertTrue(new ItemStack(armor).is(net.minecraft.tags.ItemTags.TRIMMABLE_ARMOR),"Reef armor missing native trimmable tag");
        var registry=h.getLevel().registryAccess();
        var trim=new net.minecraft.world.item.armortrim.ArmorTrim(
            registry.registryOrThrow(net.minecraft.core.registries.Registries.TRIM_MATERIAL).getHolderOrThrow(net.minecraft.world.item.armortrim.TrimMaterials.GOLD),
            registry.registryOrThrow(net.minecraft.core.registries.Registries.TRIM_PATTERN).getHolderOrThrow(net.minecraft.world.item.armortrim.TrimPatterns.SENTRY));
        for(var armor:java.util.List.of(ModEquipment.REEF_HELMET.get(),ModEquipment.REEF_CHESTPLATE.get(),ModEquipment.REEF_LEGGINGS.get(),ModEquipment.REEF_BOOTS.get())) {
            var trimmed=new ItemStack(armor);
            h.assertTrue(net.minecraft.world.item.armortrim.ArmorTrim.setTrim(registry,trimmed,trim),"Native trim setter rejected Reef Armor");
            h.assertTrue(net.minecraft.world.item.armortrim.ArmorTrim.getTrim(registry,trimmed).orElseThrow().equals(trim),"Native trim getter lost Reef Armor trim");
        }
        var spear=new ItemStack(ModEquipment.REEF_SPEAR.get());
        h.assertTrue(spear.getItem().isValidRepairItem(spear,new ItemStack(ModEquipment.CRUSHER_TOOTH.get())),"Spear does not repair with teeth");
        h.assertTrue(!com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get().canEnchant(new ItemStack(net.minecraft.world.item.Items.BOW)),"Serration applies to bows");
        h.assertTrue(!com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE.get().canEnchant(new ItemStack(net.minecraft.world.item.Items.CROSSBOW)),"Hemorrhage applies to crossbows");
        for(var item:java.util.List.of(ModEquipment.REEF_HELMET.get(),ModEquipment.REEF_CHESTPLATE.get(),ModEquipment.REEF_LEGGINGS.get(),ModEquipment.REEF_BOOTS.get()))
            h.assertTrue(item.isValidRepairItem(new ItemStack(item),new ItemStack(ModEquipment.SHARDBACK_PLATE.get())),"Armor does not repair with plates");
        for(var enchantment:java.util.List.of(net.minecraft.world.item.enchantment.Enchantments.MOB_LOOTING,net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK,net.minecraft.world.item.enchantment.Enchantments.SHARPNESS))
            h.assertTrue(enchantment.canEnchant(spear),"Native melee enchantment rejected by spear");
        var candidates=net.minecraft.world.item.enchantment.EnchantmentHelper.getAvailableEnchantmentResults(30,spear,false);
        h.assertTrue(candidates.stream().anyMatch(entry->entry.enchantment==net.minecraft.world.item.enchantment.Enchantments.MOB_LOOTING),"Looting absent from spear enchanting table");
        h.assertTrue(candidates.stream().noneMatch(entry->entry.enchantment==net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT||entry.enchantment==net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE),"Invalid spear table enchantment");
        var bowCandidates=net.minecraft.world.item.enchantment.EnchantmentHelper.getAvailableEnchantmentResults(30,new ItemStack(net.minecraft.world.item.Items.BOW),false);
        h.assertTrue(bowCandidates.stream().noneMatch(entry->entry.enchantment==com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get()||entry.enchantment==com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE.get()),"Bleeding offered on ranged weapon table");
        h.succeed();
    }

    // Exercise the native activation/serialization paths; testing a hand-written
    // boolean guard alone would miss a mixin attached to the wrong constructor.
    private static java.util.Set<net.minecraft.world.level.ChunkPos> repairs(GameTestHelper h) {
        try {
            var field=com.nhat.tidal_terror.worldgen.ReefWaterFinish.class.getDeclaredField("PENDING");field.setAccessible(true);
            @SuppressWarnings("unchecked") var queues=(java.util.Map<net.minecraft.server.level.ServerLevel,java.util.Set<net.minecraft.world.level.ChunkPos>>)field.get(null);
            return queues.computeIfAbsent(h.getLevel(),level->new java.util.HashSet<>());
        } catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
    }
    private static net.minecraft.world.level.chunk.LevelChunk reloadChunk(GameTestHelper h,net.minecraft.world.level.chunk.LevelChunk chunk) {
        var level=h.getLevel();var data=net.minecraft.world.level.chunk.storage.ChunkSerializer.write(level,chunk);
        return ((net.minecraft.world.level.chunk.ImposterProtoChunk)net.minecraft.world.level.chunk.storage.ChunkSerializer.read(level,level.getPoiManager(),chunk.getPos(),data)).getWrapped();
    }
    @GameTest(template="tidalterror:reef_life_pool",timeoutTicks=20)
    public static void completedChunkRevisitPreservesPlayerBlocks(GameTestHelper h) {
        var level=h.getLevel();var pos=new net.minecraft.world.level.ChunkPos(-2000,-2000);
        var chunk=new net.minecraft.world.level.chunk.LevelChunk(level,pos);
        var blocks=java.util.List.of(net.minecraft.world.level.block.Blocks.AIR,net.minecraft.world.level.block.Blocks.MAGMA_BLOCK,net.minecraft.world.level.block.Blocks.SOUL_SAND,net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN);
        for(int i=0;i<blocks.size();i++)chunk.setBlockState(new net.minecraft.core.BlockPos(pos.getMinBlockX()+i,20,pos.getMinBlockZ()),blocks.get(i).defaultBlockState(),false);
        h.assertTrue(!repairs(h).contains(pos),"Existing full chunk was treated as new generation");
        chunk=reloadChunk(h,chunk);
        chunk.postProcessGeneration();
        chunk.postProcessGeneration();
        h.assertTrue(!repairs(h).contains(pos),"Revisiting completed terrain queued destructive water repair");
        for(int i=0;i<blocks.size();i++)h.assertTrue(chunk.getBlockState(new net.minecraft.core.BlockPos(pos.getMinBlockX()+i,20,pos.getMinBlockZ())).is(blocks.get(i)),"Player construction changed on reload/revisit");
        h.succeed();
    }
    @GameTest(template="tidalterror:reef_life_pool",timeoutTicks=20)
    public static void unfinishedGenerationRepairSurvivesReload(GameTestHelper h) {
        var level=h.getLevel();var pos=new net.minecraft.world.level.ChunkPos(-2001,-2001);
        var proto=new net.minecraft.world.level.chunk.ProtoChunk(pos,net.minecraft.world.level.chunk.UpgradeData.EMPTY,level,level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME),null);
        var chunk=new net.minecraft.world.level.chunk.LevelChunk(level,proto,null);
        h.assertTrue(repairs(h).contains(pos),"Newly generated terrain did not queue water repair");
        var data=net.minecraft.world.level.chunk.storage.ChunkSerializer.write(level,chunk);
        repairs(h).remove(pos); // Simulate loss of the in-memory queue on restart.
        var restored=net.minecraft.world.level.chunk.storage.ChunkSerializer.read(level,level.getPoiManager(),pos,data);

        h.assertTrue(repairs(h).contains(pos),"Saved unfinished repair did not resume");
        repairs(h).remove(pos);
        var wrapped=new net.minecraft.world.level.chunk.ImposterProtoChunk(chunk,false);
        new net.minecraft.world.level.chunk.LevelChunk(level,wrapped,null);
        h.assertTrue(!repairs(h).contains(pos),"A wrapper around existing full terrain was treated as generation");
        h.succeed();
    }
}
