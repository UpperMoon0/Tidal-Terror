package com.nhat.tidal_terror.preview;

import com.mojang.math.Axis;
import com.nhat.tidal_terror.TidalTerror;
import com.nhat.tidal_terror.items.ModEquipment;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Fresh isolated world, native renderers and actual final framebuffer; never shipped. */
@Mod.EventBusSubscriber(modid=TidalTerror.MODID,value=Dist.CLIENT)
public final class EquipmentPreview {
    private static final Minecraft MC=Minecraft.getInstance();
    private static final Path OUT=Path.of(System.getProperty("tidalterror.equipmentOutput","../../art/reef-equipment/runtime"));
    private static final long START=System.nanoTime();
    private static boolean opened,setup,done,reloaded;
    private static CompletableFuture<Void> future;
    private static int ticks,stage,frames;
    private static long ready;
    private static final List<String> CAPTURES=new ArrayList<>();
    private static ArmorStand mannequin;

    private static void equip(LivingEntity entity) {
        entity.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModEquipment.REEF_HELMET.get()));
        entity.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ModEquipment.REEF_CHESTPLATE.get()));
        entity.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ModEquipment.REEF_LEGGINGS.get()));
        entity.setItemSlot(EquipmentSlot.FEET,new ItemStack(ModEquipment.REEF_BOOTS.get()));
        entity.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModEquipment.REEF_SPEAR.get()));
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||done)return;
        try {
            if(System.nanoTime()-START>12L*60*1_000_000_000L)throw new IllegalStateException("Equipment preview timed out");
            if(!opened&&MC.getOverlay()==null) {
                opened=true;Files.createDirectories(OUT);Files.deleteIfExists(OUT.resolve("passed.txt"));Files.deleteIfExists(OUT.resolve("failure.txt"));
                MC.options.pauseOnLostFocus=false;MC.options.hideGui=true;MC.options.guiScale().set(2);
                MC.options.renderDistance().set(4);MC.options.simulationDistance().set(4);
                MC.options.fov().set(65);MC.options.bobView().set(false);MC.options.framerateLimit().set(45);MC.options.enableVsync().set(false);
                MC.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                if(MC.getWindow().isFullscreen())MC.getWindow().toggleFullScreen();
                MC.getWindow().setWindowed(1280,720);org.lwjgl.glfw.GLFW.glfwHideWindow(MC.getWindow().getWindow());
                String name="Reef Equipment Preview";
                if(Files.exists(Path.of("saves",name,"level.dat")))MC.createWorldOpenFlows().loadLevel(null,name);
                else MC.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name,GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                    new WorldOptions(7142026,false,false),
                    registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if(MC.player==null||MC.level==null||MC.getSingleplayerServer()==null)return;
            MC.getToasts().clear();
            if(!setup) {
                setup=true;var server=MC.getSingleplayerServer();var id=MC.player.getUUID();
                future=CompletableFuture.runAsync(()->{
                    var level=server.overworld();var player=server.getPlayerList().getPlayer(id);
                    level.setDayTime(6000);level.setWeatherParameters(0,100000,false,false);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
                    for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++) {
                        level.setBlock(new BlockPos(x,79,z),Blocks.SANDSTONE.defaultBlockState(),2);
                        for(int y=80;y<=86;y++)level.setBlock(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState(),2);
                    }
                    player.teleportTo(level,0,80,0,0,0);player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);
                    player.getAbilities().flying=true;player.onUpdateAbilities();equip(player);
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,100000,0,false,false));
                },server);return;
            }
            if(!future.isDone())return;future.join();
            if(++ticks<80)return;
            if(mannequin==null) {mannequin=new ArmorStand(MC.level,0,80,0);mannequin.setShowArms(true);equip(mannequin);}
            if(ready==0) {
                if(stage==0)MC.setScreen(new Board(false));
                else if(stage==1) {MC.options.hideGui=false;MC.setScreen(null);}
                else if(stage==2) {
                    MC.options.hideGui=false;MC.setScreen(null);
                    var server=MC.getSingleplayerServer();var id=MC.player.getUUID();
                    future=CompletableFuture.runAsync(()->{
                        var player=server.getPlayerList().getPlayer(id);
                        player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
                        player.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ModEquipment.REEF_SPEAR.get()));
                    },server);
                } else if(stage==3) {
                    MC.options.hideGui=false;
                    var screen=new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(MC.player,MC.level.enabledFeatures(),true);MC.setScreen(screen);
                    var select=screen.getClass().getDeclaredMethod("selectTab",CreativeModeTab.class);select.setAccessible(true);select.invoke(screen,TidalTerror.TIDAL_TERROR_TAB.get());
                    var entries=TidalTerror.TIDAL_TERROR_TAB.get().getDisplayItems();
                    for(var item:List.of(ModEquipment.REEF_SPEAR.get(),ModEquipment.REEF_HELMET.get(),ModEquipment.REEF_CHESTPLATE.get(),ModEquipment.REEF_LEGGINGS.get(),ModEquipment.REEF_BOOTS.get(),ModEquipment.CRUSHER_TOOTH.get(),ModEquipment.SHARDBACK_PLATE.get()))
                        if(entries.stream().noneMatch(s->s.is(item)))throw new IllegalStateException("Missing equipment creative entry "+item);
                } else if(stage==4&&!reloaded) {
                    reloaded=true;MC.setScreen(new Board(true));
                    future=MC.reloadResourcePacks();return;
                } else if(stage==4) MC.setScreen(new Board(true));
                ready=System.nanoTime();frames=0;
            }
        }catch(Throwable error){fail(error);}
    }

    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||done||ready==0||MC.getOverlay()!=null||MC.level==null)return;
        if(++frames<90||System.nanoTime()-ready<3_000_000_000L)return;
        try {
            if(stage==1||stage==2) {
                var type=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                var api=type.getMethod("getInstance").invoke(null);
                if(!(boolean)type.getMethod("isShaderPackInUse").invoke(api))throw new IllegalStateException("Equipment shader preview inactive");
            }
            String file=new String[]{"01-native-orthographic.png","02-underwater-first-person.png","03-underwater-offhand.png","04-creative-equipment.png","05-reload-and-glint.png"}[stage];
            try(var image=Screenshot.takeScreenshot(MC.getMainRenderTarget())){image.writeToFile(OUT.resolve(file));}
            CAPTURES.add(file);System.out.println("REEF_EQUIPMENT_PREVIEW CAPTURE "+file);
            stage++;ready=0;
            if(stage==5) {
                Files.writeString(OUT.resolve("passed.txt"),"Native worn armor, held spear, first person, creative items, full resource reload, glint; fresh isolated world\n"+String.join("\n",CAPTURES));
                System.out.println("REEF_EQUIPMENT_PREVIEW PASS five actual framebuffer captures");done=true;MC.stop();
            }
        }catch(Throwable error){fail(error);}
    }

    private static final class Board extends Screen {
        private final boolean glint;
        Board(boolean glint) {super(Component.literal("Reef Equipment"));this.glint=glint;}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
            g.fill(0,0,width,height,0xFFDDDCE4);
            g.drawCenteredString(font,"TIDAL TERROR - NATIVE EQUIPMENT",width/2,12,0xFF30263C);
            if(glint)for(var item:mannequin.getAllSlots())if(!item.isEmpty()&&!item.isEnchanted())item.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING,1);
            int floor=height-45;int scale=Math.min(100,(height-90)/3);
            for(int i=0;i<3;i++) {
                float yaw=180+i*90;
                mannequin.yBodyRot=yaw;mannequin.setYRot(yaw);mannequin.yHeadRot=yaw;mannequin.yHeadRotO=yaw;
                InventoryScreen.renderEntityInInventory(g,width*(i+1)/4,floor,scale,Axis.ZP.rotationDegrees(180),null,mannequin);
                g.drawCenteredString(font,new String[]{"FRONT","SIDE","BACK"}[i],width*(i+1)/4,height-30,0xFF30263C);
            }
            g.drawCenteredString(font,glint?"RESOURCE RELOAD + ENCHANTMENT GLINT":"ACTUAL ARMOR AND HELD SPEAR RENDERERS",width/2,height-15,0xFF30263C);
        }
    }
    private static void fail(Throwable error) {
        done=true;error.printStackTrace();
        try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("failure.txt"),error.toString());}catch(Exception ignored){}
        MC.stop();
    }
}
