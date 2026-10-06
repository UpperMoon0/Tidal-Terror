package com.nhat.tidal_terror.worldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import java.util.*;
/** Chunks place only their own slices of deterministic multi-chunk coral plans. */
public final class CoralCathedralFeature extends Feature<NoneFeatureConfiguration>{
 public static final Block[] CORAL={Blocks.TUBE_CORAL_BLOCK,Blocks.BRAIN_CORAL_BLOCK,Blocks.BUBBLE_CORAL_BLOCK,Blocks.FIRE_CORAL_BLOCK,Blocks.HORN_CORAL_BLOCK};
 public static final Block[] FANS={Blocks.TUBE_CORAL_FAN,Blocks.BRAIN_CORAL_FAN,Blocks.BUBBLE_CORAL_FAN,Blocks.FIRE_CORAL_FAN,Blocks.HORN_CORAL_FAN};
 private record Key(long seed,int height,int style){}
 private static final Map<Key,CoralGeometry.Plan> CACHE=new LinkedHashMap<>(64,.75F,true){
  @Override protected boolean removeEldestEntry(Map.Entry<Key,CoralGeometry.Plan> e){return size()>64;}
 };
 public CoralCathedralFeature(){super(NoneFeatureConfiguration.CODEC);}
 public static long seed(long world,int x,int z){
  long n=world^((long)x*341873128712L)^((long)z*132897987541L);
  n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);
 }
 @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
  var level=c.level();var terrain=new ReefTerrain(level,c.chunkGenerator());int mx=c.origin().getX()&~15,mz=c.origin().getZ()&~15;boolean placed=false;
  for(int gx=Math.floorDiv(mx-40,88);gx<=Math.floorDiv(mx+55,88);gx++)for(int gz=Math.floorDiv(mz-40,88);gz<=Math.floorDiv(mz+55,88);gz++){
   long seed=seed(level.getSeed(),gx,gz);Random r=new Random(seed);
   int cx=gx*88+44+r.nextInt(13)-6,cz=gz*88+44+r.nextInt(13)-6;
   if(!terrain.giant(cx,cz))continue;int base=terrain.anchorFloor(cx,cz)+1,style=r.nextInt(3);
   int height=level.getSeaLevel()-base-2-r.nextInt(5);if(Math.floorMod(gx+gz,3)!=0)height-=12+r.nextInt(20);
   Key k=new Key(seed,height,style);CoralGeometry.Plan plan;
   synchronized(CACHE){plan=CACHE.computeIfAbsent(k,key->CoralGeometry.build(key.seed,key.height,key.style));}
   for(var e:plan.blocks().entrySet()){
    var v=e.getKey();int x=cx+v.x(),z=cz+v.z();if(x<mx||x>=mx+16||z<mz||z>=mz+16)continue;
    BlockPos p=new BlockPos(x,base+v.y(),z);if(!level.getBlockState(p).is(Blocks.WATER))continue;
    boolean wet=false,safe=true;
    for(var d:net.minecraft.core.Direction.values()){
     BlockPos next=p.relative(d);
     var local=new CoralGeometry.Voxel(next.getX()-cx,next.getY()-base,next.getZ()-cz);
     if(!plan.blocks().containsKey(local) && level.getFluidState(next).is(net.minecraft.tags.FluidTags.WATER)
         && (next.getY()>-44 || next.getY()>terrain.floor(next.getX(),next.getZ())))wet=true;
     if(level.getBlockState(next).is(net.minecraft.tags.BlockTags.CORAL_BLOCKS)){
      boolean retained=false;
      for(var n:net.minecraft.core.Direction.values()){
       BlockPos water=next.relative(n);
       var candidate=new CoralGeometry.Voxel(water.getX()-cx,water.getY()-base,water.getZ()-cz);
       if(!plan.blocks().containsKey(candidate) && level.getFluidState(water).is(net.minecraft.tags.FluidTags.WATER)
           && (water.getY()>-44 || water.getY()>terrain.floor(water.getX(),water.getZ())))retained=true;
      }
      if(!retained)safe=false;
     }
    }
    if(safe){level.setBlock(p,e.getValue()<0||!wet?Blocks.SMOOTH_SANDSTONE.defaultBlockState():CORAL[e.getValue()].defaultBlockState(),2);placed=true;}
   }
   for(var e:plan.blocks().entrySet()){
    var v=e.getKey();int x=cx+v.x(),z=cz+v.z(),kind=e.getValue();
    if(kind<0 || x<mx || x>=mx+16 || z<mz || z>=mz+16 || Math.floorMod((long)x*31+v.y()*17+z*13,13)!=0)continue;
    BlockPos p=new BlockPos(x,base+v.y(),z),top=p.above();
    if(plan.blocks().containsKey(new CoralGeometry.Voxel(v.x(),v.y()+1,v.z())) || !level.getBlockState(top).is(Blocks.WATER))continue;
    var fan=FANS[kind].defaultBlockState();
    if(fan.canSurvive(level,top))level.setBlock(top,fan,2);
   }
  }
  // Finish the flooded volume after native decoration. Unsupported waterlogged
  // plants return AIR in native neighbour-shape processing; retain their water.
  BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();
  for(int x=mx;x<mx+16;x++)for(int z=mz;z<mz+16;z++){
   if(!terrain.reef(x,z))continue;
   int floor=terrain.floor(x,z);
   for(int y=level.getMinY();y<level.getSeaLevel();y++){
    p.set(x,y,z);var state=level.getBlockState(p);
    boolean invalid=state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)
        && !(state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)
        && (!state.canSurvive(level,p) || net.minecraft.world.level.block.Block.updateFromNeighbourShapes(state,level,p).isAir());
    if(state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.SOUL_SAND))level.setBlock(p,Blocks.SANDSTONE.defaultBlockState(),2);
    else if(y>floor && (state.isAir() || invalid || state.is(Blocks.BUBBLE_COLUMN)))level.setBlock(p,Blocks.WATER.defaultBlockState(),2);
   }
  }
  return placed;
 }
}
