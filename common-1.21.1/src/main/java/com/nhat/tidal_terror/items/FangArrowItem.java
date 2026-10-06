package com.nhat.tidal_terror.items;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Standard ArrowItem factory used by native and Forge-compatible ranged weapons. */
public final class FangArrowItem extends ArrowItem {
    public FangArrowItem() { super(new Properties()); }
    @Override public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new com.nhat.tidal_terror.entities.FangArrowEntity(level, shooter,stack,weapon);
    }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> text, TooltipFlag flags) {
        text.add(Component.translatable("tooltip.tidalterror.fang_arrow").withStyle(ChatFormatting.AQUA));
    }
}
