package com.nhat.tidal_terror.recipes;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.*;
import net.minecraft.core.HolderLookup;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
public final class ReefArmorUpgradeRecipe implements CraftingRecipe {
    private final ShapedRecipe shape;
    private ReefArmorUpgradeRecipe(ShapedRecipe shape) { this.shape=shape; }
    public boolean matches(CraftingInput input,net.minecraft.world.level.Level level) { return shape.matches(input,level); }
    public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries) {
        var result=shape.assemble(input,registries);
        if(!(result.getItem() instanceof com.nhat.tidal_terror.items.ReefArmorItem armor))return ItemStack.EMPTY;
        for(int i=0;i<input.size();i++) {
            var source=input.getItem(i);
            if(source.getItem() instanceof ArmorItem old && old.getMaterial().is(net.minecraft.core.registries.BuiltInRegistries.ARMOR_MATERIAL.getKey(ArmorMaterials.IRON.value())) && old.getType()==armor.getType())
                return source.transmuteCopy(result.getItem(),result.getCount());
        }
        return ItemStack.EMPTY;
    }
    public boolean canCraftInDimensions(int width,int height) { return shape.canCraftInDimensions(width,height); }
    public ItemStack getResultItem(HolderLookup.Provider registries) { return shape.getResultItem(registries); }
    public String getGroup() { return shape.getGroup(); }
    public CraftingBookCategory category() { return shape.category(); }
    public net.minecraft.core.NonNullList<Ingredient> getIngredients() { return shape.getIngredients(); }
    public RecipeSerializer<?> getSerializer() { return ModRecipes.ARMOR_UPGRADE.get(); }
    public static final class Serializer implements RecipeSerializer<ReefArmorUpgradeRecipe> {
        public MapCodec<ReefArmorUpgradeRecipe> codec() { return ShapedRecipe.Serializer.CODEC.xmap(ReefArmorUpgradeRecipe::new,r->r.shape); }
        public StreamCodec<RegistryFriendlyByteBuf,ReefArmorUpgradeRecipe> streamCodec() { return ShapedRecipe.Serializer.STREAM_CODEC.map(ReefArmorUpgradeRecipe::new,r->r.shape); }
    }
}
