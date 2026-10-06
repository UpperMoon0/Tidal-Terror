package com.nhat.tidal_terror.items;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;


public final class ReefArmorItem extends Item {
    private final ArmorType type;
    public ReefArmorItem(String name,ArmorType type) { super(com.nhat.tidal_terror.TidalTerror.properties(name).humanoidArmor(ReefArmorMaterial.INSTANCE,type)); this.type=type; }
    public EquipmentSlot getEquipmentSlot(){return type.getSlot();}

    public static boolean hasFullSet(net.minecraft.world.entity.LivingEntity wearer) {
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(wearer.getItemBySlot(slot).getItem() instanceof ReefArmorItem armor) || armor.getEquipmentSlot() != slot) return false;
        }
        return true;
    }

    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> text, TooltipFlag flags) {
        text.accept(Component.translatable("tooltip.tidalterror.reef_armor").withStyle(ChatFormatting.AQUA));
        text.accept(Component.translatable("tooltip.tidalterror.reef_armor_bleeding").withStyle(ChatFormatting.AQUA));
        text.accept(Component.translatable("tooltip.tidalterror.reef_armor_repair").withStyle(ChatFormatting.GRAY));
    }




}
