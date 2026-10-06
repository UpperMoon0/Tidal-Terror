package com.nhat.tidal_terror.mixin;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EnchantmentHelper.class)
public abstract class SpearEnchantingMixin {
 @Inject(method="getAvailableEnchantmentResults",at=@At("RETURN"),cancellable=true)
 private static void candidates(int cost,ItemStack stack,boolean treasure,CallbackInfoReturnable<java.util.List<EnchantmentInstance>> ci){
  var result=new java.util.ArrayList<>(ci.getReturnValue());
  boolean spear=stack.getItem() instanceof com.nhat.tidal_terror.items.ReefSpearItem;
  boolean book=stack.is(net.minecraft.world.item.Items.BOOK);
  result.removeIf(e->(!spear&&!book&&(e.enchantment==com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get()||e.enchantment==com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE.get()))
    ||(spear&&(e.enchantment==Enchantments.FIRE_ASPECT||e.enchantment==Enchantments.SWEEPING_EDGE)));
  if(spear)for(var enchantment:net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT){
   if((enchantment.isTreasureOnly()&&!treasure)||!enchantment.isDiscoverable()||!enchantment.canEnchant(stack)||result.stream().anyMatch(e->e.enchantment==enchantment))continue;
   for(int level=enchantment.getMaxLevel();level>=enchantment.getMinLevel();level--)if(cost>=enchantment.getMinCost(level)&&cost<=enchantment.getMaxCost(level)){result.add(new EnchantmentInstance(enchantment,level));break;}
  }
  ci.setReturnValue(result);
 }
}
