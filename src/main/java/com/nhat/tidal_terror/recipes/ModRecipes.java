package com.nhat.tidal_terror.recipes;

import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TidalTerror.MODID);
    public static final RegistryObject<RecipeSerializer<ReefArmorUpgradeRecipe>> ARMOR_UPGRADE = SERIALIZERS.register("reef_armor_upgrade", ReefArmorUpgradeRecipe.Serializer::new);
    private ModRecipes() {}
    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
}
