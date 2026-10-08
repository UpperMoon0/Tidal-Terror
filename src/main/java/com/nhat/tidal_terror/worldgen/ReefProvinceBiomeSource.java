package com.nhat.tidal_terror.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.*;

/** Opt-in province source. The delegate retains vanilla climate outside accepted provinces. */
public final class ReefProvinceBiomeSource extends BiomeSource implements ReefProvinceAccess {
    public static final Codec<ReefProvinceBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            BiomeSource.CODEC.fieldOf("delegate").forGetter(s -> s.delegate),
            Biome.CODEC.fieldOf("cathedral").forGetter(s -> s.cathedral),
            Biome.CODEC.fieldOf("wastes").forGetter(s -> s.wastes),
            Codec.BOOL.optionalFieldOf("deep",false).forGetter(s -> s.deep)
    ).apply(i, ReefProvinceBiomeSource::new));
    private final BiomeSource delegate;
    private final Holder<Biome> cathedral, wastes;
    private final boolean deep;
    private record Key(long seed, ReefProvinceLayout.Center center) {}
    private final Map<Key, Boolean> eligible = new LinkedHashMap<>(128, .75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, Boolean> e) { return size() > 512; }
    };
    public ReefProvinceBiomeSource(BiomeSource delegate, Holder<Biome> cathedral, Holder<Biome> wastes,boolean deep) {
        this.delegate = delegate; this.cathedral = cathedral; this.wastes = wastes; this.deep=deep;
    }
    @Override public boolean deep() { return deep; }
    @Override protected Codec<? extends BiomeSource> codec() { return CODEC; }
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(delegate.possibleBiomes().stream(), Stream.of(cathedral, wastes));
    }
    @Override public ReefProvinceLayout.Sample province(long seed, int x, int z, Climate.Sampler sampler) {
        var sample = ReefProvinceLayout.sample(seed, x, z);
        if (sample.zone() == ReefProvinceLayout.Zone.OCEAN) return null;
        boolean accept;
        synchronized (eligible) {
            accept = eligible.computeIfAbsent(new Key(seed, sample.center()), k -> oceanEnvelope(k.center, ProvinceSeeds.canonical(sampler)));
        }
        return accept ? sample : null;
    }
    public BiomeSource delegate() { return delegate; }
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
        return oceans * 4 >= total * 3;
    }
    private boolean ocean(int x, int z, Climate.Sampler sampler) {
        return delegate.getNoiseBiome(QuartPos.fromBlock(x), 8, QuartPos.fromBlock(z), sampler).is(BiomeTags.IS_OCEAN);
    }
    @Override public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        Long seed = ProvinceSeeds.get(sampler);
        if (seed == null || qy >= 16 || (!deep && qy < -16)) return delegate.getNoiseBiome(qx, qy, qz, sampler);
        var s = province(seed, QuartPos.toBlock(qx), QuartPos.toBlock(qz), sampler);
        return s == null ? delegate.getNoiseBiome(qx, qy, qz, sampler)
                : s.zone() == ReefProvinceLayout.Zone.CATHEDRAL ? cathedral : wastes;
    }
}
