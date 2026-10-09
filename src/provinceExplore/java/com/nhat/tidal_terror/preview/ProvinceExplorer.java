package com.nhat.tidal_terror.preview;

import com.google.gson.GsonBuilder;
import com.nhat.tidal_terror.worldgen.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Development-only native world tour. Leaves its independent save open for exploration. */
@Mod.EventBusSubscriber(modid="tidalterror", value=Dist.CLIENT)
public final class ProvinceExplorer {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final boolean DEEP = Boolean.getBoolean("tidalterror.deepExplore");
    private static final String SAVE = System.getProperty("tidalterror.performanceSave", DEEP ? "Deep Reef Province Exploration v5" : "Reef Province Exploration v2");
    private static final boolean GARDEN_REVIEW=Boolean.getBoolean("tidalterror.reefGardenReview");
    private static final Path OUT = Path.of("captures");
    private static final int[] DISTANCES = {500, 340, 230, 0, 0};
    private static final String[] NAMES = {"outer-wastes", "inner-wastes", "rim-passage", "cathedral", "cathedral-surface"};
    private static final List<Map<String,Object>> RECORDS = new ArrayList<>();
    private static boolean opened, done, configured, positioned;
    private static int cx, cz, index, frames, floor;
    private static long movedAt;
    private static boolean readinessLogged;
    private static BlockPos[] renderChecks;
    private static BlockPos[] floorChecks;
    private static CompletableFuture<Void> move, verification;
    private static final class Metrics {
        private static final boolean ACTIVE=Boolean.getBoolean("tidalterror.performanceMetrics");
        private static final java.lang.management.ThreadMXBean CPU=java.lang.management.ManagementFactory.getThreadMXBean();
        private static volatile boolean started;
        private static long frameAt,tickAt,tickStartAt,startAt;
        private static boolean skipFrame;
        private static final List<Double> frames=new ArrayList<>(),ticks=new ArrayList<>(),tickIntervals=new ArrayList<>();
        private static final List<Map<String,Object>> moves=new ArrayList<>();
        private static final Map<Long,Long> cpuStart=new HashMap<>();
        static synchronized void start(){if(!ACTIVE||started)return;startAt=System.nanoTime();for(long id:CPU.getAllThreadIds())cpuStart.put(id,Math.max(0,CPU.getThreadCpuTime(id)));started=true;}
        static synchronized void frame(){if(!started)return;long now=System.nanoTime();if(frameAt!=0 && !skipFrame)frames.add((now-frameAt)/1e6);skipFrame=false;frameAt=now;}
        static synchronized void server(TickEvent.Phase phase){if(!started)return;if(phase==TickEvent.Phase.START){tickAt=System.nanoTime();if(tickStartAt!=0)tickIntervals.add((tickAt-tickStartAt)/1e6);tickStartAt=tickAt;}else if(tickAt!=0){ticks.add((System.nanoTime()-tickAt)/1e6);tickAt=0;}}
        static synchronized void capture(){skipFrame=true;}
        static synchronized void move(int shot,long requested,long began,long finished){if(!ACTIVE)return;var record=new LinkedHashMap<String,Object>();record.put("pose",NAMES[shot]);record.put("responseMs",(finished-requested)/1e6);record.put("queueMs",(began-requested)/1e6);record.put("executionMs",(finished-began)/1e6);moves.add(record);}
        static Map<String,Object> stats(List<Double> values){var sorted=new ArrayList<>(values);Collections.sort(sorted);var map=new LinkedHashMap<String,Object>();map.put("count",sorted.size());if(!sorted.isEmpty()){for(int p:new int[]{50,95,99})map.put("p"+p+"Ms",sorted.get(Math.min(sorted.size()-1,(int)Math.ceil(sorted.size()*p/100.0)-1)));map.put("maxMs",sorted.get(sorted.size()-1));map.put("totalMs",sorted.stream().mapToDouble(Double::doubleValue).sum());}return map;}
        static synchronized void finish() throws Exception {
            if(!started)return;started=false;var cpu=new LinkedHashMap<String,Double>();
            for(long id:CPU.getAllThreadIds()){var info=CPU.getThreadInfo(id);if(info==null)continue;String name=info.getThreadName();if(name.equals("Render thread")||name.equals("Server thread")||name.startsWith("Worker-Main"))cpu.merge(name,Math.max(0,CPU.getThreadCpuTime(id)-cpuStart.getOrDefault(id,0L))/1e6,Double::sum);}
            var result=new LinkedHashMap<String,Object>();result.put("save",SAVE);result.put("elapsedMs",(System.nanoTime()-startAt)/1e6);result.put("frames",stats(frames));result.put("serverTicks",stats(ticks));result.put("serverTickIntervals",stats(tickIntervals));result.put("moves",moves);result.put("cpuMs",cpu);
            try { result.put("admissionBuilds",DeepProvinceGenerator.class.getMethod("admissionBuilds").invoke(null)); } catch(NoSuchMethodException baseline) { result.put("admissionBuilds",null); }
            result.put("logicalMin",com.nstut.endless.heights.EndlessHeights.getMinBuildHeight());result.put("logicalMax",com.nstut.endless.heights.EndlessHeights.getMaxBuildHeight());
            Files.writeString(OUT.resolve("performance-metrics.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));System.out.println("PROVINCE_PERFORMANCE_COMPLETE "+result);
        }
    }
    @SubscribeEvent public static void serverMetrics(TickEvent.ServerTickEvent event){Metrics.server(event.phase);}

    private static final long START = System.nanoTime();

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || done) return;
        try {
            if (System.nanoTime()-START > 15L*60*1_000_000_000L) throw new IllegalStateException("World tour timed out");
            if (!opened && MC.getOverlay() == null) {
                String[] coords = Files.readString(Path.of("province-center.txt")).trim().split(",");
                cx=Integer.parseInt(coords[0]); cz=Integer.parseInt(coords[1]);
                Files.createDirectories(OUT); Files.deleteIfExists(OUT.resolve("failure.txt"));
                Files.deleteIfExists(OUT.resolve("manifest.json"));
                opened=true;
                MC.options.onboardAccessibility=false; MC.options.pauseOnLostFocus=false;
                MC.options.hideGui=true; MC.options.bobView().set(false);
                MC.options.renderDistance().set(DEEP?4:6); MC.options.simulationDistance().set(5);
                MC.options.fov().set(75); MC.options.framerateLimit().set(60);
                MC.options.cloudStatus().set(CloudStatus.OFF); MC.options.setCameraType(CameraType.FIRST_PERSON);
                MC.getWindow().setTitle("Tidal Terror - " + SAVE);
                MC.createWorldOpenFlows().loadLevel(null,SAVE);
            }
            if (MC.level==null || MC.player==null || MC.getSingleplayerServer()==null) {
                if (opened && MC.screen!=null) for (var widget:MC.screen.children()) if(widget instanceof Button b) {
                    String label=b.getMessage().getString().toLowerCase(Locale.ROOT);
                    if(label.contains("proceed") || label.contains("i know what")) { b.onPress(); break; }
                }
                return;
            }
            if (MC.screen!=null) MC.setScreen(null);
            if (verification!=null) {
                if (!verification.isDone()) return;
                verification.join(); verification=null;
            }
            if (index>=NAMES.length) {
                Files.writeString(OUT.resolve("manifest.json"),new GsonBuilder().setPrettyPrinting().create().toJson(RECORDS));
                Metrics.finish();
                MC.options.hideGui=false; done=true;
                if(DEEP && !Metrics.ACTIVE) {
                    var server=MC.getSingleplayerServer();var uuid=MC.player.getUUID();
                    server.execute(()->{
                        var level=server.overworld();var player=server.getPlayerList().getPlayer(uuid);
                        int y=new ReefTerrain(level,level.getChunkSource().getGenerator()).floor(cx,cz)+30;
                        player.teleportTo(level,cx+.5,y,cz+.5,90,0);
                        player.getAbilities().flying=true;player.onUpdateAbilities();
                        System.out.println("PROVINCE_EXPLORER REVIEW_POSE x="+cx+" y="+y+" z="+cz);
                    });
                }
                System.out.println("PROVINCE_EXPLORER READY captures="+RECORDS.size()+"; controls returned to player");
                if(Metrics.ACTIVE) MC.stop();
                return;
            }
            if (!positioned && move==null) {
                var server=MC.getSingleplayerServer(); var uuid=MC.player.getUUID();
                int offset=(int)Math.round(DISTANCES[index]*ReefProvinceLayout.SCALE);
                int x=cx+offset, z=cz, shot=index;
                long requested=System.nanoTime();
                move=CompletableFuture.runAsync(()->{
                    long began=System.nanoTime();
                    var level=server.overworld(); var player=server.getPlayerList().getPlayer(uuid);
                    if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof ReefProvinceAccess))
                        throw new IllegalStateException("Exploration save has the wrong generator");
                    if(!configured) {
                        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
                        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,server);
                        level.setDayTime(6000); level.setWeatherParameters(0,100000,false,false);
                        player.setGameMode(GameType.CREATIVE);
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,100000,0,false,false));
                        configured=true;Metrics.start();
                    }
                    var terrain=new ReefTerrain(level,level.getChunkSource().getGenerator());
                    floor=terrain.floor(x,z);
                    floorChecks=new BlockPos[9];int check=0;
                    for(int dx=-48;dx<=48;dx+=48)for(int dz=-48;dz<=48;dz+=48)
                        floorChecks[check++]=new BlockPos(x+dx,terrain.floor(x+dx,z+dz),z+dz);
                    // Require a section in front of this camera. Offscreen and
                    // buried sections can legitimately remain uncompiled.
                    int viewX=x-(shot==3 && !GARDEN_REVIEW?64:32);
                    int viewY=shot==4?63:shot==3?(GARDEN_REVIEW?floor+4:DEEP?floor+85:4):terrain.floor(viewX,z);
                    renderChecks=new BlockPos[]{new BlockPos(viewX,viewY,z)};
                    level.getChunk(x>>4,z>>4);
                    double y=shot==4?75:shot==3?(GARDEN_REVIEW?floor+5:DEEP?floor+65:-8):Math.min(48,floor+12);
                    player.teleportTo(level,x+.5,y,z+.5,90,shot==3 && !GARDEN_REVIEW?-12:shot==4?25:32);
                    player.getAbilities().flying=true; player.onUpdateAbilities();
                    Metrics.move(shot,requested,began,System.nanoTime());
                },server);
                return;
            }
            if (move!=null) {
                if(!move.isDone())return;
                move.join(); move=null; positioned=true; frames=0; movedAt=System.nanoTime(); readinessLogged=false;
            }
        } catch(Throwable error) { fail(error); }
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        if(event.phase==TickEvent.Phase.END)Metrics.frame();
        if(event.phase!=TickEvent.Phase.END || done || !positioned || MC.level==null || MC.getOverlay()!=null) return;
        try {
            frames++;
            if(frames<180 || System.nanoTime()-movedAt<25_000_000_000L) return;
            int x=cx+(int)Math.round(DISTANCES[index]*ReefProvinceLayout.SCALE), z=cz;
            // Wait for client-side terrain around the view, not merely a completed teleport.
            for(int dx=-48;dx<=48;dx+=48) for(int dz=-48;dz<=48;dz+=48) {
                if(!MC.level.hasChunkAt(new BlockPos(x+dx,32,z+dz)))return;
            }
            // A dense chunk packet can precede its sparse page packet. Check
            // actual seabed data around the view before accepting its mesh.
            for(var pos:floorChecks)if(!MC.level.getBlockState(pos).is(Blocks.SAND)
                    || MC.level.getBlockState(pos.above()).isAir())return;
            for(var pos:renderChecks)if(!MC.levelRenderer.isChunkCompiled(pos)) {
                if(!readinessLogged) {
                    readinessLogged=true;
                    System.out.println("PROVINCE_EXPLORER WAIT visible="+pos+" block="+MC.level.getBlockState(pos));
                    Metrics.capture();
                    try(var diagnostic=Screenshot.takeScreenshot(MC.getMainRenderTarget())) {
                        diagnostic.writeToFile(OUT.resolve("waiting-"+NAMES[index]+".png"));
                    }
                }
                return;
            }
            String file=String.format("%02d-%s.png",index+1,NAMES[index]);
            Metrics.capture();
            try(var image=Screenshot.takeScreenshot(MC.getMainRenderTarget())) { image.writeToFile(OUT.resolve(file)); }
            Map<String,Object> record=new LinkedHashMap<>();
            record.put("file",file); record.put("x",x); record.put("z",z); record.put("floor",floor);
            record.put("biome",MC.level.getBiome(new BlockPos(x,floor+5,z)).unwrapKey().orElseThrow().location().toString());
            RECORDS.add(record);
            System.out.println("PROVINCE_EXPLORER CAPTURE "+record);
            // Tick-time verification on the server thread after the photographed chunks were active.
            var server=MC.getSingleplayerServer();
            verification=CompletableFuture.runAsync(()->{
                var level=server.overworld();
                var terrain=new ReefTerrain(level,level.getChunkSource().getGenerator());
                int air=0,magma=0,bubbles=0;
                for(int dx=-16;dx<=16;dx+=4) for(int dz=-16;dz<=16;dz+=4) {
                    int bottom=terrain.floor(x+dx,z+dz);
                    for(int y=bottom+1;y<level.getSeaLevel();y++) {
                        var block=level.getBlockState(new BlockPos(x+dx,y,z+dz));
                        if(block.isAir())air++;
                        if(block.is(Blocks.MAGMA_BLOCK))magma++;
                        if(block.is(Blocks.BUBBLE_COLUMN))bubbles++;
                    }
                }
                if(air!=0 || magma!=0 || bubbles!=0) throw new IllegalStateException("Ticked water defects: air="+air+", magma="+magma+", bubbles="+bubbles);
                System.out.println("PROVINCE_EXPLORER TICKED_WATER PASS x="+x+" z="+z);
            },server);
            index++; positioned=false;
            // Tick() consumes verification before issuing the next teleport.
        } catch(Throwable error) { fail(error); }
    }
    private static void fail(Throwable error) {
        done=true; MC.options.hideGui=false;
        try { Files.createDirectories(OUT); Files.writeString(OUT.resolve("failure.txt"),error.toString()); } catch(Exception ignored) {}
        error.printStackTrace(); System.out.println("PROVINCE_EXPLORER FAILED");
        if(Metrics.ACTIVE) MC.stop();
    }
}
