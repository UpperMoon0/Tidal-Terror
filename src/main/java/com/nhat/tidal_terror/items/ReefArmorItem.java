package com.nhat.tidal_terror.items;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class ReefArmorItem extends ArmorItem {
    public ReefArmorItem(Type type) { super(ReefArmorMaterial.INSTANCE, type, new Properties()); }

    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> text, TooltipFlag flags) {
        text.add(Component.translatable("tooltip.tidalterror.reef_armor").withStyle(ChatFormatting.AQUA));
    }

    @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "tidalterror:textures/models/armor/reef.png";
    }

    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.nhat.tidal_terror.client.ReefEquipmentClient.armor());
    }
}
