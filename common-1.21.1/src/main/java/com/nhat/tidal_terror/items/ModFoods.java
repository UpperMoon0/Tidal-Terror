package com.nhat.tidal_terror.items;

import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import dev.architectury.registry.registries.RegistrySupplier;

/** Food shared by every skin of a mob; cooking uses vanilla recipe serializers. */
public final class ModFoods {
    public static final RegistrySupplier<Item> RAW_CORAL_CRUSHER_STEAK = food("raw_coral_crusher_steak", 3, 0.3F);
    public static final RegistrySupplier<Item> COOKED_CORAL_CRUSHER_STEAK = food("cooked_coral_crusher_steak", 8, 0.8F);
    public static final RegistrySupplier<Item> RAW_CATHEDRAL_RAY_WING = food("raw_cathedral_ray_wing", 2, 0.2F);
    public static final RegistrySupplier<Item> COOKED_CATHEDRAL_RAY_WING = food("cooked_cathedral_ray_wing", 6, 0.6F);
    public static final RegistrySupplier<Item> RAW_VEILGLOW_GEL = food("raw_veilglow_gel", 1, 0.1F);
    public static final RegistrySupplier<Item> COOKED_VEILGLOW_GEL = food("cooked_veilglow_gel", 4, 0.4F);
    public static final RegistrySupplier<Item> RAW_SHARDBACK_CLAW = food("raw_shardback_claw", 2, 0.3F);
    public static final RegistrySupplier<Item> COOKED_SHARDBACK_CLAW = food("cooked_shardback_claw", 5, 0.6F);

    private ModFoods() {}

    private static RegistrySupplier<Item> food(String name, int nutrition, float saturation) {
        return TidalTerror.ITEMS.register(name, () -> new Item(new Item.Properties().food(
                new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build())));
    }

    public static void register() { /* Trigger registration before ITEMS is attached to the event bus. */ }
}
