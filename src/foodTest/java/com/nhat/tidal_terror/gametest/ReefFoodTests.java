package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.entities.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("tidalterror")
@PrefixGameTestTemplate(false)
public class ReefFoodTests {
    private static final String[] FOODS = {"coral_crusher_steak", "cathedral_ray_wing", "veilglow_gel", "shardback_claw"};
    private static Item food(String state, String name) {
        return BuiltInRegistries.ITEM.get(new ResourceLocation("tidalterror", state + "_" + name));
    }

    @GameTest(template="coral_crusher_pool", timeoutTicks=40)
    public static void actualDeathDropsRawAndFireCooked(GameTestHelper h) {
        var types = java.util.List.of(ModEntities.CORAL_CRUSHER.get(), ModEntities.CATHEDRAL_RAY.get(),
                ModEntities.VEILGLOW.get(), ModEntities.SHARDBACK.get());
        for (int i=0; i<types.size(); i++) for (boolean burning : new boolean[]{false,true}) {
            var pos=h.absolutePos(new BlockPos(8,5,8));
            Mob mob=types.get(i).create(h.getLevel());
            mob.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
            mob.setNoAi(true);
            h.getLevel().addFreshEntity(mob);
            if (burning) mob.setSecondsOnFire(30);
            h.assertTrue(mob.hurt(h.getLevel().damageSources().generic(),1000),"Mob rejected lethal damage");
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(3));
            int count=0;
            for(var drop:drops) {
                h.assertTrue(drop.getItem().is(food(burning?"cooked":"raw",FOODS[i])),"Unexpected death drop: "+drop.getItem());
                count+=drop.getItem().getCount(); drop.discard();
            }
            int min=i==0?2:1, max=i==0?4:i==1?3:2;
            h.assertTrue(count>=min&&count<=max,"Wrong loot amount for "+FOODS[i]+": "+count);
            mob.discard();
        }
        h.succeed();
    }

    @GameTest(template="coral_crusher_pool", timeoutTicks=40)
    public static void cookingSerializersAndFoodConsumption(GameTestHelper h) {
        var player=FakePlayerFactory.getMinecraft(h.getLevel());
        for(String name:FOODS) {
            var raw=food("raw",name);var cooked=food("cooked",name);
            for(var type:java.util.List.of(RecipeType.SMELTING,RecipeType.SMOKING,RecipeType.CAMPFIRE_COOKING)) {
                var recipe=h.getLevel().getRecipeManager().getRecipeFor(type,new SimpleContainer(new ItemStack(raw)),h.getLevel());
                h.assertTrue(recipe.isPresent(),"Missing native cooking recipe "+type+" for "+name);
                h.assertTrue(recipe.get().getResultItem(h.getLevel().registryAccess()).is(cooked),"Wrong cooked output");
            }
            for(Item item:java.util.List.of(raw,cooked)) {
                player.getFoodData().setFoodLevel(0); player.getFoodData().setSaturation(0);
                var stack=new ItemStack(item,2);stack.finishUsingItem(h.getLevel(),player);
                h.assertTrue(stack.getCount()==1,"Food was not consumed");
                h.assertTrue(player.getFoodData().getFoodLevel()==item.getFoodProperties().getNutrition(),"Food did not restore hunger");
            }
            h.assertTrue(cooked.getFoodProperties().getNutrition()>raw.getFoodProperties().getNutrition(),"Cooking should improve food");
        }
        h.succeed();
    }

    @GameTest(template="coral_crusher_pool", timeoutTicks=500)
    public static void realFurnacesCookEveryDrop(GameTestHelper h) {
        for(int i=0;i<FOODS.length;i++) {
            var pos=new BlockPos(3+i*3,3,3);h.setBlock(pos,Blocks.FURNACE);
            var furnace=(FurnaceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
            furnace.setItem(0,new ItemStack(food("raw",FOODS[i])));
            furnace.setItem(1,new ItemStack(Items.COAL));furnace.setChanged();
        }
        h.succeedWhen(()->{
            for(int i=0;i<FOODS.length;i++) {
                var furnace=(FurnaceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(3+i*3,3,3)));
                h.assertTrue(furnace.getItem(2).is(food("cooked",FOODS[i])),"Furnace hasn't cooked "+FOODS[i]);
                h.assertTrue(furnace.getItem(0).isEmpty(),"Furnace did not consume raw input");
            }
        });
    }
}
