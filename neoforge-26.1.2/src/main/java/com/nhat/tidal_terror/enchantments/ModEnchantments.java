package com.nhat.tidal_terror.enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> SERRATION=ResourceKey.create(Registries.ENCHANTMENT,Identifier.fromNamespaceAndPath("tidalterror","serration"));
    public static final ResourceKey<Enchantment> HEMORRHAGE=ResourceKey.create(Registries.ENCHANTMENT,Identifier.fromNamespaceAndPath("tidalterror","hemorrhage"));
    public static int level(net.minecraft.world.item.ItemStack stack,ResourceKey<Enchantment> key) {
        var values=stack.getOrDefault(stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)?net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS:net.minecraft.core.component.DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        for(var entry:values.entrySet())if(entry.getKey().is(key))return entry.getIntValue();
        return 0;
    }
    public static void addBooks(net.minecraft.core.HolderLookup.Provider registries,net.minecraft.world.item.CreativeModeTab.Output output) {
        var lookup=registries.lookupOrThrow(Registries.ENCHANTMENT);
        for(var key:java.util.List.of(SERRATION,HEMORRHAGE)) {
            var holder=lookup.getOrThrow(key);
            for(int level=1;level<=holder.value().getMaxLevel();level++)
                output.accept(net.minecraft.world.item.enchantment.EnchantmentHelper.createBook(new net.minecraft.world.item.enchantment.EnchantmentInstance(holder,level)));
        }
    }
}
