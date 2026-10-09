package com.nhat.tidal_terror.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.*;

/** Default ocean province source. The delegate retains vanilla climate outside accepted provinces. */
public final class ReefProvinceBiomeSource extends BiomeSource implements ReefProvinceAccess {
    public static final Codec<ReefProvinceBiomeSource> CODEC = RecordCodecBuilder.<ReefProvinceBiomeSource>mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("delegate").forGetter(s -> s.delegate),
            Biome.CODEC.fieldOf("cathedral").forGetter(s -> s.cathedral),
            Biome.CODEC.fieldOf("wastes").forGetter(s -> s.wastes),
            Codec.BOOL.optionalFieldOf("deep",false).forGetter(s -> s.deep),
            Codec.intRange(1,2).optionalFieldOf("placement_version",1).forGetter(s->s.placementVersion)
    ).apply(i, ReefProvinceBiomeSource::new)).flatXmap(ReefProvinceBiomeSource::checkDependency, ReefProvinceBiomeSource::checkDependency).codec();
    private static com.mojang.serialization.DataResult<ReefProvinceBiomeSource> checkDependency(ReefProvinceBiomeSource source) {
        return !source.deep || ProvinceDependencies.endless()
                ? com.mojang.serialization.DataResult.success(source)
                : com.mojang.serialization.DataResult.error(() -> "Deep Reef Province requires Endless 0.9.3; reinstall it to load this world.");
    }
    private final BiomeSource delegate;
    private final Holder<Biome> cathedral, wastes;
    private final boolean deep;
    private final int placementVersion;
    private final ReefProvincePlacement placement;
    public ReefProvinceBiomeSource(BiomeSource delegate, Holder<Biome> cathedral, Holder<Biome> wastes,boolean deep) {
        this(delegate,cathedral,wastes,deep,2);
    }
    public ReefProvinceBiomeSource(BiomeSource delegate, Holder<Biome> cathedral, Holder<Biome> wastes,boolean deep,int placementVersion) {
        this.delegate = delegate; this.cathedral = cathedral; this.wastes = wastes; this.deep=deep;this.placementVersion=placementVersion;
        this.placement=new ReefProvincePlacement(delegate,placementVersion);
    }
    @Override public int placementVersion(){return placementVersion;}
    @Override public boolean deep() { return deep; }
    @Override protected Codec<? extends BiomeSource> codec() { return CODEC; }
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(delegate.possibleBiomes().stream(), Stream.of(cathedral, wastes));
    }
    @Override public ReefProvinceLayout.Sample province(long seed, int x, int z, Climate.Sampler sampler) {
        return placement.province(seed,x,z,sampler);
    }
    public BiomeSource delegate() { return delegate; }
    private Holder<Biome> backgroundBiome(int x,int y,int z,Climate.Sampler sampler) {
        return delegate.getNoiseBiome(x,y,z,sampler);
    }
    @Override public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        Long seed = ProvinceSeeds.get(sampler);
        if (seed == null || qy >= 16 || (!deep && qy < -16)) return backgroundBiome(qx, qy, qz, sampler);
        var s = province(seed, QuartPos.toBlock(qx), QuartPos.toBlock(qz), sampler);
        return s == null ? backgroundBiome(qx, qy, qz, sampler)
                : s.zone() == ReefProvinceLayout.Zone.CATHEDRAL ? cathedral : wastes;
    }
}
