package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.effects.ModEffects;
import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.shardback.ShardbackEntity;
import com.nhat.tidal_terror.items.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.gametest.*;

@GameTestHolder("tidalterror") @PrefixGameTestTemplate(false)
public final class ReefEquipmentTests {
    private static void pool(GameTestHelper h) {
        for(int x=0;x<=23;x++)for(int z=0;z<=23;z++)for(int y=3;y<=8;y++)
            h.setBlock(x,y,z,y==3?Blocks.SANDSTONE:Blocks.WATER);
    }
    private static Player player(GameTestHelper h, boolean water) {
        Player p=h.makeMockSurvivalPlayer();
        var pos=h.absolutePos(new BlockPos(8,water?4:10,8));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        p.setNoGravity(true);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModEquipment.REEF_SPEAR.get()));
        charge(p);
        return p;
    }
    private static void charge(Player p) { for(int i=0;i<25;i++){p.setDeltaMovement(Vec3.ZERO);p.tick();} }
    private static LivingEntity target(GameTestHelper h) {
        var mob=h.spawn(EntityType.DROWNED,10,4,8);mob.setNoAi(true);mob.setNoGravity(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);mob.getAttribute(Attributes.ARMOR).setBaseValue(0);mob.setHealth(100);
        mob.tick();return mob;
    }
    private static void near(GameTestHelper h,double a,double b,String message) { h.assertTrue(Math.abs(a-b)<.001,message+": "+a+" != "+b); }

    @GameTest(template="reef_life_pool",timeoutTicks=60)
    public static void spearUsesRealAttacksCooldownAndMainhandReach(GameTestHelper h) {
        pool(h);var p=player(h,true);var t=target(h);
        near(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),6,"Spear damage");
        near(h,p.getAttributeValue(Attributes.ATTACK_SPEED),1.1,"Spear speed");
        near(h,p.getAttributeValue(ForgeMod.ENTITY_REACH.get()),4,"Spear reach");
        h.assertTrue(p.isInWater()&&t.isInWater(),"Native water flags missing");
        p.attack(t);near(h,t.getHealth(),94,"Charged hit damage");
        h.assertTrue(t.hasEffect(ModEffects.REEF_BLEEDING.get()),"Charged native attack did not bleed");
        h.assertTrue(p.getMainHandItem().getDamageValue()==1,"Native attack did not damage spear");
        t.removeAllEffects();t.invulnerableTime=0;p.attack(t);
        h.assertTrue(!t.hasEffect(ModEffects.REEF_BLEEDING.get()),"Uncharged hit applied bleeding");
        charge(p);t.setInvulnerable(true);p.attack(t);t.setInvulnerable(false);
        h.assertTrue(!t.hasEffect(ModEffects.REEF_BLEEDING.get()),"Rejected attack applied bleeding");
        var dry=player(h,false);t.invulnerableTime=0;dry.attack(t);
        h.assertTrue(!t.hasEffect(ModEffects.REEF_BLEEDING.get()),"Dry attacker applied bleeding");
        charge(p);var dryPos=h.absolutePos(new BlockPos(10,10,8));t.setPos(dryPos.getX(),dryPos.getY(),dryPos.getZ());t.tick();t.invulnerableTime=0;p.attack(t);
        h.assertTrue(!t.hasEffect(ModEffects.REEF_BLEEDING.get()),"Dry target applied bleeding");
        var waterPos=h.absolutePos(new BlockPos(12,4,8));t.setPos(waterPos.getX()+.3,waterPos.getY(),waterPos.getZ()+.5);
        h.assertTrue(p.canReach(t,0),"Forge server reach rejected extended spear attack");
        p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ModEquipment.REEF_SPEAR.get()));p.tick();
        near(h,p.getAttributeValue(ForgeMod.ENTITY_REACH.get()),3,"Offhand spear leaked reach");
        h.assertTrue(!p.canReach(t,0),"Unarmed server reach accepted spear distance");
        h.succeed();
    }

    @GameTest(template="reef_life_pool",timeoutTicks=110)
    public static void bleedingRefreshesWithoutStackingAndDealsTwoDamage(GameTestHelper h) {
        pool(h);var p=player(h,true);var t=target(h);
        p.attack(t);charge(p);t.invulnerableTime=0;p.attack(t);
        var effect=t.getEffect(ModEffects.REEF_BLEEDING.get());
        h.assertTrue(effect!=null&&effect.getDuration()==80&&effect.getAmplifier()==0,"Bleed did not refresh at fixed strength");
        float health=t.getHealth();
        h.runAfterDelay(20,()->near(h,t.getHealth(),health,"Bleed fired too early"));
        h.runAfterDelay(45,()->near(h,t.getHealth(),health-1,"First bleed pulse"));
        h.runAfterDelay(85,()->{
            near(h,t.getHealth(),health-2,"Non-stacking total bleed damage");
            h.assertTrue(!t.hasEffect(ModEffects.REEF_BLEEDING.get()),"Bleed failed to expire");h.succeed();
        });
    }

    @GameTest(template="reef_life_pool",timeoutTicks=40)
    public static void recipesRequireIronAndCoralAndEquipmentRepairs(GameTestHelper h) {
        Player player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        var coralDrops=net.minecraft.world.level.block.Block.getDrops(Blocks.DEAD_TUBE_CORAL_BLOCK.defaultBlockState(),
                h.getLevel(),h.absolutePos(new BlockPos(8,10,8)),null,player,new ItemStack(Items.IRON_PICKAXE));
        h.assertTrue(coralDrops.stream().anyMatch(s->s.is(Items.DEAD_TUBE_CORAL_BLOCK)),"Coral ingredient unexpectedly requires Silk Touch");
        for(var entry:Map.of("reef_spear",ModEquipment.REEF_SPEAR.get(),"reef_helmet",ModEquipment.REEF_HELMET.get(),
                "reef_chestplate",ModEquipment.REEF_CHESTPLATE.get(),"reef_leggings",ModEquipment.REEF_LEGGINGS.get(),"reef_boots",ModEquipment.REEF_BOOTS.get()).entrySet()) {
            var recipe=(ShapedRecipe)h.getLevel().getRecipeManager().byKey(new ResourceLocation("tidalterror",entry.getKey())).orElseThrow();
            var grid=new TransientCraftingContainer(player.inventoryMenu,3,3);int iron=-1,coral=-1;
            for(int y=0;y<recipe.getHeight();y++)for(int x=0;x<recipe.getWidth();x++) {
                var ingredient=recipe.getIngredients().get(y*recipe.getWidth()+x);
                if(ingredient.isEmpty())continue;
                ItemStack sample=ingredient.getItems()[0].copy();int index=y*3+x;grid.setItem(index,sample);
                if(sample.is(Items.IRON_INGOT))iron=index;
                if(ingredient.test(new ItemStack(Items.DEAD_TUBE_CORAL_BLOCK)))coral=index;
            }
            h.assertTrue(iron>=0&&coral>=0,"Missing iron/coral gate in "+entry.getKey());
            h.assertTrue(recipe.matches(grid,h.getLevel())&&recipe.assemble(grid,h.getLevel().registryAccess()).is(entry.getValue()),"Native crafting failed");
            var saved=grid.getItem(iron);grid.setItem(iron,ItemStack.EMPTY);h.assertTrue(!recipe.matches(grid,h.getLevel()),"Recipe crafted without iron");grid.setItem(iron,saved);
            grid.setItem(coral,new ItemStack(Items.STONE));h.assertTrue(!recipe.matches(grid,h.getLevel()),"Recipe accepted stone as coral");
            h.assertTrue(entry.getValue().isValidRepairItem(new ItemStack(entry.getValue()),new ItemStack(entry.getKey().equals("reef_spear")?ModEquipment.CRUSHER_TOOTH.get():ModEquipment.SHARDBACK_PLATE.get())),"Wrong repair material");
        }
        var spear=ModEquipment.REEF_SPEAR.get();
        h.assertTrue(spear.canApplyAtEnchantingTable(new ItemStack(spear),Enchantments.SHARPNESS)&&spear.canApplyAtEnchantingTable(new ItemStack(spear),Enchantments.MOB_LOOTING),"Spear rejected weapon enchantments");
        h.assertTrue(!spear.canApplyAtEnchantingTable(new ItemStack(spear),Enchantments.SWEEPING_EDGE),"Spear accepted sweeping");
        h.succeed();
    }

    @GameTest(template="reef_life_pool",timeoutTicks=40)
    public static void armorStatsAndConditionalMixedPieceKnockback(GameTestHelper h) {
        pool(h);var wearer=target(h);wearer.setOnGround(true);
        var armor=List.of(ModEquipment.REEF_HELMET.get(),ModEquipment.REEF_CHESTPLATE.get(),ModEquipment.REEF_LEGGINGS.get(),ModEquipment.REEF_BOOTS.get());
        for(var piece:armor)wearer.setItemSlot(piece.getEquipmentSlot(),new ItemStack(piece));
        wearer.tick();wearer.setOnGround(true);
        near(h,wearer.getArmorValue(),16,"Armor set defense");near(h,wearer.getAttributeValue(Attributes.ARMOR_TOUGHNESS),0,"Armor toughness");
        for(var piece:armor)h.assertTrue(piece.getMaxDamage()==ArmorMaterials.IRON.getDurabilityForType(piece.getType()),"Armor durability tier");
        // Actual native knockback posts Forge's event and changes velocity.
        wearer.setDeltaMovement(Vec3.ZERO);wearer.knockback(1,1,0);near(h,Math.abs(wearer.getDeltaMovement().x),.8,"Full set anchor");
        wearer.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);wearer.setDeltaMovement(Vec3.ZERO);wearer.knockback(1,1,0);near(h,Math.abs(wearer.getDeltaMovement().x),.85,"Three-piece anchor");
        wearer.setOnGround(false);wearer.setDeltaMovement(Vec3.ZERO);wearer.knockback(1,1,0);near(h,Math.abs(wearer.getDeltaMovement().x),1,"Swimming anchor leaked");
        var pos=h.absolutePos(new BlockPos(10,10,8));wearer.setPos(pos.getX(),pos.getY(),pos.getZ());wearer.tick();wearer.setOnGround(true);
        wearer.setDeltaMovement(Vec3.ZERO);wearer.knockback(1,1,0);near(h,Math.abs(wearer.getDeltaMovement().x),1,"Dry anchor leaked");h.succeed();
    }

    @GameTest(template="reef_life_pool",timeoutTicks=40)
    public static void actualDeathsDropFoodAndCraftingMaterials(GameTestHelper h) {
        for(boolean fire:new boolean[]{false,true})for(boolean shark:new boolean[]{false,true}) {
            Mob mob=(shark?ModEntities.CORAL_CRUSHER.get():ModEntities.SHARDBACK.get()).create(h.getLevel());
            var pos=h.absolutePos(new BlockPos(8,10,8));mob.setPos(pos.getX(),pos.getY(),pos.getZ());mob.setNoAi(true);h.getLevel().addFreshEntity(mob);
            if(fire)mob.setSecondsOnFire(30);mob.hurt(h.getLevel().damageSources().generic(),1000);
            int material=0,food=0;
            for(var drop:h.getLevel().getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(3))) {
                var stack=drop.getItem();
                if(stack.is(shark?ModEquipment.CRUSHER_TOOTH.get():ModEquipment.SHARDBACK_PLATE.get()))material+=stack.getCount();
                else if(stack.is(shark?(fire?ModFoods.COOKED_CORAL_CRUSHER_STEAK.get():ModFoods.RAW_CORAL_CRUSHER_STEAK.get()):(fire?ModFoods.COOKED_SHARDBACK_CLAW.get():ModFoods.RAW_SHARDBACK_CLAW.get())))food+=stack.getCount();
                else h.assertTrue(false,"Unexpected material/food drop "+stack);
                drop.discard();
            }
            h.assertTrue(material>=1&&material<=2&&food>0,"Death lost material or seafood");mob.discard();
        }
        h.succeed();
    }

    @GameTest(template="reef_life_pool",timeoutTicks=200)
    public static void peacefulMoltingPersistsCooldownAndRespectsMobLoot(GameTestHelper h) {
        pool(h);var crab=h.spawn(ModEntities.SHARDBACK.get(),10,4,10);
        var tag=new CompoundTag();crab.addAdditionalSaveData(tag);tag.putInt("ReefMoltCooldown",1);crab.readAdditionalSaveData(tag);
        h.succeedWhen(()->{
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,crab.getBoundingBox().inflate(8),e->e.getItem().is(ModEquipment.SHARDBACK_PLATE.get()));
            h.assertTrue(drops.size()==1,"Waiting for peaceful forage molt");
            h.assertTrue(crab.isAlive()&&crab.getHealth()==16,"Molting harmed the crab");
            var saved=new CompoundTag();crab.addAdditionalSaveData(saved);
            h.assertTrue(saved.getInt("ReefMoltCooldown")>5800,"Molt cooldown not reset");
            var copy=ModEntities.SHARDBACK.get().create(h.getLevel());copy.readAdditionalSaveData(saved);
            var restored=new CompoundTag();copy.addAdditionalSaveData(restored);
            h.assertTrue(restored.getInt("ReefMoltCooldown")==saved.getInt("ReefMoltCooldown"),"Reload reset molt cooldown");
            for(var drop:drops)drop.discard();
            var rules=h.getLevel().getGameRules();var rule=rules.getRule(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT);boolean before=rule.get();
            rule.set(false,h.getLevel().getServer());
            try {
                saved.putInt("ReefMoltCooldown",0);crab.readAdditionalSaveData(saved);crab.tick();
                h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,crab.getBoundingBox().inflate(8),e->e.getItem().is(ModEquipment.SHARDBACK_PLATE.get())).isEmpty(),"Molting ignored doMobLoot");
            } finally {rule.set(before,h.getLevel().getServer());}
        });
    }
}
