package com.nhat.tidal_terror.worldgen;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
/** Shared climate admission and bounded cache for normal and deep provinces. */
public final class ReefProvincePlacement {
    private final BiomeSource delegate;
    private final int placementVersion;
    private record Key(long seed, ReefProvinceLayout.Center center) {}
    private final Map<Key, Boolean> eligible = new LinkedHashMap<>(128, .75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, Boolean> e) { return size() > 512; }
    };
    public ReefProvincePlacement(BiomeSource delegate,int version) { this.delegate=delegate;this.placementVersion=version; }
    public ReefProvinceLayout.Sample province(long seed, int x, int z, Climate.Sampler sampler) {
        var sample = ReefProvinceLayout.sample(seed, x, z,placementVersion);
        if (sample.zone() == ReefProvinceLayout.Zone.OCEAN) return null;
        boolean accept;
        synchronized (eligible) {
            Boolean cached=eligible.get(new Key(seed,sample.center()));
            if(cached!=null)return cached?sample:null;
        }
        // Native climate sampling can run on the locator worker. Keep it outside
        // the shared LRU monitor so a search cannot block biome-generation workers.
        accept=oceanEnvelope(sample.center(),ProvinceSeeds.canonical(sampler));
        synchronized(eligible) {eligible.put(new Key(seed,sample.center()),accept);}
        return accept ? sample : null;
    }
    private boolean oceanEnvelope(ReefProvinceLayout.Center c, Climate.Sampler sampler) {
        if (!ocean(c.x(), c.z(), sampler)) return false;
        // Keep the large core in ocean-dominated terrain without trimming any rings.
        // This admits coastal land within the footprint; it is not a shoreline guarantee.
        int total = 0, oceans = 0;
        int step = 128;
        int extent = (int)Math.ceil(ReefProvinceLayout.CORE * 1.11 / step) * step;
        for (int dx = -extent; dx <= extent; dx += step)
            for (int dz = -extent; dz <= extent; dz += step) {
                if (Math.hypot(dx, dz) > ReefProvinceLayout.CORE * 1.11) continue;
                total++;
                if (ocean(c.x() + dx, c.z() + dz, sampler)) oceans++;
            }
        return placementVersion==1?oceans*4>=total*3:oceans * 3 >= total * 2;
    }
    /** Native climate lookup, without generated chunks or loader integration. */
    private Holder<Biome> backgroundBiome(int x, int y, int z, Climate.Sampler sampler) {
        return delegate.getNoiseBiome(x,y,z,sampler);
    }
    private boolean ocean(int x, int z, Climate.Sampler sampler) {
        return backgroundBiome(QuartPos.fromBlock(x), 8, QuartPos.fromBlock(z), sampler).is(BiomeTags.IS_OCEAN);
    }
}
