package com.nhat.tidal_terror.preview;

import com.google.gson.GsonBuilder;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.NaturalSpawner;
import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Development-only real framebuffer capture; excluded from release jars. */
@Mod.EventBusSubscriber(modid="tidalterror", value=Dist.CLIENT)
public final class ReefShaderPreview {
    private record Shot(String name,Vec3 eye,Vec3 target,boolean shark) {}
    private static final Minecraft MC=Minecraft.getInstance();
    private static final boolean SANDY_PHOTOS=Boolean.getBoolean("tidalterror.sandyPhotosOnly");
    private static final boolean LUSH=Boolean.getBoolean("tidalterror.lushPreview");
    private static final Path OUT=Path.of(LUSH?"../../art/coral-cathedral-v4":SANDY_PHOTOS?"../../art/coral-crusher-sandy-v1":"../../art/coral-cathedral-v3");
    private static final Path AUDIT=Path.of(LUSH?"../reef-audit-v4":"../reef-audit-v2");
    private static boolean inventoryCapture,inventoryDone;
    private static final List<Map<String,Object>> RECORDS=new ArrayList<>();
    private static final List<Shot> SHOTS=new ArrayList<>();
    private static volatile UUID sharkId;
    private static volatile Vec3 sharkPos;
    private static int reefX,reefZ;
    private static boolean requested,setup,done;
    private static CompletableFuture<Void> future;
    private static int tick,index,frames;
    private static long cameraAt;
    private static final long START=System.nanoTime();

    @SubscribeEvent public static void natural(MobSpawnEvent.FinalizeSpawn event) {
        if(event.getEntity() instanceof CoralCrusherEntity shark && event.getSpawnType()==MobSpawnType.NATURAL && sharkId==null
                && Math.abs(shark.getX()-reefX)<=48 && Math.abs(shark.getZ()-reefZ)<=48
                && openWater(shark) && (!SANDY_PHOTOS || CoralCrusherEntity.sandyAtSpawn(
                        (net.minecraft.world.level.ServerLevelAccessor)shark.level(),shark.blockPosition()))) {
            shark.setPersistenceRequired();
            shark.setNoAi(true);shark.setNoGravity(true);shark.setDeltaMovement(Vec3.ZERO);
            sharkId=shark.getUUID();sharkPos=shark.position();
            System.out.println("REEF_PREVIEW NATURAL uuid="+sharkId+" pos="+sharkPos);
        }
    }
    private static boolean openWater(CoralCrusherEntity shark){
        BlockPos center=shark.blockPosition();
        for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)for(int y=-5;y<=5;y++)
            if(!shark.level().getBlockState(center.offset(x,y,z)).is(net.minecraft.world.level.block.Blocks.WATER))return false;
        return true;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || done)return;
        try {
            if(System.nanoTime()-START>18L*60*1_000_000_000L)throw new IllegalStateException("Preview timeout");
            if(!requested && MC.getOverlay()==null) {
                requested=true;Files.createDirectories(OUT);
                if(Boolean.getBoolean("tidalterror.sharkPhotosOnly")){
                    List<Map<String,Object>> previous=new com.google.gson.Gson().fromJson(Files.readString(OUT.resolve("capture-manifest.json")),
                            new com.google.gson.reflect.TypeToken<List<Map<String,Object>>>(){}.getType());
                    for(var record:previous)if(!record.get("file").toString().matches("0[89]-.*|10-.*"))RECORDS.add(record);
                }
                String[] location=Files.readString(AUDIT.resolve("location.txt")).trim().split(",");
                reefX=Integer.parseInt(location[0]);reefZ=Integer.parseInt(location[1]);
                MC.options.onboardAccessibility=false;MC.options.pauseOnLostFocus=false;MC.options.hideGui=true;
                MC.options.bobView().set(false);MC.options.enableVsync().set(false);MC.options.framerateLimit().set(60);
                MC.options.fov().set(65);MC.options.renderDistance().set(12);MC.options.simulationDistance().set(8);
                MC.options.cloudStatus().set(CloudStatus.OFF);MC.options.setCameraType(CameraType.FIRST_PERSON);
                // Keep the user's separate explorer visible while this client photographs.
                if(MC.getWindow().isFullscreen())MC.getWindow().toggleFullScreen();
                org.lwjgl.glfw.GLFW.glfwSetWindowAttrib(MC.getWindow().getWindow(),org.lwjgl.glfw.GLFW.GLFW_DECORATED,org.lwjgl.glfw.GLFW.GLFW_FALSE);
                MC.getWindow().setWindowed(1920,1080);
                org.lwjgl.glfw.GLFW.glfwHideWindow(MC.getWindow().getWindow());
                MC.createWorldOpenFlows().loadLevel(null,"Coral Cathedral Preview");
            }
            if(MC.level==null || MC.player==null || MC.getSingleplayerServer()==null) {
                if(requested && MC.screen!=null)for(var widget:MC.screen.children())if(widget instanceof Button button) {
                    String label=button.getMessage().getString().toLowerCase(Locale.ROOT);
                    if(label.contains("proceed") || label.contains("i know what")) { button.onPress();break; }
                }
                return;
            }
            if(MC.screen!=null && !inventoryCapture)MC.setScreen(null);
            if(!setup) {
                setup=true;
                var server=MC.getSingleplayerServer();var uuid=MC.player.getUUID();
                future=CompletableFuture.runAsync(() -> {
                    var level=server.overworld();var player=server.getPlayerList().getPlayer(uuid);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true,server);
                    level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);
                    player.setGameMode(GameType.CREATIVE);
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,100000,0,false,false));
                    // Remove the audit's deliberately accelerated population from this copy.
                    for(var entity:java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).filter(java.util.Objects::nonNull).toList())
                        if(entity.getType().getCategory()==MobCategory.WATER_CREATURE ||
                                entity.getType()==net.minecraft.world.entity.EntityType.TURTLE ||
                                entity.getType()==net.minecraft.world.entity.EntityType.TROPICAL_FISH)entity.discard();
                    player.teleportTo(level,reefX-45,-10,reefZ,0,0);
                    player.getAbilities().flying=true;player.onUpdateAbilities();
                    System.out.println("REEF_PREVIEW Preparing accelerated native spawn attempts");
                },server);
                return;
            }
            if(!future.isDone())return;future.join();
            tick++;
            if(sharkId==null) {
                // Accelerate native weighted selection, biome rules and collision checks.
                // Production spawning rates and caps are not changed by this photo fixture.
                if(tick==100 || tick%200==0)MC.getSingleplayerServer().execute(() -> {
                    var level=MC.getSingleplayerServer().overworld();
                    for(var entity:java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).filter(java.util.Objects::nonNull).toList())if(entity.getType().getCategory()==MobCategory.WATER_CREATURE
                            )entity.discard();
                    List<BlockPos> candidates=new ArrayList<>();
                    for(int x=reefX-16;x<=reefX+48;x+=3)for(int z=reefZ-32;z<=reefZ+48;z+=3)for(int y=-30;y<=25;y+=5) {
                        BlockPos pos=new BlockPos(x,y,z);
                        if(level.getBiome(pos).is(ReefWorldgen.BIOME) && level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WATER)) candidates.add(pos);
                    }
                    for(int attempt=0;attempt<300 && sharkId==null && !candidates.isEmpty();attempt++) {
                        NaturalSpawner.spawnCategoryForPosition(MobCategory.WATER_CREATURE,level,candidates.get(level.random.nextInt(candidates.size())));
                        for(var entity:level.getAllEntities())if(entity instanceof CoralCrusherEntity shark && openWater(shark)
                                && Math.abs(shark.getX()-reefX)<=48 && Math.abs(shark.getZ()-reefZ)<=48
                                && (!SANDY_PHOTOS || shark.isSandy())) {
                            shark.setPersistenceRequired();sharkId=shark.getUUID();sharkPos=shark.position();
                            shark.setNoAi(true);shark.setNoGravity(true);shark.setDeltaMovement(Vec3.ZERO);
                            System.out.println("REEF_PREVIEW TRIGGERED_NATURAL uuid="+sharkId+" pos="+sharkPos+" attempt="+attempt);
                            break;
                        }
                    }
                    if(sharkId==null)throw new IllegalStateException("Native spawn attempts failed: candidates="+candidates.size());
                });
                return;
            }
            if(SHOTS.isEmpty()) {
                Vec3 target=new Vec3(reefX+44,5,reefZ+44);
                SHOTS.add(new Shot("01-vast-deep-reef",target.add(65,40,-75),target,false));
                SHOTS.add(new Shot("02-seabed-gardens",new Vec3(reefX+65,-22,reefZ-20),new Vec3(reefX+44,-44,reefZ+25),false));
                SHOTS.add(new Shot("03-from-the-depths",new Vec3(reefX-25,-30,reefZ-25),target.add(0,15,0),false));
                SHOTS.add(new Shot("04-giant-coral-crown",target.add(-35,33,35),target.add(0,28,0),false));
                SHOTS.add(new Shot("05-swimming-between-corals",target.add(42,-12,35),target.add(0,-12,0),false));
                SHOTS.add(new Shot("06-near-surface-coral",target.add(-28,57,-15),target.add(0,40,0),false));
                SHOTS.add(new Shot("07-aerial-basin",target.add(-20,105,-20),target,false));
                SHOTS.add(new Shot("08-natural-crusher-side",new Vec3(8,2,-6),Vec3.ZERO,true));
                SHOTS.add(new Shot("09-natural-crusher-front",new Vec3(-7,2,-7),Vec3.ZERO,true));
                SHOTS.add(new Shot("10-natural-crusher-reef",new Vec3(11,5,8),Vec3.ZERO,true));
                String[] boundary=Files.readString(AUDIT.resolve("boundary.txt")).trim().split(",");
                Vec3 shore=new Vec3(Double.parseDouble(boundary[0]),Double.parseDouble(boundary[1]),Double.parseDouble(boundary[2]));
                Vec3 toward=new Vec3(reefX,shore.y,reefZ).subtract(shore).normalize();
                Vec3 wide=shore.add(toward.scale(100)),near=shore.add(toward.scale(60));
                SHOTS.add(new Shot("11-biome-transition",new Vec3(wide.x,Math.min(58,shore.y+30),wide.z),shore.add(toward.scale(20)).add(0,-8,0),false));
                SHOTS.add(new Shot("12-transition-seabed",new Vec3(near.x,Math.min(58,shore.y+12),near.z),shore.add(toward.scale(35)).add(0,-9,0),false));
                if(Boolean.getBoolean("tidalterror.sharkPhotosOnly") || SANDY_PHOTOS)SHOTS.removeIf(shot->!shot.shark);
                cameraAt=0;
            }
            if(index>=SHOTS.size()) {
                if(LUSH && !inventoryDone){
                    if(!inventoryCapture){
                        var tab=com.nhat.tidal_terror.TidalTerror.TIDAL_TERROR_TAB.get();
                        var screen=new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(MC.player,MC.level.enabledFeatures(),true);
                        MC.options.hideGui=false;MC.setScreen(screen);
                        var select=screen.getClass().getDeclaredMethod("selectTab",net.minecraft.world.item.CreativeModeTab.class);
                        select.setAccessible(true);select.invoke(screen,tab);
                        if(tab.getDisplayItems().size()!=1 || !tab.getDisplayItems().iterator().next().is(com.nhat.tidal_terror.TidalTerror.CORAL_CRUSHER_SPAWN_EGG.get()))
                            throw new IllegalStateException("Client creative tab has wrong contents");
                        inventoryCapture=true;frames=0;cameraAt=System.nanoTime();
                    }
                    return;
                }
                done=true;RECORDS.sort(Comparator.comparing(record->record.get("file").toString()));Files.writeString(OUT.resolve("capture-manifest.json"),new GsonBuilder().setPrettyPrinting().create().toJson(RECORDS));
                System.out.println("REEF_PREVIEW PASS frames="+RECORDS.size());MC.stop();return;
            }
            if(cameraAt==0) {
                Shot shot=SHOTS.get(index);
                if(shot.shark) for(var entity:MC.level.entitiesForRendering()) if(entity.getUUID().equals(sharkId)) sharkPos=entity.position().add(0,.5,0);
                Vec3 target=shot.shark?sharkPos:shot.target;
                Vec3 eye=shot.shark?target.add(shot.eye):shot.eye;
                eye=clearCamera(eye,target,shot.shark);
                setCamera(eye,target);cameraAt=System.nanoTime();frames=0;
                System.out.println("REEF_PREVIEW CAMERA "+shot.name+" eye="+eye+" target="+target);
            }
        } catch(Throwable error) { fail(error); }
    }

    private static void setCamera(Vec3 eye,Vec3 target) {
        Vec3 delta=target.subtract(eye);
        float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
        MC.player.setPos(eye.x,eye.y-MC.player.getEyeHeight(),eye.z);
        MC.player.setYRot(yaw);MC.player.yRotO=yaw;MC.player.setXRot(pitch);MC.player.xRotO=pitch;
        MC.player.setDeltaMovement(Vec3.ZERO);MC.player.setNoGravity(true);MC.player.getAbilities().flying=true;
        var server=MC.getSingleplayerServer();UUID uuid=MC.player.getUUID();
        future=CompletableFuture.runAsync(() -> {
            var player=server.getPlayerList().getPlayer(uuid);
            player.teleportTo(server.overworld(),eye.x,eye.y-player.getEyeHeight(),eye.z,yaw,pitch);
            player.setNoGravity(true);player.getAbilities().flying=true;player.onUpdateAbilities();
        },server);
    }

    private static boolean clear(Vec3 eye) {
        BlockPos origin=BlockPos.containing(eye);
        for(int x=-1;x<=1;x++)for(int y=-2;y<=1;y++)for(int z=-1;z<=1;z++) {
            var state=MC.level.getBlockState(origin.offset(x,y,z));
            if(!state.isAir() && !state.is(net.minecraft.world.level.block.Blocks.WATER))return false;
        }
        return true;
    }

    private static boolean sight(Vec3 eye,Vec3 target){
        return MC.level.clip(new net.minecraft.world.level.ClipContext(eye,target,net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,MC.player)).getType()==net.minecraft.world.phys.HitResult.Type.MISS;
    }
    private static Vec3 clearCamera(Vec3 eye,Vec3 target,boolean needsSight) {
        if(clear(eye) && (!needsSight || sight(eye,target)))return eye;
        Vec3 best=null;double score=Double.MAX_VALUE;
        for(int x=-8;x<=8;x++)for(int y=-8;y<=8;y++)for(int z=-8;z<=8;z++) {
            Vec3 point=eye.add(x,y,z);double distance=x*x+y*y+z*z;
            if(distance<score && clear(point) && (!needsSight || sight(point,target))) { best=point;score=distance; }
        }
        if(best==null)throw new IllegalStateException("No clear camera position around "+eye);
        return best;
    }

    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || done || cameraAt==0 || MC.level==null || MC.getOverlay()!=null)return;
        frames++;
        if(frames<180 || System.nanoTime()-cameraAt<10_000_000_000L)return;
        try {
            if(inventoryCapture){
                try(var image=Screenshot.takeScreenshot(MC.getMainRenderTarget())){
                    String name="13-spawn-egg-creative-tab.png";
                    image.writeToFile(OUT.resolve(name));
                    RECORDS.add(Map.of("file",name,"width",image.getWidth(),"height",image.getHeight(),"shadersActive",true,
                            "shaderPack","ComplementaryReimagined_r5.9.3.zip","creativeTab","Tidal Terror"));
                }
                System.out.println("REEF_PREVIEW CREATIVE_GUI PASS native egg icon and mod tab");
                inventoryDone=true;inventoryCapture=false;cameraAt=0;return;
            }
            Shot shot=SHOTS.get(index);
            Vec3 currentEye=MC.gameRenderer.getMainCamera().getPosition();
            if(shot.shark && (!clear(currentEye) || !sight(currentEye,sharkPos))){
                setCamera(clearCamera(currentEye,sharkPos,true),sharkPos);cameraAt=System.nanoTime();frames=0;return;
            }
            Class<?> apiType=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api=apiType.getMethod("getInstance").invoke(null);
            if(!(boolean)apiType.getMethod("isShaderPackInUse").invoke(api))throw new IllegalStateException("Shader pack not active");
            String pack=Class.forName("net.irisshaders.iris.Iris").getMethod("getCurrentPackName").invoke(null).toString();
            if(!pack.contains("Complementary"))throw new IllegalStateException("Wrong shader pack "+pack);
            if(shot.shark && java.util.stream.StreamSupport.stream(MC.level.entitiesForRendering().spliterator(),false).noneMatch(entity -> entity.getUUID().equals(sharkId)))
                throw new IllegalStateException("Natural shark not tracked by client");
            if(SANDY_PHOTOS){
                CoralCrusherEntity clientShark=null;
                for(var entity:MC.level.entitiesForRendering())if(entity.getUUID().equals(sharkId))clientShark=(CoralCrusherEntity)entity;
                if(clientShark==null || !clientShark.isSandy())throw new IllegalStateException("Sandy variant not synchronized to client");
                var renderer=MC.getEntityRenderDispatcher().getRenderer(clientShark);
                if(!renderer.getTextureLocation(clientShark).getPath().endsWith("coral_crusher.sandy-v1.png"))
                    throw new IllegalStateException("Renderer selected wrong skin");
                System.out.println("REEF_PREVIEW SANDY_CLIENT PASS uuid="+sharkId+" texture="+renderer.getTextureLocation(clientShark));
            }
            // Inspect the live, ticking client after chunk postprocessing, not
            // just the freshly generated server fixture's block palettes.
            BlockPos camera=BlockPos.containing(MC.gameRenderer.getMainCamera().getPosition());
            int floatingAir=0,bubbles=0;
            for(int x=-24;x<=24;x++)for(int z=-24;z<=24;z++)for(int y=-24;y<=12;y++){
                BlockPos p=camera.offset(x,y,z);if(p.getY()>=MC.level.getSeaLevel()-3)continue;
                if(!MC.level.getBiome(p).is(ReefWorldgen.BIOME))continue;
                var state=MC.level.getBlockState(p);
                if(state.is(net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN))bubbles++;
                if(state.isAir() && Arrays.stream(net.minecraft.core.Direction.values()).allMatch(d->MC.level.getFluidState(p.relative(d)).is(net.minecraft.tags.FluidTags.WATER)))floatingAir++;
            }
            if(floatingAir!=0 || bubbles!=0)throw new IllegalStateException("Live reef water defects: floatingAir="+floatingAir+" bubbles="+bubbles+" shot="+shot.name);
            System.out.println("REEF_PREVIEW LIVE_WATER PASS "+shot.name+" floatingAir="+floatingAir+" bubbles="+bubbles);
            try(var image=Screenshot.takeScreenshot(MC.getMainRenderTarget())) {
                image.writeToFile(OUT.resolve(shot.name+".png"));
                RECORDS.add(Map.of("file",shot.name+".png","width",image.getWidth(),"height",image.getHeight(),
                        "shaderPack",pack,"shadersActive",true,"camera",MC.gameRenderer.getMainCamera().getPosition().toString(),
                        "naturalShark",sharkId.toString(),"spawnReason","NATURAL","seed",7142026,
                        "spawnTrigger","NaturalSpawner.spawnCategoryForPosition"));
            }
            System.out.println("REEF_PREVIEW CAPTURE "+shot.name);index++;cameraAt=0;
        } catch(Throwable error) { fail(error); }
    }

    private static void fail(Throwable error) {
        done=true;error.printStackTrace();System.out.println("REEF_PREVIEW FAIL "+error);
        try { Files.createDirectories(OUT);Files.writeString(OUT.resolve("failure.txt"),error.toString()); } catch(Exception ignored) {}
        MC.stop();
    }
}
