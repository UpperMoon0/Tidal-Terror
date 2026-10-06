package com.nhat.tidal_terror.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Bounded, loaded-chunk-only routes for the reef's small ambient creatures. */
public final class ReefNavigation {
 private ReefNavigation(){}
 public static boolean clear(Mob mob,Vec3 point,boolean grounded){
  var level=mob.level();var box=mob.getBoundingBox().move(point.subtract(mob.position()));
  var feet=BlockPos.containing(point);
  if(!level.hasChunkAt(feet))return false;
  if(grounded&&!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),Direction.UP))return false;
  // Require the full occupied volume to remain underwater, including wide claws.
  for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX+.001,box.minY+.001,box.minZ+.001),
     BlockPos.containing(box.maxX-.001,box.maxY-.001,box.maxZ-.001)))
   if(!level.hasChunkAt(pos)||!level.getFluidState(pos).is(FluidTags.WATER))return false;
  return level.noCollision(mob,box);
 }
 public static boolean move(Mob mob,Vec3 destination,double speed,boolean grounded){
  if(!clear(mob,destination,grounded))return false;
  double offset=(int)(mob.getBbWidth()+1)*.5;
  var path=mob.getNavigation().createPath(BlockPos.containing(destination.add(-offset,0,-offset)),0);
  if(path==null||!path.canReach()||path.getNodeCount()>32)return false;
  for(int i=0;i<path.getNodeCount();i++)if(!clear(mob,path.getEntityPosAtNode(mob,i),grounded))return false;
  return mob.getNavigation().moveTo(path,speed);
 }
 public static Vec3 away(Mob mob,Vec3 danger){
  Vec3 direction=danger==null?new Vec3(1,0,0):mob.position().subtract(danger).multiply(1,0,1);
  return direction.lengthSqr()<.01?new Vec3(1,0,0):direction.normalize();
 }
 public static Vec3 rotate(Vec3 direction,double angle){
  return new Vec3(direction.x*Math.cos(angle)-direction.z*Math.sin(angle),0,
   direction.x*Math.sin(angle)+direction.z*Math.cos(angle));
 }
}
