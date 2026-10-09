package com.nhat.tidal_terror.worldgen;
public final class ProvinceDependencies {
    public static boolean endless(){ return net.minecraftforge.fml.ModList.get().isLoaded("endless"); }
    private ProvinceDependencies(){}
}
