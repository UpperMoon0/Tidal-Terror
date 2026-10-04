package com.nhat.tidal_terror.items;
import com.nhat.tidal_terror.entities.ModEntities;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
public class ShardbackSpawnEggItem extends ForgeSpawnEggItem {
 public ShardbackSpawnEggItem(){super(ModEntities.SHARDBACK,0xffffff,0xffffff,new Item.Properties());}
 @Override public int getColor(int layer){return 0xffffff;}
}
