package com.nhat.tidal_terror.worldgen;
public final class ReefProvinceMode {
    public static boolean enabled() {
        return Boolean.getBoolean("tidalterror.provincePrototype")
                || ReefProvinceMode.class.getResource("/reef-province-prototype.marker") != null;
    }
    private ReefProvinceMode() {}
}
