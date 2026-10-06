package com.nhat.tidal_terror.platform;
public final class ReefMobCategories {
    public static net.minecraft.world.entity.MobCategory create(String name,String id,int cap,boolean friendly,boolean persistent,int distance) {
        return net.minecraft.world.entity.MobCategory.valueOf(name);
    }
}
