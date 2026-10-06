package com.nhat.tidal_terror.gametest;
import com.nhat.tidal_terror.effects.ModEffects;
import com.nhat.tidal_terror.items.ModEquipment;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public final class PortEquipmentTests {
    private static net.minecraft.nbt.CompoundTag save(net.minecraft.world.entity.Entity entity,net.minecraft.server.level.ServerLevel level){
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());entity.saveWithoutId(output);return output.buildResult();
    }
    private static net.minecraft.world.entity.animal.pig.Pig victim(GameTestHelper h) {
        var target=h.spawn(EntityType.PIG,10,4,10);target.setNoAi(true);target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);return target;
    }
    public static void refreshRetainsClockAndUpdatedDamage(GameTestHelper h) {
        var target=victim(h);ModEffects.applyBleeding(target,null,80,0);
        h.runAfterDelay(18,()->ModEffects.applyBleeding(target,null,80,2));
        h.runAfterDelay(39,()->h.assertTrue(target.getHealth()==100,"Early bleed"));
        h.runAfterDelay(41,()->h.assertTrue(target.getHealth()==98,"Refresh reset the clock or ignored power"));
        h.runAfterDelay(81,()->h.assertTrue(target.getHealth()==96,"Second scheduled hit missing"));
        h.runAfterDelay(105,()->{
            h.assertTrue(!target.hasEffect(ModEffects.bleeding()),"Refreshed effect did not expire");
            h.assertTrue(com.nhat.tidal_terror.platform.ReefEntityData.get(target).isEmpty(),"Expired bleed retained clock/owner");h.succeed();
        });
    }
    public static void independentCountdownSurvivesReload(GameTestHelper h) {
        var target=victim(h);ModEffects.applyBleeding(target,null,80,0);
        net.minecraft.world.entity.animal.pig.Pig[] restored={null};
        h.runAfterDelay(15,()->{
            var saved=save(target,h.getLevel());
            int before=com.nhat.tidal_terror.platform.ReefEntityData.get(target).getIntOr("tidalterror:bleeding_pulse_ticks",0);
            target.discard();restored[0]=EntityType.PIG.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.LOAD);restored[0].load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));h.getLevel().addFreshEntity(restored[0]);
            h.assertTrue(com.nhat.tidal_terror.platform.ReefEntityData.get(restored[0]).getIntOr("tidalterror:bleeding_pulse_ticks",0)==before,"Reload reset independent clock");
        });
        h.runAfterDelay(45,()->{h.assertTrue(restored[0].getHealth()==99,"Reload postponed first hit");h.succeed();});
    }
    public static void fangArrowBleedsOnLand(GameTestHelper h) {
        var target=victim(h);var arrow=new com.nhat.tidal_terror.entities.FangArrowEntity(com.nhat.tidal_terror.entities.ModEntities.FANG_ARROW.get(),h.getLevel());
        try {
            var hit=com.nhat.tidal_terror.entities.FangArrowEntity.class.getDeclaredMethod("doPostHurtEffects",net.minecraft.world.entity.LivingEntity.class);hit.setAccessible(true);hit.invoke(arrow,target);
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        h.assertTrue(!target.isInWater()&&target.hasEffect(ModEffects.bleeding()),"Dry arrow victim did not bleed");
        h.runAfterDelay(85,()->{h.assertTrue(target.getHealth()==98,"Base arrow bleeding is not two damage");h.succeed();});
    }
    public static void armorResistanceAppliesOnLand(GameTestHelper h) {
        var target=victim(h);
        for(var pair:java.util.Map.of(net.minecraft.world.entity.EquipmentSlot.HEAD,ModEquipment.REEF_HELMET.get(),net.minecraft.world.entity.EquipmentSlot.CHEST,ModEquipment.REEF_CHESTPLATE.get(),net.minecraft.world.entity.EquipmentSlot.LEGS,ModEquipment.REEF_LEGGINGS.get(),net.minecraft.world.entity.EquipmentSlot.FEET,ModEquipment.REEF_BOOTS.get()).entrySet())target.setItemSlot(pair.getKey(),new ItemStack(pair.getValue()));
        ModEffects.applyBleeding(target,null,80,0);
        h.runAfterDelay(45,()->{h.assertTrue(target.getHealth()==99.25F,"Full armor did not reduce bleed by 25 percent on land");h.succeed();});
    }
    public static void equipmentRepairsAndRestrictions(GameTestHelper h) {
        var spear=new ItemStack(ModEquipment.REEF_SPEAR.get());
        h.assertTrue(spear.isValidRepairItem(new ItemStack(ModEquipment.CRUSHER_TOOTH.get())),"Spear does not repair with teeth");
        h.assertTrue(!h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION).value().canEnchant(new ItemStack(net.minecraft.world.item.Items.BOW)),"Serration applies to bows");
        h.assertTrue(!h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE).value().canEnchant(new ItemStack(net.minecraft.world.item.Items.CROSSBOW)),"Hemorrhage applies to crossbows");
        for(var item:java.util.List.of(ModEquipment.REEF_HELMET.get(),ModEquipment.REEF_CHESTPLATE.get(),ModEquipment.REEF_LEGGINGS.get(),ModEquipment.REEF_BOOTS.get()))
            h.assertTrue(new ItemStack(item).isValidRepairItem(new ItemStack(ModEquipment.SHARDBACK_PLATE.get())),"Armor does not repair with plates");
        var enchantments=h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        for(var key:java.util.List.of(net.minecraft.world.item.enchantment.Enchantments.LOOTING,net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK,net.minecraft.world.item.enchantment.Enchantments.SHARPNESS))
            h.assertTrue(enchantments.getOrThrow(key).value().canEnchant(spear),"Native melee enchantment rejected by spear: "+key);
        for(var key:java.util.List.of(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT,net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE))
            h.assertTrue(!enchantments.getOrThrow(key).value().canEnchant(spear),"Incompatible enchantment accepted: "+key);
        h.assertTrue(!net.minecraft.world.item.enchantment.Enchantment.areCompatible(enchantments.getOrThrow(com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION),enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS)),"Bleeding stacks with Sharpness");
        try (var stream=PortEquipmentTests.class.getResourceAsStream("/data/tidalterror/recipe/reef_helmet.json")) {
            var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8));
            var recipe=com.nhat.tidal_terror.recipes.ModRecipes.ARMOR_UPGRADE.get().codec().codec().parse(h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),json).getOrThrow();
            var iron=new ItemStack(net.minecraft.world.item.Items.IRON_HELMET);
            iron.setDamageValue(37);iron.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Inherited helmet"));iron.set(net.minecraft.core.component.DataComponents.REPAIR_COST,9);
            iron.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION),3);
            var grid=new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(9,ItemStack.EMPTY));grid.set(4,iron);
            for(int index:new int[]{1,3,5})grid.set(index,new ItemStack(ModEquipment.SHARDBACK_PLATE.get()));
            grid.set(7,new ItemStack(net.minecraft.world.item.Items.DEAD_FIRE_CORAL_BLOCK));
            var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid);
            h.assertTrue(recipe.matches(input,h.getLevel()),"Loaded armor upgrade recipe does not match");
            var upgraded=recipe.assemble(input);
            h.assertTrue(upgraded.is(ModEquipment.REEF_HELMET.get())&&upgraded.getDamageValue()==37,"Upgrade erased wear or returned wrong armor");
            h.assertTrue(upgraded.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).equals(iron.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)),"Upgrade erased name");
            h.assertTrue(upgraded.get(net.minecraft.core.component.DataComponents.REPAIR_COST)==9,"Upgrade erased repair cost");
            h.assertTrue(upgraded.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS).equals(iron.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS)),"Upgrade erased enchantments");
        } catch (java.io.IOException failure) { throw new RuntimeException(failure); }
        var attacker=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"spear-parity"),net.minecraft.server.level.ClientInformation.createDefault());
        attacker.getAbilities().instabuild=false;
        var struck=victim(h);
        h.assertTrue(spear.hurtEnemy(struck,attacker),"Native spear hit did not count as weapon use");
        spear.postHurtEnemy(struck,attacker);
        h.assertTrue(spear.getDamageValue()==1,"Native hit durability was lost or charged twice: "+spear.getDamageValue());
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
        var level=h.getLevel();var data=net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(level,level.palettedContainerFactory(),net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(level,chunk).write());
        var restored=data.read(level,level.getPoiManager(),new net.minecraft.world.level.chunk.storage.RegionStorageInfo("port-test",level.dimension(),"chunk"),chunk.getPos());
        var full=((net.minecraft.world.level.chunk.ImposterProtoChunk)restored).getWrapped();
        com.nhat.tidal_terror.worldgen.ReefWaterFinish.restore(full);return full;
    }

    public static void completedChunkRevisitPreservesPlayerBlocks(GameTestHelper h) {
        var level=h.getLevel();var pos=new net.minecraft.world.level.ChunkPos(-2000,-2000);
        var chunk=new net.minecraft.world.level.chunk.LevelChunk(level,pos);
        var blocks=java.util.List.of(net.minecraft.world.level.block.Blocks.AIR,net.minecraft.world.level.block.Blocks.MAGMA_BLOCK,net.minecraft.world.level.block.Blocks.SOUL_SAND,net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN);
        for(int i=0;i<blocks.size();i++)chunk.setBlockState(new net.minecraft.core.BlockPos(pos.getMinBlockX()+i,20,pos.getMinBlockZ()),blocks.get(i).defaultBlockState(),2);
        h.assertTrue(!repairs(h).contains(pos),"Existing full chunk was treated as new generation");
        chunk=reloadChunk(h,chunk);
        chunk.postProcessGeneration(level);
        chunk.postProcessGeneration(level);
        h.assertTrue(!repairs(h).contains(pos),"Revisiting completed terrain queued destructive water repair");
        for(int i=0;i<blocks.size();i++)h.assertTrue(chunk.getBlockState(new net.minecraft.core.BlockPos(pos.getMinBlockX()+i,20,pos.getMinBlockZ())).is(blocks.get(i)),"Player construction changed on reload/revisit");
        h.succeed();
    }

    public static void unfinishedGenerationRepairSurvivesReload(GameTestHelper h) {
        var level=h.getLevel();var pos=new net.minecraft.world.level.ChunkPos(-2001,-2001);
        var proto=new net.minecraft.world.level.chunk.ProtoChunk(pos,net.minecraft.world.level.chunk.UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
        var chunk=new net.minecraft.world.level.chunk.LevelChunk(level,proto,null);
        h.assertTrue(repairs(h).contains(pos),"Newly generated terrain did not queue water repair");
        var data=net.minecraft.world.level.chunk.storage.SerializableChunkData.parse(level,level.palettedContainerFactory(),net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(level,chunk).write());
        repairs(h).remove(pos); // Simulate loss of the in-memory queue on restart.
        var restored=data.read(level,level.getPoiManager(),new net.minecraft.world.level.chunk.storage.RegionStorageInfo("port-test",level.dimension(),"chunk"),pos);
        com.nhat.tidal_terror.worldgen.ReefWaterFinish.restore(((net.minecraft.world.level.chunk.ImposterProtoChunk)restored).getWrapped());
        h.assertTrue(repairs(h).contains(pos),"Saved unfinished repair did not resume");
        repairs(h).remove(pos);
        var wrapped=new net.minecraft.world.level.chunk.ImposterProtoChunk(chunk,false);
        new net.minecraft.world.level.chunk.LevelChunk(level,wrapped,null);
        h.assertTrue(!repairs(h).contains(pos),"A wrapper around existing full terrain was treated as generation");
        h.succeed();
    }
}
