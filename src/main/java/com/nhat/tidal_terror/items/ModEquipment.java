package com.nhat.tidal_terror.items;

import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public final class ModEquipment {
    public static final RegistryObject<Item> CRUSHER_TOOTH = TidalTerror.ITEMS.register("crusher_tooth", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SHARDBACK_PLATE = TidalTerror.ITEMS.register("shardback_plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<FangArrowItem> FANG_ARROW = TidalTerror.ITEMS.register("fang_arrow", FangArrowItem::new);
    public static final RegistryObject<ReefSpearItem> REEF_SPEAR = TidalTerror.ITEMS.register("reef_spear", ReefSpearItem::new);
    public static final RegistryObject<ReefArmorItem> REEF_HELMET = armor("reef_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<ReefArmorItem> REEF_CHESTPLATE = armor("reef_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<ReefArmorItem> REEF_LEGGINGS = armor("reef_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<ReefArmorItem> REEF_BOOTS = armor("reef_boots", ArmorItem.Type.BOOTS);

    private static RegistryObject<ReefArmorItem> armor(String name, ArmorItem.Type type) {
        return TidalTerror.ITEMS.register(name, () -> new ReefArmorItem(type));
    }

    private ModEquipment() {}
    public static void register() {}
}
