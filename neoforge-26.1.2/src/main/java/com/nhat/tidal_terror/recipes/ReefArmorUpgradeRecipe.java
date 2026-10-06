package com.nhat.tidal_terror.recipes;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.*;
public record ReefArmorUpgradeRecipe(ShapedRecipe shape) implements CraftingRecipe {
 public static final RecipeSerializer<ReefArmorUpgradeRecipe> SERIALIZER=new RecipeSerializer<>(ShapedRecipe.MAP_CODEC.xmap(ReefArmorUpgradeRecipe::new,ReefArmorUpgradeRecipe::shape),ShapedRecipe.STREAM_CODEC.map(ReefArmorUpgradeRecipe::new,ReefArmorUpgradeRecipe::shape));
 @Override public boolean matches(CraftingInput input,net.minecraft.world.level.Level level){return shape.matches(input,level);}
 @Override public ItemStack assemble(CraftingInput input){
  ItemStack result=shape.assemble(input);if(!(result.getItem() instanceof com.nhat.tidal_terror.items.ReefArmorItem armor))return result;
  Item iron=switch(armor.getEquipmentSlot()){case HEAD->Items.IRON_HELMET;case CHEST->Items.IRON_CHESTPLATE;case LEGS->Items.IRON_LEGGINGS;case FEET->Items.IRON_BOOTS;default->Items.AIR;};
  for(int i=0;i<input.size();i++){ItemStack source=input.getItem(i);if(source.is(iron))return source.transmuteCopy(result.getItem(),result.getCount());}
  return ItemStack.EMPTY;
 }
 @Override public CraftingBookCategory category(){return shape.category();}
 @Override public boolean showNotification(){return shape.showNotification();}
 @Override public String group(){return shape.group();}
 @Override public PlacementInfo placementInfo(){return shape.placementInfo();}
 @Override public java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display(){return shape.display();}
 @Override public RecipeSerializer<ReefArmorUpgradeRecipe> getSerializer(){return ModRecipes.ARMOR_UPGRADE.get();}
}
