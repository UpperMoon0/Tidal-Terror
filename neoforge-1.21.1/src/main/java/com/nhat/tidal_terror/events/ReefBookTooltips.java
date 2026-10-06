package com.nhat.tidal_terror.events;

@net.neoforged.fml.common.EventBusSubscriber(modid="tidalterror", value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class ReefBookTooltips {
    @net.neoforged.bus.api.SubscribeEvent
    public static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (!stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) return;
        int power = com.nhat.tidal_terror.enchantments.ModEnchantments.level(stack, com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION);
        int duration = com.nhat.tidal_terror.enchantments.ModEnchantments.level(stack, com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE);
        if (power > 0) event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.tidalterror.serration_book", .5F * power).withStyle(net.minecraft.ChatFormatting.AQUA));
        if (duration > 0) event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.tidalterror.hemorrhage_book", 2 * duration).withStyle(net.minecraft.ChatFormatting.AQUA));
    }
}
