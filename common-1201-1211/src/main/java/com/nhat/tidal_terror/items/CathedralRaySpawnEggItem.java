package com.nhat.tidal_terror.items;

import com.nhat.tidal_terror.entities.ModEntities;
import dev.architectury.core.item.ArchitecturySpawnEggItem;

/** Native egg interaction with an untinted ray-themed pixel sprite. */
public final class CathedralRaySpawnEggItem extends ArchitecturySpawnEggItem {
    public CathedralRaySpawnEggItem() {
        super(ModEntities.CATHEDRAL_RAY, 0xffffff, 0xffffff, new Properties());
    }
    @Override public int getColor(int tintIndex) { return 0xffffff; }
}
