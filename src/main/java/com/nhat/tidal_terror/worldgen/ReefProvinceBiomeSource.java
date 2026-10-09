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
    public static final Codec<ReefProvinceBiomeSource> CODEC = RecordCodecBuilder.<ReefProvinceBiomeSource>mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("delegate").forGetter(s -> s.delegate),
            Biome.CODEC.fieldOf("cathedral").forGetter(s -> s.cathedral),
            Biome.CODEC.fieldOf("wastes").forGetter(s -> s.wastes),
            Codec.BOOL.optionalFieldOf("deep",false).forGetter(s -> s.deep),
            Codec.intRange(1,2).optionalFieldOf("placement_version",1).forGetter(s->s.placementVersion)
    ).apply(i, ReefProvinceBiomeSource::new)).flatXmap(ReefProvinceBiomeSource::checkDependency, ReefProvinceBiomeSource::checkDependency).codec();
    private static com.mojang.serialization.DataResult<ReefProvinceBiomeSource> checkDependency(ReefProvinceBiomeSource source) {
        return !source.deep || net.minecraftforge.fml.ModList.get().isLoaded("endless")
                ? com.mojang.serialization.DataResult.success(source)
                : com.mojang.serialization.DataResult.error(() -> "Deep Reef Province requires Endless 0.9.3; reinstall it to load this world.");
    }
    private final BiomeSource delegate;
    private final Holder<Biome> cathedral, wastes;
    private final boolean deep;
    private final int placementVersion;
    private record Key(long seed, ReefProvinceLayout.Center center) {}
    private final Map<Key, Boolean> eligible = new LinkedHashMap<>(128, .75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, Boolean> e) { return size() > 512; }
    };
    public ReefProvinceBiomeSource(BiomeSource delegate, Holder<Biome> cathedral, Holder<Biome> wastes,boolean deep) {
        this(delegate,cathedral,wastes,deep,2);
    }
    public ReefProvinceBiomeSource(BiomeSource delegate, Holder<Biome> cathedral, Holder<Biome> wastes,boolean deep,int placementVersion) {
        this.delegate = delegate; this.cathedral = cathedral; this.wastes = wastes; this.deep=deep;this.placementVersion=placementVersion;
    }
    @Override public int placementVersion(){return placementVersion;}
    @Override public boolean deep() { return deep; }
    @Override protected Codec<? extends BiomeSource> codec() { return CODEC; }
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(delegate.possibleBiomes().stream(), Stream.of(cathedral, wastes));
    }
    @Override public ReefProvinceLayout.Sample province(long seed, int x, int z, Climate.Sampler sampler) {
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
        return placementVersion==1?oceans*4>=total*3:oceans * 3 >= total * 2;
    }
    /** Preserve the native delegate climate in this preset despite global TerraBlender regions. */
    private Holder<Biome> backgroundBiome(int x, int y, int z, Climate.Sampler sampler) {
        return delegate instanceof MultiNoiseBiomeSource multi
                ? multi.getNoiseBiome(sampler.sample(x,y,z)) : delegate.getNoiseBiome(x,y,z,sampler);
    }
    private boolean ocean(int x, int z, Climate.Sampler sampler) {
        return backgroundBiome(QuartPos.fromBlock(x), 8, QuartPos.fromBlock(z), sampler).is(BiomeTags.IS_OCEAN);
    }
    @Override public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        Long seed = ProvinceSeeds.get(sampler);
        if (seed == null || qy >= 16 || (!deep && qy < -16)) return backgroundBiome(qx, qy, qz, sampler);
        var s = province(seed, QuartPos.toBlock(qx), QuartPos.toBlock(qz), sampler);
        return s == null ? backgroundBiome(qx, qy, qz, sampler)
                : s.zone() == ReefProvinceLayout.Zone.CATHEDRAL ? cathedral : wastes;
    }
}
