package com.nhat.tidal_terror.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.nhat.tidal_terror.effects.ModEffects;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;

/** Forge's attack cooldown is reset after hurtEnemy, so charge is still accurate here. */
public final class ReefSpearItem extends Item {
    private static final UUID REACH_UUID = UUID.fromString("533e7a35-85a6-4f4b-919e-45761e28ea29");
    private final Multimap<Attribute, AttributeModifier> modifiers;

    public ReefSpearItem() {
        super(new Properties().durability(250));
        modifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Spear damage", 5, AttributeModifier.Operation.ADDITION))
                .put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Spear speed", -2.9, AttributeModifier.Operation.ADDITION))
                .put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH_UUID, "Spear reach", 1, AttributeModifier.Operation.ADDITION))
                .build();
    }

    @Override public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? modifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide && attacker instanceof Player player
                && player.getAttackStrengthScale(0.5F) >= 0.99F && player.isInWater() && target.isInWater() && target.isAlive()) {
            // Native equal-strength effects refresh duration without stacking damage.
            int power = net.minecraft.util.Mth.clamp(EnchantmentHelper.getItemEnchantmentLevel(com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get(), stack), 0, 3);
            int duration = 80 + 40 * net.minecraft.util.Mth.clamp(EnchantmentHelper.getItemEnchantmentLevel(com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE.get(), stack), 0, 2);
            target.addEffect(new MobEffectInstance(ModEffects.REEF_BLEEDING.get(), duration, power, false, false, true));
            com.nhat.tidal_terror.particles.ModParticles.bleed(target, 12);
        }
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (state.getDestroySpeed(level, pos) != 0) stack.hurtAndBreak(2, miner, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }
    @Override public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) { return !player.isCreative(); }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return ingredient.is(ModEquipment.CRUSHER_TOOTH.get()); }
    @Override public int getEnchantmentValue() { return 14; }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.category == com.nhat.tidal_terror.enchantments.ModEnchantments.SPEAR
                || (enchantment.category == EnchantmentCategory.WEAPON && enchantment != Enchantments.SWEEPING_EDGE)
                || super.canApplyAtEnchantingTable(stack, enchantment);
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> text, TooltipFlag flags) {
        int power = net.minecraft.util.Mth.clamp(EnchantmentHelper.getItemEnchantmentLevel(com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get(), stack), 0, 3);
        int extension = net.minecraft.util.Mth.clamp(EnchantmentHelper.getItemEnchantmentLevel(com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE.get(), stack), 0, 2);
        text.add(Component.translatable("tooltip.tidalterror.reef_spear", (2 + extension) * (1 + .5F * power), 4 + 2 * extension).withStyle(ChatFormatting.AQUA));
        text.add(Component.translatable("tooltip.tidalterror.reef_spear_refresh").withStyle(ChatFormatting.GRAY));
        text.add(Component.translatable("tooltip.tidalterror.reef_spear_repair").withStyle(ChatFormatting.GRAY));
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.nhat.tidal_terror.client.ReefEquipmentClient.spear());
    }
}
