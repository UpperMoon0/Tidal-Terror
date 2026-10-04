package com.nhat.tidal_terror.items;

import com.nhat.tidal_terror.entities.ModEntities;
import net.minecraftforge.common.ForgeSpawnEggItem;

/** Native egg interaction with an untinted ray-themed pixel sprite. */
public final class CathedralRaySpawnEggItem extends ForgeSpawnEggItem {
    public CathedralRaySpawnEggItem() {
        super(ModEntities.CATHEDRAL_RAY, 0xffffff, 0xffffff, new Properties());
    }
    @Override public int getColor(int tintIndex) { return 0xffffff; }
}
