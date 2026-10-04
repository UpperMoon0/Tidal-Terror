package com.nhat.tidal_terror.entities.shardback;

import com.nhat.tidal_terror.entities.ReefNavigation;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import static com.nhat.tidal_terror.entities.shardback.ShardbackEntity.Behavior.*;

/** One movement owner: sediment browsing, feeding pauses, warnings and cover. */
public final class ShardbackForageGoal extends Goal {
 private final ShardbackEntity crab;
 private LivingEntity attacker,shark;
 private Player visitor;
 private Vec3 danger;
 private int memory,scan,route,phase,quiet,heal;
 private boolean reachedCover;
 public ShardbackForageGoal(ShardbackEntity crab){this.crab=crab;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 @Override public boolean canUse(){return true;}
 @Override public boolean canContinueToUse(){return true;}
 @Override public boolean requiresUpdateEveryTick(){return true;}
 @Override public void stop(){crab.getNavigation().stop();}
 private boolean active(LivingEntity e){return e!=null&&e.isAlive()&&e.level()==crab.level()
  &&(!(e instanceof Player p)||!p.isCreative()&&!p.isSpectator());}
 public void startle(LivingEntity source){attacker=active(source)?source:null;danger=source==null?null:source.position();
  memory=180;quiet=heal=0;reachedCover=false;change(FLEE);}
 private void change(ShardbackEntity.Behavior state){if(crab.getBehavior()!=state){
  crab.setBehavior(state);phase=0;route=0;crab.getNavigation().stop();
 }}
 private Vec3 seabed(Vec3 near){
  BlockPos p=BlockPos.containing(near).above(2);
  for(int i=0;i<6;i++,p=p.below()){
   Vec3 point=Vec3.atBottomCenterOf(p);
   if(ReefNavigation.clear(crab,point,true))return point;
  }return null;
 }
 private int cover(Vec3 p){
  BlockPos pos=BlockPos.containing(p);int value=0;
  for(Direction d:Direction.Plane.HORIZONTAL)if(crab.level().getBlockState(pos.relative(d,2)).isSolid())value++;
  return value;
 }
 @Override public void tick(){
  phase++;if(route>0)route--;if(memory>0)memory--;
  if(--scan<=0){
   scan=20;
   shark=crab.level().getEntitiesOfClass(CoralCrusherEntity.class,crab.getBoundingBox().inflate(10),
    s->s.isAlive()&&s.isInWater()&&crab.distanceToSqr(s)<100&&crab.hasLineOfSight(s))
    .stream().min(Comparator.comparingDouble(crab::distanceToSqr)).orElse(null);
   visitor=crab.level().getEntitiesOfClass(Player.class,crab.getBoundingBox().inflate(3),
    p->active(p)&&p.isInWater()&&crab.distanceToSqr(p)<9&&crab.hasLineOfSight(p))
    .stream().min(Comparator.comparingDouble(crab::distanceToSqr)).orElse(null);
  }
  if(shark!=null&&(!active(shark)||!shark.isInWater()||crab.distanceToSqr(shark)>144))shark=null;
  if(shark!=null){danger=shark.position();memory=180;quiet=heal=0;}
  if(active(attacker)&&memory>0&&crab.distanceToSqr(attacker)<225)danger=attacker.position();
  if(!crab.isInWater()){
   quiet=heal=0;change(RECOVER);
   if(route==0){route=40;
    for(int i=0;i<12;i++){
     Vec3 p=seabed(crab.position().add(crab.getRandom().nextInt(11)-5,0,crab.getRandom().nextInt(11)-5));
     // Amphibious navigation can cross land while seeking water.
     if(p!=null&&crab.getNavigation().moveTo(p.x,p.y,p.z,.9))break;
    }
   }return;
  }
  if(memory>0){
   quiet=heal=0;
   if(reachedCover&&crab.getNavigation().isDone()&&(danger==null||crab.position().distanceToSqr(danger)>16)){change(SHELTER);return;}
   if(reachedCover&&crab.getNavigation().isDone())reachedCover=false;
   change(FLEE);
   if(route==0){route=30;Vec3 d=ReefNavigation.away(crab,danger);var candidates=new ArrayList<Vec3>();
    for(int i=0;i<10;i++){
     Vec3 p=seabed(crab.position().add(ReefNavigation.rotate(d,(i%2==0?1:-1)*(i/2)*.3).scale(4+i%2)));
     if(p!=null&&(danger==null||p.distanceToSqr(danger)>crab.position().distanceToSqr(danger)))candidates.add(p);
    }
    candidates.sort(Comparator.comparingInt(this::cover).reversed());
    for(var p:candidates)if(ReefNavigation.move(crab,p,1.2,true)){reachedCover=cover(p)>0;break;}
   }return;
  }
  attacker=null;danger=null;reachedCover=false;
  if(visitor!=null&&(!active(visitor)||!visitor.isInWater()||crab.distanceToSqr(visitor)>9||!crab.hasLineOfSight(visitor)))visitor=null;
  if(visitor!=null){
   quiet=heal=0;change(THREATEN);crab.getLookControl().setLookAt(visitor,30,20);
   if(phase>=60){startle(visitor);visitor=null;}return;
  }
  if(++quiet>300&&crab.getBehavior()==FORAGE&&++heal>=100){crab.heal(1);heal=0;}
  if(crab.getBehavior()==FORAGE&&phase<80)return;
  if(crab.getBehavior()==FORAGE){change(WANDER);route=20;return;}
  if(crab.getBehavior()!=WANDER)change(WANDER);
  if(!crab.getNavigation().isDone()||route>0)return;
  // Reaching a sediment patch starts a visible, non-destructive feeding pause.
  if(phase>30&&ShardbackEntity.isSeabed(crab.level().getBlockState(crab.blockPosition().below()))){change(FORAGE);return;}
  route=40;
  for(int i=0;i<10;i++){
   Vec3 p=seabed(crab.position().add(crab.getRandom().nextInt(11)-5,0,crab.getRandom().nextInt(11)-5));
   if(p!=null&&p.distanceToSqr(crab.position())>4&&ReefNavigation.move(crab,p,.65,true))break;
  }
 }
}
