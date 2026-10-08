package com.nhat.tidal_terror.worldgen;

import com.google.gson.*;
import com.nstut.endless.heights.EndlessHeights;
import com.nstut.endless.vertical.EndlessVerticalEngine;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Native generation plus cold saved-page validation, in an isolated fresh preset. */
@Mod.EventBusSubscriber(modid="tidalterror")
public final class DeepProvinceAudit {
    private static net.minecraft.server.MinecraftServer server;
    private static ReefProvinceLayout.Center center;
    private static final List<BlockPos> points=new ArrayList<>();
    private static boolean ready,done;private static int ticks;
    private static final boolean RELOAD=Boolean.getBoolean("tidalterror.deepReload");
    private static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
    @SubscribeEvent public static void start(ServerStartedEvent event) {
        server=event.getServer();
        try {
            require(ModList.get().isLoaded("endless"),"Endless required");
            require(!ModList.get().isLoaded("terrablender"),"Unexpected TerraBlender runtime");
            var level=server.overworld();var generator=level.getChunkSource().getGenerator();
            require(generator.getBiomeSource() instanceof ReefProvinceBiomeSource source && source.deep(),"Wrong deep preset");
            require(EndlessHeights.getDenseMinBuildHeight()==-64 && level.getSectionsCount()==24,"Dense core widened");
            require(EndlessHeights.getMinBuildHeight()==-1024,"Logical min differs from fixture");
            var source=(ReefProvinceBiomeSource)generator.getBiomeSource();
            var sampler=level.getChunkSource().randomState().sampler();
            if(RELOAD) {
                var checkpoint=JsonParser.parseString(Files.readString(Path.of("checkpoint.json"))).getAsJsonObject();
                center=new ReefProvinceLayout.Center(checkpoint.get("x").getAsInt(),checkpoint.get("z").getAsInt());
            } else {
                outer:for(int r=0;r<=30;r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++) {
                    if(Math.max(Math.abs(x),Math.abs(z))!=r)continue;
                    var c=ReefProvinceLayout.center(level.getSeed(),x,z);
                    if(source.province(level.getSeed(),c.x(),c.z(),sampler)!=null) { center=c;break outer; }
                }
            }
            require(center!=null,"No bounded fixture province");
            System.out.println("DEEP_AUDIT center="+center+" reload="+RELOAD);
            var terrain=new ReefTerrain(level,generator);
            verifyLandmarks(level,terrain);
            int[] offsets={0,230,340,500};
            for(int i=0;i<offsets.length;i++) {
                int x=center.x()+(int)Math.round(offsets[i]*ReefProvinceLayout.SCALE),z=center.z();
                int floor=terrain.floor(x,z);BlockPos point=new BlockPos(x,floor+1,z);points.add(point);
                level.setChunkForced(x>>4,z>>4,true);
                var chunk=level.getChunk(x>>4,z>>4);
                require(chunk.getSections().length==24,"Chunk arrays widened");
                require(chunk.getBlockState(point.below()).is(Blocks.SAND),"Deep sediment missing");
                int bedrock=ReefProvinceLayout.bedrockBase(floor);
                require(chunk.getBlockState(new BlockPos(x,bedrock,z)).is(Blocks.BEDROCK),"Deep bedrock sheet missing");
                if(floor < -64) {
                    require(!chunk.getBlockState(new BlockPos(x,-64,z)).is(Blocks.BEDROCK),"Vanilla bedrock barrier remains");
                    require(chunk.getBlockState(new BlockPos(x,-64,z)).is(Blocks.WATER),"Dense/sparse water junction missing");
                    require(chunk.getBlockState(new BlockPos(x,-65,z)).is(Blocks.WATER),"Sparse junction missing");
                    require(chunk.getBlockState(new BlockPos(x,bedrock-8,z)).is(Blocks.DEEPSLATE),"Rock under deep bedrock missing");
                    require(level.getBiome(new BlockPos(x,floor+10,z)).is(i==0?ReefWorldgen.BIOME:ReefWorldgen.WASTES),"Sparse biome mismatch");
                    require(level.getHeight(Heightmap.Types.OCEAN_FLOOR,x,z)< -64,"Heightmap clamped to dense floor");
                } else {
                    require(chunk.getBlockState(new BlockPos(x,-65,z)).is(Blocks.DEEPSLATE),"Solid valley wall missing below ordinary floor");
                }
                System.out.println("DEEP_AUDIT zone="+i+" floor="+floor+" bedrock="+bedrock);
            }
            // Ordinary terrain outside the province retains its vanilla floor and no sparse page.
            var outside=level.getChunk(0,0);
            require(source.province(level.getSeed(),0,0,sampler)==null,"Outside control lies in province");
            require(outside.getBlockState(new BlockPos(0,-64,0)).is(Blocks.BEDROCK),"Outside vanilla bedrock changed");
            require(EndlessVerticalEngine.world(level).knownPageYs(0,0).isEmpty(),"Outside terrain allocated sparse pages");
            BlockPos edit=points.get(2).above(9);
            if(RELOAD) require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Cold restart lost or regenerated player edit");
            else require(level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),2),"Sparse player edit failed");
            com.nstut.endless.testing.NativeSectionLightChecks.run(level,new BlockPos(111,-200,111),false);
            com.nstut.endless.testing.NativeSectionLightChecks.run(level,new BlockPos(111,-63,111),true);
            var vertical=EndlessVerticalEngine.world(level);
            var pagePos=new com.nstut.endless.vertical.VerticalPagePos(points.get(0).getX()>>4,-1,points.get(0).getZ()>>4);
            var duplicate=new com.nstut.endless.vertical.VerticalPage<net.minecraft.world.level.chunk.LevelChunkSection>(-1);
            var section=new net.minecraft.world.level.chunk.LevelChunkSection(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME));
            section.setBlockState(0,0,0,Blocks.STONE.defaultBlockState());duplicate.putSection(-20,section);
            boolean refused=false;
            try { vertical.installGeneratedPage(pagePos,duplicate); } catch(IllegalStateException expected) { refused=true; }
            require(refused,"Bulk admission overwrote an existing page");
            require(level.getBlockState(points.get(0).below()).is(Blocks.SAND),"Duplicate admission damaged existing terrain");
            require(DeepProvinceGenerator.admissionBuilds()==0,"Deep sections built on admission instead of workers");
            System.out.println("DEEP_ADMISSION_WORKER_PASS builds="+DeepProvinceGenerator.admissionBuilds());
            ready=true;
        } catch(Throwable error) { finish(error); }
    }
    private static void verifyLandmarks(net.minecraft.server.level.ServerLevel level,ReefTerrain terrain) {
        int gx=Math.floorDiv(center.x(),88),gz=Math.floorDiv(center.z(),88);
        long seed=CoralCathedralFeature.seed(level.getSeed(),gx,gz);Random r=new Random(seed);
        int cx=gx*88+44+r.nextInt(13)-6,cz=gz*88+44+r.nextInt(13)-6;
        int base=terrain.anchorFloor(cx,cz)+1,style=r.nextInt(3);
        int height=Math.min(level.getSeaLevel()-base-2-r.nextInt(5),110-r.nextInt(5));
        if(Math.floorMod(gx+gz,3)!=0)height-=12+r.nextInt(20);
        var plan=CoralGeometry.build(seed,height,style);
        var tip=plan.blocks().entrySet().stream().filter(e->e.getValue()>=0)
            .max(Comparator.comparingInt(e->e.getKey().y())).orElseThrow();
        BlockPos coral=new BlockPos(cx+tip.getKey().x(),base+tip.getKey().y(),cz+tip.getKey().z());
        require(coral.getY()<-64,"Coral witness is not sparse");
        level.getChunk(coral.getX()>>4,coral.getZ()>>4);
        require(level.getBlockState(coral).is(CoralCathedralFeature.CORAL[tip.getValue()]),"Deep Cathedral coral missing");
        int innerX=center.x()+(int)Math.round(340*ReefProvinceLayout.SCALE);
        int ax=Math.floorDiv(innerX,144),az=Math.floorDiv(center.z(),144);
        for(int x=ax-4;x<=ax+4;x++)for(int z=az-4;z<=az+4;z++) {
            seed=CoralCathedralFeature.seed(level.getSeed()^0xdead5eaL,x,z);r=new Random(seed);
            if(r.nextDouble()>.24)continue;
            cx=x*144+72+r.nextInt(25)-12;cz=z*144+72+r.nextInt(25)-12;
            var sample=terrain.provinceSample(cx,cz);
            if(sample==null || sample.zone()==ReefProvinceLayout.Zone.CATHEDRAL)continue;
            base=terrain.anchorFloor(cx,cz)-4;
            plan=CoralGeometry.build(seed,16+r.nextInt(13),r.nextInt(3));
            tip=plan.blocks().entrySet().stream().filter(e->e.getValue()>=0)
                .max(Comparator.comparingInt(e->e.getKey().y())).orElseThrow();
            BlockPos skeleton=new BlockPos(cx+tip.getKey().x(),base+tip.getKey().y(),cz+tip.getKey().z());
            if(skeleton.getY()>=-64 || skeleton.getY()<=terrain.floor(skeleton.getX(),skeleton.getZ()))continue;
            level.getChunk(skeleton.getX()>>4,skeleton.getZ()>>4);
            require(level.getBlockState(skeleton).is(Blocks.DEAD_BRAIN_CORAL_BLOCK),"Deep Wastes skeleton missing");
            System.out.println("DEEP_AUDIT sparse landmarks coral="+coral+" skeleton="+skeleton);
            return;
        }
        throw new AssertionError("No bounded deep Wastes landmark witness");
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !ready || done || ++ticks<200)return;
        try {
            var level=server.overworld();var terrain=new ReefTerrain(level,level.getChunkSource().getGenerator());
            for(var point:points) {
                var chunk=level.getChunk(point.getX()>>4,point.getZ()>>4);
                require(chunk.getFullStatus().isOrAfter(net.minecraft.server.level.FullChunkStatus.BLOCK_TICKING),"Fixture never reached block ticking");
                for(int dx=-16;dx<=16;dx+=4)for(int dz=-16;dz<=16;dz+=4) {
                    int x=point.getX()+dx,z=point.getZ()+dz,floor=terrain.floor(x,z);
                    int base=ReefProvinceLayout.bedrockBase(floor);
                    require(level.getBlockState(new BlockPos(x,base,z)).is(Blocks.BEDROCK),"Bedrock discontinuity at chunk edge");
                    for(int y=floor+1;y<level.getSeaLevel();y++) {
                        var state=level.getBlockState(new BlockPos(x,y,z));
                        require(!state.isAir() && !state.is(Blocks.BUBBLE_COLUMN) && !state.is(Blocks.MAGMA_BLOCK),"Ticked deep water gap at "+x+","+y+","+z);
                    }
                }
            }
            if(!RELOAD) {
                JsonObject checkpoint=new JsonObject();checkpoint.addProperty("x",center.x());checkpoint.addProperty("z",center.z());
                Files.writeString(Path.of("checkpoint.json"),checkpoint.toString());
            }
            EndlessVerticalEngine.flushAll();
            server.saveEverything(false,true,true);
            Files.writeString(Path.of("passed.txt"),"Deep native generation, ordinary bedrock, sparse/dense continuity, active ticking, "+(RELOAD?"cold persisted edit":"initial sparse edit")+" passed\n");
            System.out.println("DEEP_AUDIT "+(RELOAD?"COLD_RELOAD":"FRESH_GENERATION")+" PASSED");
            finish(null);
        } catch(Throwable error) { finish(error); }
    }
    private static void finish(Throwable error) {
        if(done)return;done=true;
        if(error!=null) {
            error.printStackTrace();
            try { Files.writeString(Path.of("failure.txt"),error.toString()); } catch(Exception ignored) {}
        }
        server.halt(false);
    }
}
