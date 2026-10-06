package com.nhat.tidal_terror.recipes;

import com.google.gson.JsonObject;
import com.nhat.tidal_terror.items.ReefArmorItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

/** Shaped crafting with the iron armor's serialized item data carried into the upgrade. */
public final class ReefArmorUpgradeRecipe extends ShapedRecipe {
    private ReefArmorUpgradeRecipe(ShapedRecipe shape) {
        super(shape.getId(), shape.getGroup(), shape.category(), shape.getWidth(), shape.getHeight(),
                shape.getIngredients(), shape.getResultItem(RegistryAccess.EMPTY), shape.showNotification());
    }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess registries) {
        var output = super.assemble(grid, registries);
        if (!(output.getItem() instanceof ReefArmorItem resultArmor)) return ItemStack.EMPTY;
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            var input = grid.getItem(slot);
            if (input.getItem() instanceof ArmorItem armor && armor.getMaterial() == ArmorMaterials.IRON
                    && armor.getType() == resultArmor.getType()) {
                // Includes enchantments, name, damage, repair cost, trims and Forge capability data.
                var saved = input.save(new CompoundTag());
                saved.putString("id", BuiltInRegistries.ITEM.getKey(output.getItem()).toString());
                saved.putByte("Count", (byte) output.getCount());
                return ItemStack.of(saved);
            }
        }
        return ItemStack.EMPTY;
    }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ARMOR_UPGRADE.get(); }
    public static final class Serializer implements RecipeSerializer<ReefArmorUpgradeRecipe> {
        private final ShapedRecipe.Serializer shape = new ShapedRecipe.Serializer();
        @Override public ReefArmorUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ReefArmorUpgradeRecipe(shape.fromJson(id, json));
        }
        @Override public ReefArmorUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            var recipe = shape.fromNetwork(id, buffer);
            return recipe == null ? null : new ReefArmorUpgradeRecipe(recipe);
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, ReefArmorUpgradeRecipe recipe) { shape.toNetwork(buffer, recipe); }
    }
}
