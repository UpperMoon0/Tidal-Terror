package com.nhat.tidal_terror.preview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
/** Opt-in explorer: imports a completed audit world, then gives all control to the player. */
@Mod.EventBusSubscriber(modid="tidalterror",value=Dist.CLIENT)
public final class ReefExplorer {
 private static boolean importing,opened,finished;
 private static CompletableFuture<Void> copy;
 private static final Minecraft MC=Minecraft.getInstance();
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||finished)return;
  try{
   Path project=Path.of(System.getProperty("tidalterror.originalProject"));
   Path location=project.resolve("build/reef-audit-v4/world-path.txt");
   if(!Files.exists(location))return;
   Path audited=Path.of(Files.readString(location).trim());
   Path target=Path.of("saves/Coral Cathedral Deep Reef");
   if(!opened && MC.level!=null){finished=true;return;}
   if(!importing && MC.getOverlay()==null && Files.exists(project.resolve("build/reef-audit-v4/passed.txt"))){
    // Native session.lock must be released before importing this independent copy.
    try(var channel=FileChannel.open(audited.resolve("session.lock"),StandardOpenOption.WRITE);var lock=channel.tryLock()){
     if(lock==null)return;
    }catch(Exception busy){return;}
    importing=true;
    copy=CompletableFuture.runAsync(()->{
     try(var paths=Files.walk(audited)){
      for(Path source:paths.toList()){
       Path relative=audited.relativize(source);if(relative.toString().equals("session.lock"))continue;
       Path destination=target.resolve(relative);
       if(Files.isDirectory(source))Files.createDirectories(destination);
       else Files.copy(source,destination,StandardCopyOption.REPLACE_EXISTING);
      }
     }catch(Exception error){throw new RuntimeException(error);}
    });
   }
   if(!opened && importing && copy.isDone()){
    copy.join();opened=true;
    MC.options.pauseOnLostFocus=false;MC.options.hideGui=false;MC.options.renderDistance().set(12);
    MC.options.simulationDistance().set(6);MC.options.fov().set(75);MC.options.framerateLimit().set(45);
    MC.options.cloudStatus().set(CloudStatus.OFF);MC.options.setCameraType(CameraType.FIRST_PERSON);
    MC.createWorldOpenFlows().loadLevel(null,"Coral Cathedral Deep Reef");return;
   }
   if(opened && MC.level==null && MC.screen!=null)for(var widget:MC.screen.children())if(widget instanceof Button b){
    String label=b.getMessage().getString().toLowerCase(Locale.ROOT);
    if(label.contains("proceed")||label.contains("i know what")){b.onPress();break;}
   }
   if(opened && MC.player!=null && MC.getSingleplayerServer()!=null){
    finished=true;
    MC.getWindow().setTitle("Tidal Terror - corrected deep reef");
    var server=MC.getSingleplayerServer();var uuid=MC.player.getUUID();
    server.execute(()->{
     var level=server.overworld();var p=server.getPlayerList().getPlayer(uuid);
     level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);
     level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
     p.setGameMode(GameType.CREATIVE);p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,100000,0,false,false));
     p.teleportTo(level,0,-15,-35,-35,-12);p.getAbilities().flying=true;p.onUpdateAbilities();
     // The audit deliberately accelerates spawning. Start this independent
     // exploration copy with a normal-looking population and one native shark.
     for(var entity:java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).filter(Objects::nonNull).toList())
      if(entity.getType().getCategory()==net.minecraft.world.entity.MobCategory.WATER_CREATURE ||
          entity.getType()==net.minecraft.world.entity.EntityType.TURTLE ||
          entity.getType()==net.minecraft.world.entity.EntityType.TROPICAL_FISH)entity.discard();
     boolean shark=false;
     for(int attempt=0;attempt<200 && !shark;attempt++){
      var pos=new net.minecraft.core.BlockPos(36+level.random.nextInt(45),-20+level.random.nextInt(40),12+level.random.nextInt(45));
      net.minecraft.world.level.NaturalSpawner.spawnCategoryForPosition(net.minecraft.world.entity.MobCategory.WATER_CREATURE,level,pos);
      for(var entity:level.getAllEntities())if(entity instanceof com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity reefShark){
       reefShark.setPersistenceRequired();shark=true;break;
      }
     }
     System.out.println("REEF_EXPLORER nativeShark="+shark);
     System.out.println("REEF_EXPLORER READY independent save; controls returned to player");
    });
   }
  }catch(Throwable error){finished=true;error.printStackTrace();System.out.println("REEF_EXPLORER failed automatic import; client remains available");}
 }
}
