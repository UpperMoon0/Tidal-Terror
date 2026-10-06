package com.nhat.tidal_terror.items;

import com.nhat.tidal_terror.entities.ModEntities;
import dev.architectury.core.item.ArchitecturySpawnEggItem;

public final class VeilglowSpawnEggItem extends ArchitecturySpawnEggItem {
    public VeilglowSpawnEggItem() { super(ModEntities.VEILGLOW,0xffffff,0xffffff,new Properties()); }
    @Override public int getColor(int tintIndex) { return 0xffffff; }
}
