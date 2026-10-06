package com.nhat.tidal_terror.mixin;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Enchantment.class)
public abstract class SpearEnchantmentSupportMixin {
 @Inject(method="canEnchant",at=@At("HEAD"),cancellable=true)
 private void supported(ItemStack stack,CallbackInfoReturnable<Boolean> ci){
  if(!(stack.getItem() instanceof com.nhat.tidal_terror.items.ReefSpearItem))return;
  Enchantment self=(Enchantment)(Object)this;
  if(self==Enchantments.FIRE_ASPECT||self==Enchantments.SWEEPING_EDGE)ci.setReturnValue(false);
  else if(self.category==EnchantmentCategory.WEAPON)ci.setReturnValue(true);
 }
}
