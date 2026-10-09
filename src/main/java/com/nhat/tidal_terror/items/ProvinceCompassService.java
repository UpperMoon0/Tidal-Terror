package com.nhat.tidal_terror.items;

import com.nhat.tidal_terror.worldgen.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-owned bounded worker; no per-tick biome searches or distant chunk loads. */
@Mod.EventBusSubscriber(modid="tidalterror")
public final class ProvinceCompassService {
    private static final Map<MinecraftServer,Session> SESSIONS=new WeakHashMap<>();
    private static final class Session {
        final Set<UUID> pending=new HashSet<>();
        final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(16),r->{
            var thread=new Thread(r,"Tidal Terror reef compass");thread.setDaemon(true);return thread;
        });
    }
    public static void seek(ServerPlayer player,ServerLevel level,ItemStack stack) {
        var server=level.getServer();var session=SESSIONS.computeIfAbsent(server,s->new Session());
        if(!session.pending.add(player.getUUID())) {message(player,"busy");return;}
        player.getCooldowns().addCooldown(stack.getItem(),60);
        var token=UUID.randomUUID();stack.getOrCreateTag().putUUID("ReefCompassSearch",token);
        var source=(ReefProvinceAccess)level.getChunkSource().getGenerator().getBiomeSource();
        var sampler=level.getChunkSource().randomState().sampler();
        var origin=player.blockPosition();var id=player.getUUID();
        message(player,"searching");
        try {
            session.worker.execute(()-> {
                ReefProvinceLayout.Center result=null;boolean failed=false;
                try {
                    result=ProvinceLocatorSearch.find(level.getSeed(),origin.getX(),origin.getZ(),source.placementVersion(),
                        c->source.province(level.getSeed(),c.x(),c.z(),sampler)!=null,
                        ()->Thread.currentThread().isInterrupted()||!server.isRunning());
                } catch(RuntimeException e) {
                    com.mojang.logging.LogUtils.getLogger().warn("Reef compass search failed",e);failed=true;
                }
                var target=result;var error=failed;
                if(!server.isRunning())return;
                server.execute(()-> {
                    session.pending.remove(id);
                    var current=server.getPlayerList().getPlayer(id);
                    boolean owned=current!=null&&current.serverLevel()==level&&current.isAlive()
                        &&java.util.stream.Stream.concat(current.getInventory().items.stream(),current.getInventory().offhand.stream()).anyMatch(s->s==stack);
                    if(!owned||!stack.hasTag()||!stack.getTag().hasUUID("ReefCompassSearch")||!stack.getTag().getUUID("ReefCompassSearch").equals(token))return;
                    stack.getTag().remove("ReefCompassSearch");
                    if(error){message(current,"failed");return;}
                    if(target==null){message(current,"not_found");return;}
                    ReefCompassItem.bind(stack,level,target);
                    int distance=(int)Math.round(Math.hypot((double)target.x()-current.getX(),(double)target.z()-current.getZ()));
                    current.displayClientMessage(Component.translatable("message.tidalterror.reef_compass_found",distance),true);
                });
            });
        } catch(RejectedExecutionException e) {
            session.pending.remove(id);stack.getTag().remove("ReefCompassSearch");message(player,"busy");
        }
    }
    private static void message(ServerPlayer player,String suffix) {
        player.displayClientMessage(Component.translatable("message.tidalterror.reef_compass_"+suffix),true);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        var session=SESSIONS.remove(event.getServer());if(session!=null)session.worker.shutdownNow();
    }
    private ProvinceCompassService(){}
}
