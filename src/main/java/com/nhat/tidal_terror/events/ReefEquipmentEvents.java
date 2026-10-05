package com.nhat.tidal_terror.events;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.items.ReefArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TidalTerror.MODID)
public final class ReefEquipmentEvents {
    @SubscribeEvent public static void enchantmentBookTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        if (!event.getItemStack().is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) return;
        var enchantments = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(event.getItemStack());
        int power = net.minecraft.util.Mth.clamp(enchantments.getOrDefault(com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get(), 0), 0, 3);
        int duration = net.minecraft.util.Mth.clamp(enchantments.getOrDefault(com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE.get(), 0), 0, 2);
        if (power == 0 && duration == 0) return;
        if (power > 0) event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.tidalterror.serration_book", .5F * power).withStyle(net.minecraft.ChatFormatting.AQUA));
        if (duration > 0) event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.tidalterror.hemorrhage_book", 2 * duration, 4 + 2 * duration).withStyle(net.minecraft.ChatFormatting.AQUA));
    }
    @SubscribeEvent public static void bloodTrail(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide || entity.tickCount % 8 != 0 || !entity.isAlive()) return;
        var effect = entity.getEffect(com.nhat.tidal_terror.effects.ModEffects.REEF_BLEEDING.get());
        if (effect != null) com.nhat.tidal_terror.particles.ModParticles.bleed(entity, 3 + net.minecraft.util.Mth.clamp(effect.getAmplifier(), 0, 3));
    }
    @SubscribeEvent public static void anchor(LivingKnockBackEvent event) {
        var wearer = event.getEntity();
        if (!wearer.isInWater() || !wearer.onGround()) return;
        int pieces = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR && wearer.getItemBySlot(slot).getItem() instanceof ReefArmorItem) pieces++;
        }
        event.setStrength(event.getStrength() * (1 - 0.05F * pieces));
    }
}
