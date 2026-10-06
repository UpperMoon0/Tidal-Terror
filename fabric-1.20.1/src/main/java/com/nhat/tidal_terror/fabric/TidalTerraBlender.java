package com.nhat.tidal_terror.fabric;
public final class TidalTerraBlender implements terrablender.api.TerraBlenderApi {
    @Override public void onTerraBlenderInitialized() {
        com.nhat.tidal_terror.worldgen.ReefWorldgen.initializeBiomes();
    }
}
