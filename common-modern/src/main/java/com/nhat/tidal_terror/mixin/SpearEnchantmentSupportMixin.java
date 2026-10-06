package com.nhat.tidal_terror.mixin;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Keep the spear's original weapon enchantment rules across data-driven versions. */
@Mixin(Enchantment.class)
public abstract class SpearEnchantmentSupportMixin {
 @Inject(method={"isSupportedItem","isPrimaryItem","canEnchant"},at=@At("HEAD"),cancellable=true)
 private void spear(ItemStack stack,CallbackInfoReturnable<Boolean> ci){
  if(!(stack.getItem() instanceof com.nhat.tidal_terror.items.ReefSpearItem))return;
  var enchantment=(Enchantment)(Object)this;
  // Vanilla enchantment descriptions have canonical translation keys. Leave
  // custom datapack enchantments to their own supported/primary item sets.
  if(!(enchantment.description().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text))return;
  switch(text.getKey()){
   case "enchantment.minecraft.fire_aspect","enchantment.minecraft.sweeping_edge"->ci.setReturnValue(false);
   case "enchantment.minecraft.sharpness","enchantment.minecraft.smite","enchantment.minecraft.bane_of_arthropods","enchantment.minecraft.looting","enchantment.minecraft.knockback"->ci.setReturnValue(true);
   default->{}
  }
 }
}
