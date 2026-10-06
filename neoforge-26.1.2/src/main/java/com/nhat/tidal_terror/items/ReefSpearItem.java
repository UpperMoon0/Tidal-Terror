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



/** Forge's attack cooldown is reset after hurtEnemy, so charge is still accurate here. */
public final class ReefSpearItem extends Item {
    private static final UUID REACH_UUID = UUID.fromString("533e7a35-85a6-4f4b-919e-45761e28ea29");
    public ReefSpearItem() {
        super(com.nhat.tidal_terror.TidalTerror.properties("reef_spear").durability(com.nhat.tidal_terror.balance.ReefBalance.SPEAR_DURABILITY).enchantable(14).repairable(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.Identifier.fromNamespaceAndPath("tidalterror","reef_spear_repairs"))).attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_ATTACK_DAMAGE_ID,5,AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED,new AttributeModifier(BASE_ATTACK_SPEED_ID,-2.9,AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ENTITY_INTERACTION_RANGE,new AttributeModifier(net.minecraft.resources.Identifier.fromNamespaceAndPath("tidalterror","spear_reach"),1,AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build()));
    }

    @Override public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide() && attacker instanceof Player player
                && ((com.nhat.tidal_terror.platform.ReefAttackCharge)player).reefAttackCharge() >= 0.99F && player.isInWater() && target.isInWater() && target.isAlive()) {
            // Native equal-strength effects refresh duration without stacking damage.
            int power = bleedingPower(stack);
            int duration = bleedingDuration(stack);
            ModEffects.applyBleeding(target, player, duration, power);
            com.nhat.tidal_terror.particles.ModParticles.bleed(target, 12);
        }
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (state.getDestroySpeed(level, pos) != 0) stack.hurtAndBreak(2, miner, EquipmentSlot.MAINHAND);
        return true;
    }

    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> text, TooltipFlag flags) {
        int duration = bleedingDuration(stack);
        float damage = 1 + .5F * bleedingPower(stack);
        String shownDamage = damage == (int)damage ? Integer.toString((int)damage) : Float.toString(damage);
        text.accept(Component.translatable("tooltip.tidalterror.reef_spear", shownDamage, duration / 20).withStyle(ChatFormatting.AQUA));
        text.accept(Component.translatable("tooltip.tidalterror.reef_spear_refresh").withStyle(ChatFormatting.GRAY));
        text.accept(Component.translatable("tooltip.tidalterror.reef_spear_repair").withStyle(ChatFormatting.GRAY));
    }
    private static int bleedingPower(ItemStack stack) {
        return net.minecraft.util.Mth.clamp(com.nhat.tidal_terror.enchantments.ModEnchantments.level(stack,com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION), 0, 3);
    }
    private static int bleedingDuration(ItemStack stack) {
        return com.nhat.tidal_terror.balance.ReefBalance.bleedingDuration(com.nhat.tidal_terror.enchantments.ModEnchantments.level(stack,com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE));
    }

}
