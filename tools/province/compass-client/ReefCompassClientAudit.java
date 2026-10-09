package com.nhat.tidal_terror.testing;

import com.nhat.tidal_terror.items.*;
import java.nio.file.*;
import java.util.Locale;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Native item-use/packet/model/framebuffer proof in an independent copied world. */
@Mod.EventBusSubscriber(modid="tidalterror",value=Dist.CLIENT)
public final class ReefCompassClientAudit {
    private static boolean opened,requested,done;
    private static int ticks,pose,poseTicks;
    private static float heading;
    private static final long START=System.nanoTime();
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("tidalterror.compassClientAudit")||event.phase!=TickEvent.Phase.END||done)return;
        var mc=Minecraft.getInstance();
        try {
            if(System.nanoTime()-START>300_000_000_000L)throw new AssertionError("Compass client check timed out");
            if(!opened&&mc.getOverlay()==null) {
                opened=true;mc.options.onboardAccessibility=false;mc.options.pauseOnLostFocus=false;
                mc.options.renderDistance().set(2);mc.options.simulationDistance().set(5);
                mc.options.framerateLimit().set(30);mc.getWindow().setTitle("Reef Compass native verification");
                mc.createWorldOpenFlows().loadLevel(null,"Reef Compass Native Check");return;
            }
            if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
            if(!requested) {
                requested=true;
                var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
                server.execute(()-> {
                    var player=server.getPlayerList().getPlayer(id);
                    player.setGameMode(GameType.CREATIVE);player.getInventory().selected=0;
                    player.getInventory().setItem(0,new ItemStack(ModEquipment.REEF_COMPASS.get()));
                    ModEquipment.REEF_COMPASS.get().use(player.serverLevel(),player,InteractionHand.MAIN_HAND);
                });return;
            }
            var stack=mc.player.getMainHandItem();
            if(!stack.is(ModEquipment.REEF_COMPASS.get())||ReefCompassItem.target(stack)==null)return;
            var target=ReefCompassItem.target(stack).pos();
            if(ticks++==0) {
                heading=(float)(Math.toDegrees(Math.atan2(target.getZ()-mc.player.getZ(),target.getX()-mc.player.getX()))-90);
                Files.createDirectories(Path.of("captures"));mc.setScreen(new InventoryScreen(mc.player));
            }
            float yaw=heading+pose*90;mc.player.setYRot(yaw);mc.player.setYBodyRot(yaw);mc.player.setYHeadRot(yaw);
            if(++poseTicks<50)return;
            var property=ItemProperties.getProperty(stack.getItem(),new ResourceLocation("angle"));
            if(property==null)throw new AssertionError("Native angle property not registered");
            float angle=property.call(stack,mc.level,mc.player,0),expected=(4-pose)%4/4.0F;
            double error=Math.min(Math.abs(angle-expected),1-Math.abs(angle-expected));
            if(error>.035)throw new AssertionError("Needle bearing wrong: "+angle+" expected "+expected);
            var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
            String sprite=model.getParticleIcon().contents().name().toString();
            int frame=Math.min(31,(int)(angle*32));
            String wanted=String.format(Locale.ROOT,"tidalterror:item/reef_compass_%02d",frame);
            if(!sprite.equals(wanted))throw new AssertionError("Native frame selection mismatch: "+sprite+" expected "+wanted);
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {image.writeToFile(Path.of("captures/compass-pose-"+pose+".png"));}
            System.out.println("REEF_COMPASS_CLIENT_POSE_PASS pose="+pose+" angle="+angle+" sprite="+sprite+" target="+target);
            pose++;poseTicks=0;
            if(pose==4) {
                Files.writeString(Path.of("passed.txt"),"Native asynchronous item use, client NBT sync, four bearings, atlas frames and screenshots pass\n");
                System.out.println("REEF_COMPASS_CLIENT_PASS");done=true;mc.stop();
            }
        } catch(Throwable t) {
            t.printStackTrace();try {Files.writeString(Path.of("failure.txt"),t.toString());}catch(Exception ignored){}
            done=true;mc.stop();
        }
    }
}
