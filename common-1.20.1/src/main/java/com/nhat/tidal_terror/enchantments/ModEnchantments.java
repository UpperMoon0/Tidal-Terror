package com.nhat.tidal_terror.enchantments;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.items.ReefSpearItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.*;
import dev.architectury.registry.registries.*;

public final class ModEnchantments {
    public static final EnchantmentCategory SPEAR = EnchantmentCategory.WEAPON;
    private static final DeferredRegister<Enchantment> REGISTRY = DeferredRegister.create(TidalTerror.MODID, net.minecraft.core.registries.Registries.ENCHANTMENT);
    public static final RegistrySupplier<Enchantment> SERRATION = REGISTRY.register("serration", () -> new SpearEnchantment(Enchantment.Rarity.UNCOMMON, 3, 12));
    public static final RegistrySupplier<Enchantment> HEMORRHAGE = REGISTRY.register("hemorrhage", () -> new SpearEnchantment(Enchantment.Rarity.RARE, 2, 20));
    private ModEnchantments() {}
    public static void register() { REGISTRY.register(); }
    public static void addBooks(net.minecraft.world.item.CreativeModeTab.Output output) {
        for (var enchantment : new Enchantment[]{SERRATION.get(), HEMORRHAGE.get()}) {
            for (int level = 1; level <= enchantment.getMaxLevel(); level++)
                output.accept(net.minecraft.world.item.EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)));
        }
    }

    private static final class SpearEnchantment extends Enchantment {
        private final int levels, cost;
        SpearEnchantment(Rarity rarity, int levels, int cost) {
            super(rarity, SPEAR, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
            this.levels = levels; this.cost = cost;
        }
        @Override public boolean canEnchant(net.minecraft.world.item.ItemStack stack) { return stack.getItem() instanceof ReefSpearItem; }
        @Override protected boolean checkCompatibility(Enchantment other) {
            return super.checkCompatibility(other) && !(other instanceof DamageEnchantment);
        }
        @Override public int getMaxLevel() { return levels; }
        @Override public int getMinCost(int level) { return cost + (level - 1) * 10; }
        @Override public int getMaxCost(int level) { return getMinCost(level) + 20; }
    }
}
