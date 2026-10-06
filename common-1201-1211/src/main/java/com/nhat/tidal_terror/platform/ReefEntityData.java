package com.nhat.tidal_terror.platform;
public interface ReefEntityData {
    net.minecraft.nbt.CompoundTag reefData();
    static net.minecraft.nbt.CompoundTag get(net.minecraft.world.entity.Entity entity) { return ((ReefEntityData)entity).reefData(); }
}
