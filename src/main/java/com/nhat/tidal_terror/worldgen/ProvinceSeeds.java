package com.nhat.tidal_terror.worldgen;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import net.minecraft.world.level.biome.Climate;

/** Samplers belong to one RandomState. Weak keys avoid retaining closed worlds. */
public final class ProvinceSeeds {
    private static final Map<Climate.Sampler, Long> SEEDS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Climate.Sampler, WeakReference<Climate.Sampler>> CANONICAL = Collections.synchronizedMap(new WeakHashMap<>());
    public static void bind(Climate.Sampler sampler, long seed) { SEEDS.put(sampler, seed); }
    public static void bindAlias(Climate.Sampler sampler, Climate.Sampler canonical) {
        Long seed = get(canonical);
        if (seed != null) {
            bind(sampler, seed);
            CANONICAL.put(sampler, new WeakReference<>(canonical));
        }
    }
    public static Climate.Sampler canonical(Climate.Sampler sampler) {
        var ref = CANONICAL.get(sampler);
        var canonical = ref == null ? null : ref.get();
        return canonical == null ? sampler : canonical;
    }
    public static Long get(Climate.Sampler sampler) { return SEEDS.get(sampler); }
    private ProvinceSeeds() {}
}
