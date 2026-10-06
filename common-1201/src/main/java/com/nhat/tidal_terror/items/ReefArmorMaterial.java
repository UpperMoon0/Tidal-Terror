package com.nhat.tidal_terror.items;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.crafting.Ingredient;

public enum ReefArmorMaterial implements ArmorMaterial {
    INSTANCE;
    @Override public int getDurabilityForType(ArmorItem.Type type) { return ArmorMaterials.IRON.getDurabilityForType(type); }
    @Override public int getDefenseForType(ArmorItem.Type type) {
        return ArmorMaterials.IRON.getDefenseForType(type);
    }
    @Override public int getEnchantmentValue() { return 9; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_TURTLE; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModEquipment.SHARDBACK_PLATE.get()); }
    @Override public String getName() { return "tidalterror:reef"; }
    @Override public float getToughness() { return 0; }
    @Override public float getKnockbackResistance() { return 0; }
}
