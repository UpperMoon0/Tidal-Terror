package com.nhat.tidal_terror.enchantments;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.items.ReefSpearItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ModEnchantments {
    public static final EnchantmentCategory SPEAR = EnchantmentCategory.create("TIDAL_REEF_SPEAR", item -> item instanceof ReefSpearItem);
    private static final DeferredRegister<Enchantment> REGISTRY = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, TidalTerror.MODID);
    public static final RegistryObject<Enchantment> SERRATION = REGISTRY.register("serration", () -> new SpearEnchantment(Enchantment.Rarity.UNCOMMON, 3, 12));
    public static final RegistryObject<Enchantment> HEMORRHAGE = REGISTRY.register("hemorrhage", () -> new SpearEnchantment(Enchantment.Rarity.RARE, 2, 20));
    private ModEnchantments() {}
    public static void register(IEventBus bus) { REGISTRY.register(bus); }

    private static final class SpearEnchantment extends Enchantment {
        private final int levels, cost;
        SpearEnchantment(Rarity rarity, int levels, int cost) {
            super(rarity, SPEAR, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
            this.levels = levels; this.cost = cost;
        }
        @Override public int getMaxLevel() { return levels; }
        @Override public int getMinCost(int level) { return cost + (level - 1) * 10; }
        @Override public int getMaxCost(int level) { return getMinCost(level) + 20; }
    }
}
