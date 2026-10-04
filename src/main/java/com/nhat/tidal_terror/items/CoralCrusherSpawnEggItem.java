package com.nhat.tidal_terror.items;

import com.nhat.tidal_terror.entities.ModEntities;
import net.minecraftforge.common.ForgeSpawnEggItem;

/** Native spawn egg behavior with an untinted, mob-themed inventory sprite. */
public final class CoralCrusherSpawnEggItem extends ForgeSpawnEggItem {
    public CoralCrusherSpawnEggItem() {
        super(ModEntities.CORAL_CRUSHER, 0xffffff, 0xffffff, new Properties());
    }

    @Override
    public int getColor(int tintIndex) {
        return 0xffffff;
    }
}
