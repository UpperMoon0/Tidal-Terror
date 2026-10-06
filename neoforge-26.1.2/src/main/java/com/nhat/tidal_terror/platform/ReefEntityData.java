package com.nhat.tidal_terror.platform;
public interface ReefEntityData {
 static net.minecraft.nbt.CompoundTag get(net.minecraft.world.entity.Entity entity){return entity.getPersistentData();}
}
