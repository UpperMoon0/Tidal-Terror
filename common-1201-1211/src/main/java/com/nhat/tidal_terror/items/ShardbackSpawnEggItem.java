package com.nhat.tidal_terror.items;
import com.nhat.tidal_terror.entities.ModEntities;
import net.minecraft.world.item.Item;
import dev.architectury.core.item.ArchitecturySpawnEggItem;
public class ShardbackSpawnEggItem extends ArchitecturySpawnEggItem {
 public ShardbackSpawnEggItem(){super(ModEntities.SHARDBACK,0xffffff,0xffffff,new Item.Properties());}
 @Override public int getColor(int layer){return 0xffffff;}
}
