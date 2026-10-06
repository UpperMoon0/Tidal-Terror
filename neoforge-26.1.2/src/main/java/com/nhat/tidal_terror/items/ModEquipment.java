package com.nhat.tidal_terror.items;

import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.Item;
import dev.architectury.registry.registries.RegistrySupplier;

public final class ModEquipment {
    public static final RegistrySupplier<Item> CRUSHER_TOOTH = TidalTerror.ITEMS.register("crusher_tooth", () -> new Item(TidalTerror.properties("crusher_tooth")));
    public static final RegistrySupplier<Item> SHARDBACK_PLATE = TidalTerror.ITEMS.register("shardback_plate", () -> new Item(TidalTerror.properties("shardback_plate")));
    public static final RegistrySupplier<FangArrowItem> FANG_ARROW = TidalTerror.ITEMS.register("fang_arrow", FangArrowItem::new);
    public static final RegistrySupplier<ReefSpearItem> REEF_SPEAR = TidalTerror.ITEMS.register("reef_spear", ReefSpearItem::new);
    public static final RegistrySupplier<ReefArmorItem> REEF_HELMET = armor("reef_helmet", ArmorType.HELMET);
    public static final RegistrySupplier<ReefArmorItem> REEF_CHESTPLATE = armor("reef_chestplate", ArmorType.CHESTPLATE);
    public static final RegistrySupplier<ReefArmorItem> REEF_LEGGINGS = armor("reef_leggings", ArmorType.LEGGINGS);
    public static final RegistrySupplier<ReefArmorItem> REEF_BOOTS = armor("reef_boots", ArmorType.BOOTS);

    private static RegistrySupplier<ReefArmorItem> armor(String name, ArmorType type) {
        return TidalTerror.ITEMS.register(name, () -> new ReefArmorItem(name,type));
    }

    private ModEquipment() {}
    public static void register() {}
}
