package com.nhat.tidal_terror.worldgen;

import net.minecraft.core.*;
import net.minecraft.tags.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import java.util.*;

/** Clustered colonies and lush native plants; each chunk writes only its own slice. */
public final class ReefGardenFeature extends Feature<NoneFeatureConfiguration> {
    private static final Block[] PLANTS={Blocks.TUBE_CORAL,Blocks.BRAIN_CORAL,Blocks.BUBBLE_CORAL,Blocks.FIRE_CORAL,Blocks.HORN_CORAL};
    public ReefGardenFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
        var level=c.level(); var terrain=new ReefTerrain(level,c.chunkGenerator());
        int mx=c.origin().getX()&~15,mz=c.origin().getZ()&~15;
        long worldSeed=level.getSeed();
        // Include neighbouring anchors, reconstruct whole shapes, write only our
        // slice. Structures no longer stop or re-centre at every 16-block seam.
        for(int x=mx-7;x<mx+23;x++)for(int z=mz-7;z<mz+23;z++) {
            var colony=ReefGardenLayout.colony(worldSeed,x,z);
            if(colony==null || !terrain.reef(x,z))continue;
            int base=terrain.anchorFloor(x,z)+1;
            int h=Math.min(colony.height(),level.getSeaLevel()-base-3);
            if(h<2)continue;
            var r=new Random(colony.seed());
            double aspect=.7+r.nextDouble()*.7, radius=2.4+r.nextDouble()*2.0;
            Map<BlockPos,Integer> shape=new HashMap<>();
            for(int y=0;y<h;y++)for(int dx=-7;dx<=7;dx++)for(int dz=-7;dz<=7;dz++) {
                double u=(dx*Math.cos(colony.yaw())+dz*Math.sin(colony.yaw()))/aspect;
                double v=-dx*Math.sin(colony.yaw())+dz*Math.cos(colony.yaw());
                double a=Math.atan2(v,u), distance=Math.hypot(u,v), twist=colony.yaw()+y*.13;
                double reach=1.1+y*.23;
                boolean hit;
                if(colony.rock())hit=distance<=radius*Math.sqrt(Math.max(0,1-y*y/(double)(h*h)))*(1+.12*Math.sin(a*3+twist));
                else if(colony.form()==0)hit=Math.abs(distance-reach-.7*Math.sin(a*5+twist))<.7;
                else if(colony.form()==1) {
                    hit=distance<1.15;
                    for(int arm=0;arm<4;arm++) {
                        double angle=arm*Math.PI/2+twist, extent=y/(double)h*radius;
                        if(Math.hypot(u-Math.cos(angle)*extent,v-Math.sin(angle)*extent)<.9)hit=true;
                    }
                } else hit=distance<1.15 || (y+Math.floorMod(colony.seed(),3))%3==2 && distance<reach+.55*Math.sin(a*5+twist);
                if(hit)shape.put(new BlockPos(x+dx,base+y,z+dz),colony.rock()?-1:colony.color());
            }
            for(var e:shape.entrySet()) {
                BlockPos p=e.getKey();
                if(!own(p,mx,mz) || !terrain.reef(p.getX(),p.getZ()) || !level.getBlockState(p).is(Blocks.WATER))continue;
                boolean wet=false,safe=true;
                for(Direction d:Direction.values()) {
                    BlockPos neighbour=p.relative(d);
                    if(!shape.containsKey(neighbour)&&level.getFluidState(neighbour).is(FluidTags.WATER))wet=true;
                    if(!level.getBlockState(neighbour).is(BlockTags.CORAL_BLOCKS))continue;
                    boolean retained=false;
                    for(Direction a:Direction.values())if(!shape.containsKey(neighbour.relative(a))&&level.getFluidState(neighbour.relative(a)).is(FluidTags.WATER))retained=true;
                    if(!retained)safe=false;
                }
                if(safe)level.setBlock(p,e.getValue()<0||!wet?Blocks.SANDSTONE.defaultBlockState():CoralCathedralFeature.CORAL[e.getValue()].defaultBlockState(),2);
            }
            for(BlockPos p:shape.keySet())if(own(p,mx,mz)&&r.nextInt(3)==0&&level.getBlockState(p.above()).is(Blocks.WATER)) {
                var decoration=r.nextInt(4)==0?PLANTS[colony.color()].defaultBlockState():CoralCathedralFeature.FANS[colony.color()].defaultBlockState();
                if(decoration.canSurvive(level,p.above()))level.setBlock(p.above(),decoration,2);
            }
        }
        for(int x=mx;x<mx+16;x++)for(int z=mz;z<mz+16;z++) {
            if(!terrain.reef(x,z))continue;
            var r=new Random(CoralCathedralFeature.seed(worldSeed ^ 0x736fabL,x,z));
            if(r.nextDouble()>ReefGardenLayout.decorationChance(worldSeed,x,z))continue;
            BlockPos p=new BlockPos(x,terrain.floor(x,z)+1,z);
            if(!level.getBlockState(p).is(Blocks.WATER))continue;
            int color=(int)Math.floorMod(CoralCathedralFeature.seed(worldSeed,Math.floorDiv(x,11),Math.floorDiv(z,11)),5);
            int kind=r.nextInt(10);
            var decoration=kind<2?Blocks.SEAGRASS.defaultBlockState():kind<6?PLANTS[color].defaultBlockState():kind<9?CoralCathedralFeature.FANS[color].defaultBlockState():
                    Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES,1+r.nextInt(4));
            if(decoration.canSurvive(level,p))level.setBlock(p,decoration,2);
        }
        return true;
    }
    private static boolean own(BlockPos p,int mx,int mz){return p.getX()>=mx&&p.getX()<mx+16&&p.getZ()>=mz&&p.getZ()<mz+16;}
}
