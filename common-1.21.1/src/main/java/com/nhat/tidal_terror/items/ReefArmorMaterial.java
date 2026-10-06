package com.nhat.tidal_terror.items;
import net.minecraft.world.item.*;
public final class ReefArmorMaterial {
    public static final net.minecraft.core.Holder<ArmorMaterial> INSTANCE=net.minecraft.core.Holder.direct(new ArmorMaterial(
        java.util.Map.of(ArmorItem.Type.HELMET,2,ArmorItem.Type.CHESTPLATE,6,ArmorItem.Type.LEGGINGS,5,ArmorItem.Type.BOOTS,2,ArmorItem.Type.BODY,6),9,
        net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_TURTLE,()->net.minecraft.world.item.crafting.Ingredient.of(ModEquipment.SHARDBACK_PLATE.get()),
        java.util.List.of(new ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tidalterror","reef"))),0,0));
}
