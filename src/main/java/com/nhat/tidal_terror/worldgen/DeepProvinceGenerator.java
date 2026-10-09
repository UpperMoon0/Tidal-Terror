package com.nhat.tidal_terror.worldgen;

import com.nstut.endless.heights.EndlessHeights;
import com.nstut.endless.vertical.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import java.util.*;

/** Bounded sparse generation for the opt-in deep preset. Never widens dense arrays. */
public final class DeepProvinceGenerator {
    private static final java.util.concurrent.atomic.AtomicInteger ADMISSION_BUILDS=new java.util.concurrent.atomic.AtomicInteger();
    public static int admissionBuilds() { return ADMISSION_BUILDS.get(); }
    private record PlanKey(long seed,int height,int style) {}
    private static final Map<PlanKey,CoralGeometry.Plan> PLANS=new LinkedHashMap<>(64,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<PlanKey,CoralGeometry.Plan> e) { return size()>64; }
    };
    private static final Map<ChunkAccess,Map<Integer,LevelChunkSection>> PREPARED=
        Collections.synchronizedMap(new WeakHashMap<>());

    /** Prepare only a requested FULL protochunk, on a native worker before main-thread admission. */
    public static void prepare(WorldGenLevel context,ChunkAccess chunk) {
        if(PREPARED.containsKey(chunk)) return;
        var sections=build(context,chunk);
        PREPARED.put(chunk,sections);
    }

    public static void generate(ServerLevel level,LevelChunk chunk,ProtoChunk proto) {
        var source=level.getChunkSource().getGenerator().getBiomeSource();
        if(!(source instanceof ReefProvinceAccess access) || !access.deep()) return;
        var sections=PREPARED.remove(proto);
        // Compatibility fallback for a generator that omitted the basin feature.
        if(sections==null) { ADMISSION_BUILDS.incrementAndGet();sections=build(level,chunk); }
        if(sections.isEmpty()) return;
        int mx=chunk.getPos().getMinBlockX(),mz=chunk.getPos().getMinBlockZ();
        var vertical=EndlessVerticalEngine.world(level);
        Map<Integer,VerticalPage<LevelChunkSection>> pages=new TreeMap<>();
        sections.forEach((sy,section)->pages.computeIfAbsent(Math.floorDiv(sy,32),VerticalPage::new).putSection(sy,section));
        for(var entry:pages.entrySet()) {
            var pos=new VerticalPagePos(mx>>4,entry.getKey(),mz>>4);
            // Sparse saves can reach disk before the native protochunk becomes a
            // saved FULL chunk. The surviving page, including edits and missing
            // sections, is authoritative; never regenerate or merge into it.
            synchronized(vertical) {
                if(!vertical.pageExists(pos))vertical.installGeneratedPage(pos,entry.getValue());
            }
        }
        chunk.setUnsaved(true);
    }

    private static Map<Integer,LevelChunkSection> build(WorldGenLevel context,ChunkAccess chunk) {
        var level=context.getLevel();
        var generator=level.getChunkSource().getGenerator();
        if(!(generator.getBiomeSource() instanceof ReefProvinceAccess access) || !access.deep())return Map.of();
        int mx=chunk.getPos().getMinBlockX(),mz=chunk.getPos().getMinBlockZ();
        var terrain=new ReefTerrain(context,generator);
        var samples=new ReefProvinceLayout.Sample[256];int[] floors=new int[256];boolean any=false;
        for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
            int i=x*16+z;samples[i]=terrain.provinceSample(mx+x,mz+z);
            if(samples[i]!=null) { floors[i]=terrain.floor(mx+x,mz+z);any=true; }
        }
        if(!any)return Map.of();
        if(EndlessHeights.getMinBuildHeight()>ReefProvinceLayout.DEEP_BOTTOM || EndlessHeights.getDenseMinBuildHeight()!=-64)
            throw new IllegalStateException("Deep Reef Province requires a fresh vanilla dense core and Endless minBuildHeight <= -512");
        var sections=new TreeMap<Integer,LevelChunkSection>();
        boolean full=true;int lowestFloor=Integer.MAX_VALUE,highestFloor=Integer.MIN_VALUE;
        for(int i=0;i<256;i++) {
            full &= samples[i]!=null;
            if(samples[i]!=null) { lowestFloor=Math.min(lowestFloor,floors[i]);highestFloor=Math.max(highestFloor,floors[i]); }
        }
        var biomes=level.registryAccess().registryOrThrow(Registries.BIOME);
        var sampler=level.getChunkSource().randomState().sampler();
        for(int sy=ReefProvinceLayout.DEEP_BOTTOM>>4;sy< -4;sy++) {
            int bottom=sy*16;
            // Entire sections above every floor are water. Those strictly below
            // every bedrock sheet are solid rock. Boundary sections keep the exact loop.
            BlockState uniform=full && bottom>highestFloor?Blocks.WATER.defaultBlockState()
                :full && bottom+15<ReefProvinceLayout.bedrockBase(lowestFloor)?Blocks.DEEPSLATE.defaultBlockState():null;
            var section=uniform==null?new LevelChunkSection(biomes):VerticalSectionFactory.uniform(biomes,uniform);
            if(uniform==null)
            for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
                int i=x*16+z;if(samples[i]==null)continue;int floor=floors[i];
                int sand=6+(int)Math.floorMod((long)(mx+x)*31+(long)(mz+z)*17,3);
                for(int y=0;y<16;y++) {
                    int wy=bottom+y;
                    BlockState state=ReefProvinceLayout.bedrock(level.getSeed(),mx+x,wy,mz+z,floor)?Blocks.BEDROCK.defaultBlockState()
                        :wy>floor?Blocks.WATER.defaultBlockState():wy>floor-sand?Blocks.SAND.defaultBlockState():Blocks.DEEPSLATE.defaultBlockState();
                    section.setBlockState(x,y,z,state,false);
                }
            }
            final int sectionY=sy;
            section.fillBiomesFromNoise((qx,qy,qz,s)->generator.getBiomeSource().getNoiseBiome(qx,qy,qz,s),
                    sampler,mx>>2,sectionY*4,mz>>2);
            if(!section.hasOnlyAir())sections.put(sy,section);
        }
        var blocks=new PrivateReefBlockAccess(sections,chunk,terrain,level.getSeed(),level.getSeaLevel());
        // Match legacy order: seabed gardens first, then giant bodies/crown fans.
        ReefGardenFeature.decorate(blocks,terrain,level.getSeed(),level.getSeaLevel(),mx,mz);
        CoralCathedralFeature.decorate(blocks,terrain,level.getSeed(),level.getSeaLevel(),mx,mz,true);
        wastes(level,terrain,sections,mx,mz);
        return sections;
    }
    private static BlockState get(Map<Integer,LevelChunkSection> sections,int x,int y,int z) {
        var section=sections.get(Math.floorDiv(y,16));
        return section==null?Blocks.AIR.defaultBlockState():section.getBlockState(x&15,y&15,z&15);
    }
    private static void set(Map<Integer,LevelChunkSection> sections,int x,int y,int z,BlockState state) {
        var section=sections.get(Math.floorDiv(y,16));
        if(section!=null)section.setBlockState(x&15,y&15,z&15,state,false);
    }
    /** Same sparse, buried skeleton layout as the dense Wastes feature. */
    private static void wastes(ServerLevel level,ReefTerrain terrain,Map<Integer,LevelChunkSection> sections,int mx,int mz) {
        for(int gx=Math.floorDiv(mx-40,144);gx<=Math.floorDiv(mx+55,144);gx++)
            for(int gz=Math.floorDiv(mz-40,144);gz<=Math.floorDiv(mz+55,144);gz++) {
                long seed=CoralCathedralFeature.seed(level.getSeed()^0xdead5eaL,gx,gz);
                Random r=new Random(seed);
                if(r.nextDouble()>.24)continue;
                int cx=gx*144+72+r.nextInt(25)-12,cz=gz*144+72+r.nextInt(25)-12;
                var sample=terrain.provinceSample(cx,cz);
                if(sample==null || sample.zone()==ReefProvinceLayout.Zone.CATHEDRAL)continue;
                int base=terrain.anchorFloor(cx,cz)-4;
                PlanKey key=new PlanKey(seed,16+r.nextInt(13),r.nextInt(3));
                CoralGeometry.Plan plan;
                synchronized(PLANS) { plan=PLANS.computeIfAbsent(key,k->CoralGeometry.build(k.seed,k.height,k.style)); }
                for(var e:plan.blocks().entrySet()) {
                    var v=e.getKey();int x=cx+v.x(),y=base+v.y(),z=cz+v.z();
                    if(x<mx || x>=mx+16 || z<mz || z>=mz+16 || y>=level.getSeaLevel())continue;
                    var local=terrain.provinceSample(x,z);
                    if(local==null || local.zone()==ReefProvinceLayout.Zone.CATHEDRAL || y<=terrain.floor(x,z))continue;
                    if(get(sections,x,y,z).is(Blocks.WATER))set(sections,x,y,z,e.getValue()<0
                        ?Blocks.SMOOTH_SANDSTONE.defaultBlockState():Blocks.DEAD_BRAIN_CORAL_BLOCK.defaultBlockState());
                }
            }
    }
    private DeepProvinceGenerator() {}
}
