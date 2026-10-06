package com.nhat.tidal_terror.items;
import net.minecraft.world.item.equipment.*;
public final class ReefArmorMaterial {
 public static final ArmorMaterial INSTANCE=new ArmorMaterial(15,java.util.Map.of(ArmorType.HELMET,2,ArmorType.CHESTPLATE,6,ArmorType.LEGGINGS,5,ArmorType.BOOTS,2),9,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_TURTLE,0,0,
  net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.Identifier.fromNamespaceAndPath("tidalterror","reef_armor_repairs")),
  net.minecraft.resources.ResourceKey.create(EquipmentAssets.ROOT_ID,net.minecraft.resources.Identifier.fromNamespaceAndPath("tidalterror","reef")));
}
