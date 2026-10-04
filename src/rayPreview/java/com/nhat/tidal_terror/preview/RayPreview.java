package com.nhat.tidal_terror.preview;

import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.cathedral_ray.CathedralRayEntity;
import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Isolated real framebuffer and native spawn audit. Never included in releases. */
@Mod.EventBusSubscriber(modid="tidalterror",value=Dist.CLIENT)
public final class RayPreview {
    private static final Minecraft MC=Minecraft.getInstance();
    private static final Path OUT=Path.of(System.getProperty("tidalterror.rayOutput", "../../art/cathedral-ray/runtime"));
    private static boolean opened,setup,done,inventory;
    private static CompletableFuture<Void> future;
    private static volatile UUID rayId;
    private static Vec3 center;
    private static int ticks,index,frames;
    private static long cameraAt;
    private static final long START=System.nanoTime();
    private static final List<String> records=new ArrayList<>();
    private static final Vec3[] EYES={new Vec3(0,6,-.1),new Vec3(0,.8,6),new Vec3(6,.8,0),new Vec3(0,-5,-.1),new Vec3(5,3,5)};
    private static final String[] NAMES={"01-top","02-front","03-side","04-belly","05-reef"};

    private static boolean openWater(net.minecraft.server.level.ServerLevel level,BlockPos p) {
        if(!level.getBiome(p).is(ReefWorldgen.BIOME))return false;
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=-6;y<=7;y++)
            if(!level.getBlockState(p.offset(x,y,z)).is(Blocks.WATER))return false;
        return true;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||done)return;
        try {
            if(System.nanoTime()-START>12L*60*1_000_000_000L)throw new IllegalStateException("Ray preview timeout");
            if(!opened && MC.getOverlay()==null) {
                opened=true;Files.createDirectories(OUT);
                MC.options.pauseOnLostFocus=false;MC.options.hideGui=true;
                MC.options.bobView().set(false);MC.options.fov().set(55);
                MC.options.renderDistance().set(8);MC.options.simulationDistance().set(6);
                MC.options.framerateLimit().set(45);MC.options.enableVsync().set(false);
                if(MC.getWindow().isFullscreen())MC.getWindow().toggleFullScreen();
                MC.getWindow().setWindowed(1280,720);
                org.lwjgl.glfw.GLFW.glfwHideWindow(MC.getWindow().getWindow());
                MC.createWorldOpenFlows().loadLevel(null,"Ray Preview");return;
            }
            if(MC.level==null || MC.player==null || MC.getSingleplayerServer()==null) {
                if(opened && MC.screen!=null)for(var widget:MC.screen.children())if(widget instanceof Button b){
                    String text=b.getMessage().getString().toLowerCase(Locale.ROOT);
                    if(text.contains("proceed")||text.contains("i know what")){b.onPress();break;}
                }
                return;
            }
            if(!setup) {
                setup=true;var server=MC.getSingleplayerServer();var uuid=MC.player.getUUID();
                future=CompletableFuture.runAsync(()->{
                    var level=server.overworld();var player=server.getPlayerList().getPlayer(uuid);
                    level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
                    player.setGameMode(GameType.CREATIVE);player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,100000,0,false,false));
                    player.teleportTo(level,-45,5,-35,0,0);player.setNoGravity(true);player.getAbilities().flying=true;player.onUpdateAbilities();
                },server);return;
            }
            if(!future.isDone())return;future.join();ticks++;
            if(rayId==null) {
                if(ticks==80)future=CompletableFuture.runAsync(()->{
                    var level=MC.getSingleplayerServer().overworld();
                    // Remove the source audit's accelerated fauna population in this copy.
                    for(var entity:java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).toList())
                        if((com.nhat.tidal_terror.entities.ModEntities.reefPools().contains(entity.getType().getCategory()) || entity.getType().getCategory()==MobCategory.WATER_CREATURE)
                                || entity.getType().getCategory()==MobCategory.WATER_AMBIENT
                                || entity.getType().getCategory()==MobCategory.UNDERGROUND_WATER_CREATURE)entity.discard();
                    var candidates=new ArrayList<BlockPos>();
                    for(int x=-16;x<=64;x+=8)for(int z=-16;z<=64;z+=8)for(int y=-20;y<=35;y+=10)
                        if(openWater(level,new BlockPos(x,y,z)))candidates.add(new BlockPos(x,y,z));
                    System.out.println("RAY_PREVIEW open candidates="+candidates.size());
                    for(int attempt=0;attempt<1200 && rayId==null && !candidates.isEmpty();attempt++) {
                        var p=candidates.get(level.random.nextInt(candidates.size()));
                        NaturalSpawner.spawnCategoryForPosition(com.nhat.tidal_terror.entities.ModEntities.RAY_POOL,level,p);
                        for(var entity:level.getAllEntities())if(entity instanceof CathedralRayEntity ray && openWater(level,ray.blockPosition())) {
                            ray.setPersistenceRequired();ray.setNoAi(true);ray.setNoGravity(true);ray.setDeltaMovement(Vec3.ZERO);
                            ray.setYRot(0);ray.setYBodyRot(0);ray.setYHeadRot(0);ray.setXRot(0);
                            rayId=ray.getUUID();System.out.println("RAY_PREVIEW NATURAL PASS uuid="+rayId+" biome="+level.getBiome(ray.blockPosition()).unwrapKey()+" position="+ray.position());break;
                        }
                    }
                    if(rayId==null)throw new IllegalStateException("Native ray spawning failed");
                    // Move into entity tracking range before waiting for its client copy.
                    var ray=level.getEntity(rayId);
                    for(var entity:java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).toList())
                        if(!entity.getUUID().equals(rayId) && ((com.nhat.tidal_terror.entities.ModEntities.reefPools().contains(entity.getType().getCategory()) || entity.getType().getCategory()==MobCategory.WATER_CREATURE)
                                || entity.getType().getCategory()==MobCategory.WATER_AMBIENT
                                || entity.getType().getCategory()==MobCategory.UNDERGROUND_WATER_CREATURE))entity.discard();
                    var player=MC.getSingleplayerServer().getPlayerList().getPlayer(MC.player.getUUID());
                    player.teleportTo(level,ray.getX(),ray.getY()+1,ray.getZ()-6,0,0);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,MC.getSingleplayerServer());
                },MC.getSingleplayerServer());
                return;
            }
            CathedralRayEntity ray=null;
            for(var entity:MC.level.entitiesForRendering())if(entity.getUUID().equals(rayId))ray=(CathedralRayEntity)entity;
            if(ray==null)return;
            center=ray.position().add(0,.2,0);
            if(index>=EYES.length) {
                if(!inventory) {
                    inventory=true;MC.options.hideGui=false;
                    var screen=new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(MC.player,MC.level.enabledFeatures(),true);
                    MC.setScreen(screen);var tab=TidalTerror.TIDAL_TERROR_TAB.get();
                    var select=screen.getClass().getDeclaredMethod("selectTab",net.minecraft.world.item.CreativeModeTab.class);
                    select.setAccessible(true);select.invoke(screen,tab);
                    if(tab.getDisplayItems().size()!=4 || tab.getDisplayItems().stream().noneMatch(s->s.is(TidalTerror.CATHEDRAL_RAY_SPAWN_EGG.get())))
                        throw new IllegalStateException("Missing ray egg from native creative tab");
                    cameraAt=System.nanoTime();frames=0;
                }
                return;
            }
            if(MC.screen!=null)MC.setScreen(null);
            if(cameraAt==0) {
                Vec3 eye=center.add(EYES[index]);Vec3 delta=center.subtract(eye);
                float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
                float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
                MC.player.setPos(eye.x,eye.y-MC.player.getEyeHeight(),eye.z);
                MC.player.setYRot(yaw);MC.player.yRotO=yaw;MC.player.setXRot(pitch);MC.player.xRotO=pitch;
                MC.player.setNoGravity(true);MC.player.setDeltaMovement(Vec3.ZERO);
                var uuid=MC.player.getUUID();var server=MC.getSingleplayerServer();
                future=CompletableFuture.runAsync(()->{
                    var p=server.getPlayerList().getPlayer(uuid);
                    p.teleportTo(server.overworld(),eye.x,eye.y-p.getEyeHeight(),eye.z,yaw,pitch);
                    p.setNoGravity(true);p.getAbilities().flying=true;p.onUpdateAbilities();
                },server);
                cameraAt=System.nanoTime();frames=0;
            }
        }catch(Throwable error){fail(error);}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||done||cameraAt==0||MC.level==null||MC.getOverlay()!=null)return;
        if(++frames<90 || System.nanoTime()-cameraAt<4_000_000_000L)return;
        try {
            var apiType=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            var api=apiType.getMethod("getInstance").invoke(null);
            if(!(boolean)apiType.getMethod("isShaderPackInUse").invoke(api))throw new IllegalStateException("Shaders inactive");
            String file=inventory?"06-spawn-eggs-creative-tab.png":NAMES[index]+".png";
            try(var image=Screenshot.takeScreenshot(MC.getMainRenderTarget())){image.writeToFile(OUT.resolve(file));}
            records.add(file);System.out.println("RAY_PREVIEW CAPTURE "+file);
            if(inventory) {
                Files.writeString(OUT.resolve("passed.txt"),"Native NaturalSpawner ray; client-tracked model; Complementary shaders; creative tab with all four eggs\n"+String.join("\n",records));
                System.out.println("RAY_PREVIEW PASS six real framebuffer captures");done=true;MC.stop();
            }else{index++;cameraAt=0;}
        }catch(Throwable error){fail(error);}
    }
    private static void fail(Throwable error) {
        done=true;error.printStackTrace();
        try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("failure.txt"),error.toString());}catch(Exception ignored){}
        MC.stop();
    }
}
