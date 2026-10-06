package com.nhat.tidal_terror.client;
public final class ReefEquipmentClient {
 public static final net.neoforged.neoforge.client.extensions.common.IClientItemExtensions SPEAR_EXTENSION=new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){};
 public static final net.neoforged.neoforge.client.extensions.common.IClientItemExtensions ARMOR_EXTENSION=new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
  @Override public net.minecraft.client.model.Model getHumanoidArmorModel(net.minecraft.world.item.ItemStack stack,net.minecraft.client.resources.model.EquipmentClientInfo.LayerType type,net.minecraft.client.model.Model original){
   var item=(com.nhat.tidal_terror.items.ReefArmorItem)stack.getItem();
   return new ReefArmorModel(ReefModelCache.layer(ReefArmorModel.layer(item.getEquipmentSlot())));
  }
 };
}
