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
    @GameTest(template="reef_life_pool",timeoutTicks=230)
    public static void repeatedSpearHitsRefreshDurationWithoutDelayingPulses(GameTestHelper h) {
        pool(h);var p=player(h,true);var t=target(h);var position=t.position();
        float start=t.getHealth();double[] direct={0};
        Runnable attack=()->{charge(p);t.invulnerableTime=0;float before=t.getHealth();p.attack(t);direct[0]+=before-t.getHealth();};
        attack.run();
        // Isolate pulse scheduling from native damage immunity after each weapon hit.
        for(int tick=1;tick<=220;tick++)h.runAfterDelay(tick,()->{t.setPos(position);t.setDeltaMovement(Vec3.ZERO);t.invulnerableTime=0;});
        for(int tick:new int[]{18,36,54})h.runAfterDelay(tick,()->{
            // Increasing power/duration must preserve the next scheduled pulse too.
            p.getMainHandItem().enchant(com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get(),3);
            p.getMainHandItem().enchant(com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE.get(),2);
            attack.run();
            h.assertTrue(t.getEffect(ModEffects.REEF_BLEEDING.get()).getDuration()==160,"Hit did not refresh duration");
        });
        h.runAfterDelay(30,()->near(h,t.getHealth(),start-direct[0],"Refresh caused an early/duplicate pulse"));
        h.runAfterDelay(45,()->near(h,t.getHealth(),start-direct[0]-2.5,"Refresh postponed first pulse"));
        h.runAfterDelay(85,()->near(h,t.getHealth(),start-direct[0]-5,"Refresh postponed second pulse"));
        h.runAfterDelay(165,()->near(h,t.getHealth(),start-direct[0]-10,"Refreshed bleed lost cadence"));
        h.runAfterDelay(220,()->{
            near(h,t.getHealth(),start-direct[0]-12.5,"Extended bleed total");
            h.assertTrue(!t.hasEffect(ModEffects.REEF_BLEEDING.get()),"Refreshed bleed did not expire");h.succeed();
        });
    }

    @GameTest(template="reef_life_pool",timeoutTicks=140)
    public static void bleedingPulseClockSurvivesSaveAndResetsAfterCure(GameTestHelper h) {
        pool(h);var t=target(h);LivingEntity[] active={t};var position=t.position();
        t.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.REEF_BLEEDING.get(),80,0,false,false,true));
        for(int tick=1;tick<=135;tick++)h.runAfterDelay(tick,()->{active[0].setPos(position);active[0].setDeltaMovement(Vec3.ZERO);});
        h.runAfterDelay(25,()->{
            var saved=new CompoundTag();t.saveWithoutId(saved);t.discard();
            var loaded=EntityType.DROWNED.create(h.getLevel());loaded.load(saved);h.getLevel().addFreshEntity(loaded);active[0]=loaded;
        });
        h.runAfterDelay(45,()->near(h,active[0].getHealth(),99,"Save/load reset the pulse cooldown"));
        h.runAfterDelay(46,()->{
            h.assertTrue(active[0].curePotionEffects(new ItemStack(Items.MILK_BUCKET)),"Milk did not cure bleeding");
            active[0].addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.REEF_BLEEDING.get(),80,0,false,false,true));
        });
        h.runAfterDelay(80,()->near(h,active[0].getHealth(),99,"New bleed inherited the cured pulse clock"));
        h.runAfterDelay(92,()->near(h,active[0].getHealth(),98,"New bleed failed its first pulse"));
        h.runAfterDelay(135,()->{
            near(h,active[0].getHealth(),97,"New bleed total after cure");
            h.assertTrue(!active[0].hasEffect(ModEffects.REEF_BLEEDING.get()),"New bleed did not expire");h.succeed();
        });
    }

    @GameTest(template="reef_life_pool",timeoutTicks=100)
    public static void fangArrowBleedsOnLandRejectsBlockedHitsAndPersistsPickup(GameTestHelper h) throws Exception {
        var p=h.makeMockSurvivalPlayer(); var t=h.spawn(EntityType.COW,10,10,8);t.setNoAi(true);t.setNoGravity(true);
        t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);t.setHealth(100);
        var arrow=ModEquipment.FANG_ARROW.get().createArrow(h.getLevel(),new ItemStack(ModEquipment.FANG_ARROW.get()),p);
        h.assertTrue(!t.isInWater(),"Arrow fixture must test dry land"); arrow.setDeltaMovement(1,0,0);
        var hit=net.minecraft.world.entity.projectile.AbstractArrow.class.getDeclaredMethod("onHitEntity",net.minecraft.world.phys.EntityHitResult.class);hit.setAccessible(true);
        hit.invoke(arrow,new net.minecraft.world.phys.EntityHitResult(t));
        var bleed=t.getEffect(ModEffects.REEF_BLEEDING.get());
        h.assertTrue(bleed!=null&&bleed.getDuration()==80&&bleed.getAmplifier()==0&&!bleed.isVisible(),"Land hit lost base blood effect");
        float health=t.getHealth();
        var blocked=h.spawn(EntityType.COW,12,10,8);blocked.setNoAi(true);blocked.setInvulnerable(true);
        var rejected=ModEquipment.FANG_ARROW.get().createArrow(h.getLevel(),new ItemStack(ModEquipment.FANG_ARROW.get()),p);rejected.setDeltaMovement(1,0,0);
        hit.invoke(rejected,new net.minecraft.world.phys.EntityHitResult(blocked));
        h.assertTrue(!blocked.hasEffect(ModEffects.REEF_BLEEDING.get()),"Rejected arrow hit caused bleeding");
        var saved=ModEquipment.FANG_ARROW.get().createArrow(h.getLevel(),new ItemStack(ModEquipment.FANG_ARROW.get()),p);
        saved.setPierceLevel((byte)2);var nbt=new CompoundTag();saved.save(nbt);nbt.putBoolean("inGround",true);nbt.putByte("shake",(byte)0);
        var loaded=(net.minecraft.world.entity.projectile.AbstractArrow)EntityType.create(nbt,h.getLevel()).orElseThrow();
        h.assertTrue(loaded instanceof com.nhat.tidal_terror.entities.FangArrowEntity&&loaded.getPierceLevel()==2,"Saved fang arrow lost identity/piercing");
        loaded.pickup=net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED;loaded.playerTouch(p);
        h.assertTrue(p.getInventory().countItem(ModEquipment.FANG_ARROW.get())==1,"Embedded arrow did not return fang ammunition");
        h.runAfterDelay(85,()->{near(h,t.getHealth(),health-2,"Arrow base bleed total on dry land");h.succeed();});
    }
    @GameTest(template="reef_life_pool",timeoutTicks=40)
    public static void nativeBowsCrossbowsAndForgeBowSubclassFireFangAmmo(GameTestHelper h) {
        for(var weapon:List.of(Items.BOW,Items.CROSSBOW,TestBowRegistration.BOW)) {
            var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FangBowTest"));var pos=h.absolutePos(new BlockPos(8,10,8));p.setPos(pos.getX(),pos.getY(),pos.getZ());
            var stack=new ItemStack(weapon);p.setItemSlot(EquipmentSlot.MAINHAND,stack);p.getInventory().add(new ItemStack(ModEquipment.FANG_ARROW.get(),8));
            h.assertTrue(((ProjectileWeaponItem)weapon).getAllSupportedProjectiles().test(new ItemStack(ModEquipment.FANG_ARROW.get())),"Weapon rejected arrow ammunition tag");
            h.assertTrue(p.getProjectile(stack).is(ModEquipment.FANG_ARROW.get()),"Native ammunition selection failed");
            for(var e:List.of(com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get(),com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE.get()))
                h.assertTrue(!e.canEnchant(stack)&&!e.canEnchant(new ItemStack(ModEquipment.FANG_ARROW.get())),"Bleeding enchantment accepted ranged gear");
            if(weapon instanceof CrossbowItem bow) {
                bow.releaseUsing(stack,h.getLevel(),p,bow.getUseDuration(stack)-CrossbowItem.getChargeDuration(stack));
                h.assertTrue(CrossbowItem.isCharged(stack)&&CrossbowItem.containsChargedProjectile(stack,ModEquipment.FANG_ARROW.get()),"Crossbow did not load fang ammunition");
                CrossbowItem.performShooting(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND,stack,3.15F,0);
            } else ((BowItem)weapon).releaseUsing(stack,h.getLevel(),p,weapon.getUseDuration(stack)-20);
            var arrows=h.getLevel().getEntitiesOfClass(com.nhat.tidal_terror.entities.FangArrowEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(5)).stream().filter(a->a.getOwner()==p).toList();
            h.assertTrue(arrows.size()==1&&arrows.get(0).getDeltaMovement().length()>2,"Weapon did not fire a native fang arrow");
            h.assertTrue(p.getInventory().countItem(ModEquipment.FANG_ARROW.get())==7,"Special arrow did not consume one ammunition");arrows.forEach(Entity::discard);
        }
        h.succeed();
    }
    @GameTest(template="reef_life_pool",timeoutTicks=40)
    public static void gearTooltipsAndNativeMaterialAnvilRepair(GameTestHelper h) {
        for(var item:List.of(ModEquipment.REEF_SPEAR.get(),ModEquipment.REEF_HELMET.get(),ModEquipment.REEF_CHESTPLATE.get(),ModEquipment.REEF_LEGGINGS.get(),ModEquipment.REEF_BOOTS.get())) {
            var stack=new ItemStack(item);stack.setDamageValue(100);var text=new ArrayList<net.minecraft.network.chat.Component>();
            item.appendHoverText(stack,h.getLevel(),text,TooltipFlag.NORMAL);h.assertTrue(text.size()==(item instanceof ReefSpearItem?3:2),"Gear lost mechanic/repair tooltip");
            var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ReefRepairTest"));var anvil=new net.minecraft.world.inventory.AnvilMenu(0,p.getInventory());
            anvil.getSlot(0).set(stack);anvil.getSlot(1).set(new ItemStack(item instanceof ReefSpearItem?ModEquipment.CRUSHER_TOOTH.get():ModEquipment.SHARDBACK_PLATE.get()));anvil.createResult();
            var repaired=anvil.getSlot(2).getItem();h.assertTrue(repaired.is(item)&&repaired.getDamageValue()<100,"Native material anvil repair failed");
        }
        var recipe=(ShapedRecipe)h.getLevel().getRecipeManager().byKey(new ResourceLocation("tidalterror","fang_arrow")).orElseThrow();
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FangRecipeTest"));var grid=new TransientCraftingContainer(p.inventoryMenu,3,3);grid.setItem(0,new ItemStack(ModEquipment.CRUSHER_TOOTH.get()));grid.setItem(3,new ItemStack(Items.STICK));grid.setItem(6,new ItemStack(Items.FEATHER));
        var result=recipe.assemble(grid,h.getLevel().registryAccess());h.assertTrue(recipe.matches(grid,h.getLevel())&&result.is(ModEquipment.FANG_ARROW.get())&&result.getCount()==4,"Fang arrow recipe failed");h.succeed();
    }

    @GameTest(template="reef_life_pool",timeoutTicks=100)
    public static void crusherBitesBleedOnlyAfterAcceptedDamage(GameTestHelper h) {
        pool(h);var victim=target(h);
        var shark=h.spawn(ModEntities.CORAL_CRUSHER.get(),8,4,8);shark.setNoAi(true);shark.setNoGravity(true);
        h.assertTrue(shark.doHurtTarget(victim),"Native Crusher bite rejected");
        var effect=victim.getEffect(ModEffects.REEF_BLEEDING.get());
        h.assertTrue(effect!=null&&effect.getDuration()==80&&effect.getAmplifier()==0&&!effect.isVisible(),"Crusher bite did not apply base bleeding without potion swirls");
        float health=victim.getHealth();
        var rejected=target(h);rejected.setInvulnerable(true);
        h.assertTrue(!shark.doHurtTarget(rejected)&&!rejected.hasEffect(ModEffects.REEF_BLEEDING.get()),"Rejected bite caused bleeding");
        h.runAfterDelay(85,()->{
            near(h,victim.getHealth(),health-2,"Crusher bleeding total");
            h.assertTrue(!victim.hasEffect(ModEffects.REEF_BLEEDING.get()),"Crusher bleeding did not expire");h.succeed();
        });
    }

    @GameTest(template="reef_life_pool",timeoutTicks=40)
    public static void spearBooksExposeEveryLevelAndExplainBonuses(GameTestHelper h) {
        var tab=com.nhat.tidal_terror.TidalTerror.TIDAL_TERROR_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(),true,h.getLevel().registryAccess()));
        int count=0;
        for(var enchantment:List.of(com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get(),com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE.get())) {
            for(int level=1;level<=enchantment.getMaxLevel();level++) {
                int expected=level;count++;
                var book=tab.getDisplayItems().stream().filter(s->s.is(Items.ENCHANTED_BOOK)&&net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(s).getOrDefault(enchantment,0)==expected).findFirst();
                h.assertTrue(book.isPresent(),"Missing creative spear book level "+expected);
                var lines=new ArrayList<net.minecraft.network.chat.Component>();
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.ItemTooltipEvent(book.get(),h.makeMockSurvivalPlayer(),lines,TooltipFlag.Default.NORMAL));
                h.assertTrue(lines.size()==1,"Book must contain only its general bonus explanation");
                var bonus=(net.minecraft.network.chat.contents.TranslatableContents)lines.get(0).getContents();
                String key=enchantment==com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get()?"tooltip.tidalterror.serration_book":"tooltip.tidalterror.hemorrhage_book";
                h.assertTrue(bonus.getKey().equals(key),"Wrong book explanation");
                near(h,((Number)bonus.getArgs()[0]).doubleValue(),enchantment==com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get()?.5*expected:2*expected,"Book bonus did not reflect its level");
            }
        }
        h.assertTrue(count==5&&tab.getDisplayItems().size()==25,"Wrong creative book/equipment count");h.succeed();
    }
    @GameTest(template="reef_life_pool",timeoutTicks=180)
    public static void enchantedBleedingScalesAndKeepsChargedWaterGate(GameTestHelper h) {
        pool(h); var p=player(h,true); var t=target(h);
        var spear=p.getMainHandItem();
        spear.enchant(com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get(),3);
        spear.enchant(com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE.get(),2);
        p.attack(t);
        var effect=t.getEffect(ModEffects.REEF_BLEEDING.get());
        h.assertTrue(effect!=null&&effect.getAmplifier()==3&&effect.getDuration()==160,"Enchanted hit lost power/duration");
        h.assertTrue(!effect.isVisible()&&effect.showIcon(),"Bleeding must suppress potion swirls but keep its status icon");
        // A weaker spear must not replace an active stronger bleed.
        charge(p);t.invulnerableTime=0;p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModEquipment.REEF_SPEAR.get()));p.attack(t);
        h.assertTrue(t.getEffect(ModEffects.REEF_BLEEDING.get()).getAmplifier()==3,"Weak hit downgraded strong bleed");
        float health=t.getHealth();
        h.runAfterDelay(20,()->near(h,t.getHealth(),health,"Enchanted bleed fired early"));
        h.runAfterDelay(45,()->near(h,t.getHealth(),health-2.5,"Serration first pulse"));
        h.runAfterDelay(85,()->near(h,t.getHealth(),health-5,"Serration second pulse"));
        h.runAfterDelay(125,()->near(h,t.getHealth(),health-7.5,"Hemorrhage third pulse"));
        h.runAfterDelay(165,()->{
            near(h,t.getHealth(),health-10,"Combined enchanted bleed total");
            h.assertTrue(!t.hasEffect(ModEffects.REEF_BLEEDING.get()),"Enhanced bleed did not expire");h.succeed();
        });
    }

    @GameTest(template="reef_life_pool",timeoutTicks=40)
    public static void spearEnchantmentsUseTableBooksAndNativeAnvil(GameTestHelper h) {
        var serration=com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get();
        var hemorrhage=com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE.get();
        var spear=new ItemStack(ModEquipment.REEF_SPEAR.get());
        h.assertTrue(serration.getMaxLevel()==3&&hemorrhage.getMaxLevel()==2&&serration.isCompatibleWith(hemorrhage),"Incorrect enchantment levels/compatibility");
        for(var enchantment:List.of(serration,hemorrhage)) {
            h.assertTrue(enchantment.canEnchant(spear)&&spear.canApplyAtEnchantingTable(enchantment),"Spear enchantment rejected");
            h.assertTrue(!enchantment.canEnchant(new ItemStack(Items.IRON_SWORD))&&!enchantment.canEnchant(new ItemStack(ModEquipment.REEF_HELMET.get())),"Spear enchantment leaked to other equipment");
            h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getAvailableEnchantmentResults(30,spear,false).stream().anyMatch(e->e.enchantment==enchantment),"Enchantment missing from native table candidates");
            var p=h.makeMockSurvivalPlayer();
            var anvil=new net.minecraft.world.inventory.AnvilMenu(0,p.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(h.getLevel(),h.absolutePos(new BlockPos(8,10,8))));
            anvil.getSlot(0).set(spear.copy());
            anvil.getSlot(1).set(EnchantedBookItem.createForEnchantment(new net.minecraft.world.item.enchantment.EnchantmentInstance(enchantment,1)));
            anvil.createResult();
            h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(enchantment,anvil.getSlot(2).getItem())==1,"Native anvil rejected spear book");
        }
        h.succeed();
    }
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
                if(sample.is(entry.getKey().equals("reef_spear")?Items.IRON_INGOT:switch(entry.getKey()){case "reef_helmet"->Items.IRON_HELMET;case "reef_chestplate"->Items.IRON_CHESTPLATE;case "reef_leggings"->Items.IRON_LEGGINGS;default->Items.IRON_BOOTS;}))iron=index;
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
