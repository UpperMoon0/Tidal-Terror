package com.nhat.tidal_terror.recipes;

import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.world.item.crafting.RecipeSerializer;
import dev.architectury.registry.registries.*;

public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(TidalTerror.MODID, net.minecraft.core.registries.Registries.RECIPE_SERIALIZER);
    public static final RegistrySupplier<RecipeSerializer<ReefArmorUpgradeRecipe>> ARMOR_UPGRADE = SERIALIZERS.register("reef_armor_upgrade", ReefArmorUpgradeRecipe.Serializer::new);
    private ModRecipes() {}
    public static void register() { SERIALIZERS.register(); }
}
