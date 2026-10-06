package com.nhat.tidal_terror.entities.veilglow;

import com.nhat.tidal_terror.entities.ReefNavigation;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import static com.nhat.tidal_terror.entities.veilglow.VeilglowEntity.Behavior.*;

/** Loose blooms, slow coast/rest cycles, and short defensive escape pulses. */
public final class VeilglowDriftGoal extends Goal {
 private final VeilglowEntity jelly;
 private LivingEntity attacker,threat;
 private VeilglowEntity leader;
 private Vec3 danger;
 private int memory,scan,route,phase,quiet,heal;
 public VeilglowDriftGoal(VeilglowEntity jelly){this.jelly=jelly;setFlags(EnumSet.of(Flag.MOVE));}
 @Override public boolean canUse(){return true;}
 @Override public boolean canContinueToUse(){return true;}
 @Override public boolean requiresUpdateEveryTick(){return true;}
 @Override public void stop(){jelly.getNavigation().stop();}
 private boolean active(LivingEntity e){return e!=null&&e.isAlive()&&e.level()==jelly.level()
  &&(!(e instanceof Player p)||!p.isCreative()&&!p.isSpectator());}
 public void startle(LivingEntity source){
  attacker=active(source)?source:null;danger=source==null?null:source.position();memory=140;
  quiet=heal=0;change(FLEE);
 }
 private void change(VeilglowEntity.Behavior state){if(jelly.getBehavior()!=state){
  jelly.setBehavior(state);phase=0;route=0;jelly.getNavigation().stop();
 }}
 @Override public void tick(){
  phase++;if(route>0)route--;if(memory>0)memory--;
  if(--scan<=0){
   scan=20;
   threat=jelly.level().getEntitiesOfClass(CoralCrusherEntity.class,jelly.getBoundingBox().inflate(9),
    s->s.isAlive()&&s.isInWater()&&jelly.distanceToSqr(s)<81&&jelly.hasLineOfSight(s))
    .stream().min(Comparator.comparingDouble(jelly::distanceToSqr)).orElse(null);
   leader=jelly.level().getEntitiesOfClass(VeilglowEntity.class,jelly.getBoundingBox().inflate(7),
    other->other!=jelly&&other.getId()<jelly.getId()&&other.isAlive()&&other.isInWater()
     &&other.getBehavior()!=FLEE&&jelly.distanceToSqr(other)<49).stream()
    .min(Comparator.comparingInt(VeilglowEntity::getId)).orElse(null);
  }
  if(active(attacker)&&memory>0&&jelly.distanceToSqr(attacker)<144)danger=attacker.position();
  if(threat!=null&&(!active(threat)||!threat.isInWater()||jelly.distanceToSqr(threat)>121))threat=null;
  if(threat!=null){danger=threat.position();memory=140;}
  if(!jelly.isInWater()){
   quiet=heal=0;change(RECOVER);jelly.getNavigation().stop();
   // Native suffocation still applies. A stranded animal only flops toward nearby clear water.
   if(route==0){route=40;
    for(int i=0;i<12;i++){
     Vec3 p=jelly.position().add(jelly.getRandom().nextInt(7)-3,0,jelly.getRandom().nextInt(7)-3);
     if(ReefNavigation.clear(jelly,p,false)&&jelly.onGround()){
      Vec3 d=p.subtract(jelly.position()).multiply(1,0,1).normalize();
      jelly.setDeltaMovement(jelly.getDeltaMovement().add(d.scale(.12)).add(0,.18,0));break;
     }
    }
   }return;
  }
  if(memory>0){
   quiet=heal=0;change(FLEE);
   if(route==0){route=30;Vec3 d=ReefNavigation.away(jelly,danger);
    for(int i=0;i<8;i++){
     Vec3 p=jelly.position().add(ReefNavigation.rotate(d,(i%2==0?1:-1)*(i/2)*.3).scale(4)).add(0,i%3-1,0);
     if((danger==null||p.distanceToSqr(danger)>jelly.position().distanceToSqr(danger))
       &&ReefNavigation.move(jelly,p,1.25,false))break;
    }
   }return;
  }
  attacker=null;danger=null;
  if(++quiet>260&&++heal>=100){jelly.heal(1);heal=0;}
  if(jelly.getBehavior()==FLEE||jelly.getBehavior()==RECOVER)change(DRIFT);
  // Every fourth second includes a short active contraction, followed by a coast/rest.
  boolean pulse=(jelly.tickCount+jelly.getId()*13)%140<35;
  if(leader!=null&&(!active(leader)||!leader.isInWater()||leader.getBehavior()==FLEE||jelly.distanceToSqr(leader)>64))leader=null;
  change(pulse?PULSE:leader!=null?BLOOM:DRIFT);
  if(route>0||!jelly.getNavigation().isDone())return;
  route=40;
  // Drift pauses avoid constant swimming; the next pulse restarts locomotion.
  if(!pulse&&leader==null&&(jelly.tickCount+jelly.getId()*13)%140>=105)return;
  for(int i=0;i<8;i++){
   Vec3 p;
   if(leader!=null){
    double angle=jelly.getId()*2.4+i*.3;
    p=leader.position().add(Math.cos(angle)*3.5,((jelly.getId()%3)-1)*.6,Math.sin(angle)*3.5);
   }else p=jelly.position().add(jelly.getRandom().nextInt(9)-4,pulse?1:jelly.getRandom().nextInt(3)-1,jelly.getRandom().nextInt(9)-4);
   if(p.distanceToSqr(jelly.position())>1&&ReefNavigation.move(jelly,p,pulse?.75:.4,false))break;
  }
 }
}
