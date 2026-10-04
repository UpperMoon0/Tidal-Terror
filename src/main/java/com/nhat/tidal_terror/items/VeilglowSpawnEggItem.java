package com.nhat.tidal_terror.items;

import com.nhat.tidal_terror.entities.ModEntities;
import net.minecraftforge.common.ForgeSpawnEggItem;

public final class VeilglowSpawnEggItem extends ForgeSpawnEggItem {
    public VeilglowSpawnEggItem() { super(ModEntities.VEILGLOW,0xffffff,0xffffff,new Properties()); }
    @Override public int getColor(int tintIndex) { return 0xffffff; }
}
