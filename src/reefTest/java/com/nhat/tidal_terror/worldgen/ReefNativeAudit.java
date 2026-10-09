package com.nhat.tidal_terror.worldgen;

import com.mojang.authlib.GameProfile;
import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.entities.coral_crusher.CoralCrusherEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Opt-in audit against a fresh normal Overworld, never the user's save. */
@Mod.EventBusSubscriber(modid="tidalterror")
public final class ReefNativeAudit {
    private static net.minecraft.server.MinecraftServer pending;
    private static int ticks;
    private static boolean prepared;
    private static BlockPos auditCenter;
    private static final Set<net.minecraft.world.level.ChunkPos> forced=new HashSet<>();
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        pending=event.getServer();
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END || pending==null || ++ticks<40)return;
        if(prepared && ticks<1200)for(var pos:forced){
            var chunk=pending.overworld().getChunkSource().getChunkNow(pos.x,pos.z);
            if(chunk==null || !chunk.getFullStatus().isOrAfter(net.minecraft.server.level.FullChunkStatus.BLOCK_TICKING))return;
        }
        audit(pending);
    }
    private static boolean largeAuditReef(ReefTerrain terrain,int x,int z) {
        if(!terrain.giant(x,z))return false;
        int reef=0,deep=0;
        for(int dx=-2048;dx<=2048;dx+=64)for(int dz=-2048;dz<=2048;dz+=64)
            if(terrain.reef(x+dx,z+dz)){reef++;if(terrain.floor(x+dx,z+dz)<=-40)deep++;}
        return reef>200 && deep>32;
    }
    private static void audit(net.minecraft.server.MinecraftServer server){
        var level = server.overworld();
        boolean defer=false;
        try {
            var terrain=new ReefTerrain(level,level.getChunkSource().getGenerator());
            if(auditCenter==null) {
                var found=ReefLocator.find(level,0,0,()->false);
                require(found!=null,"Province missing from default Overworld");
                auditCenter=new BlockPos(found.x(),32,found.z());
                require(largeAuditReef(terrain,auditCenter.getX(),auditCenter.getZ()),"Default Cathedral lost its full-sized core");
            }
            BlockPos center=auditCenter;
            System.out.println("REEF_AUDIT biome="+center);
            int minX = (center.getX() >> 4) * 16 - 48;
            int minZ = (center.getZ() >> 4) * 16 - 48;
            for(int x=minX>>4; x<=(minX+111)>>4; x++) for(int z=minZ>>4; z<=(minZ+111)>>4; z++) {
                level.getChunk(x,z);
                if(!prepared){level.setChunkForced(x,z,true);forced.add(new net.minecraft.world.level.ChunkPos(x,z));}
            }
            if(!prepared){prepared=true;defer=true;ticks=0;System.out.println("REEF_AUDIT PRELOADED; waiting for native world ticks");return;}
            List<String> voxels = new ArrayList<>();
            Set<String> palette = new TreeSet<>();
            List<BlockPos> clearWater = new ArrayList<>();
            int corals=0, tallest=0, plants=0, maxDepth=0, deepColumns=0, sedimentColumns=0, dead=0, air=0, bubbles=0,magma=0;
            for(int x=minX;x<minX+112;x++) for(int z=minZ;z<minZ+112;z++) {
                int low=999,high=-999;
                // Measure the actual generated sediment/depth, also on cold
                // reload; avoid reconstructing native terrain for every column.
                int floor=level.getMinBuildHeight()-1;
                for(int y=level.getMinBuildHeight();y<level.getSeaLevel();y++)
                    if(level.getBlockState(new BlockPos(x,y,z)).is(Blocks.SAND))floor=y;
                if(floor<level.getMinBuildHeight())floor=terrain.floor(x,z);
                if(terrain.reef(x,z) && floor<-40) {
                    int depth=level.getSeaLevel()-floor-1;
                    maxDepth=Math.max(maxDepth,depth);deepColumns++;
                    boolean sediment=true;
                    for(int y=floor;y>floor-6;y--)if(!level.getBlockState(new BlockPos(x,y,z)).is(Blocks.SAND))sediment=false;
                    if(!level.getBlockState(new BlockPos(x,floor-8,z)).is(Blocks.SANDSTONE))sediment=false;
                    if(sediment)sedimentColumns++;
                    for(int y=level.getMinBuildHeight();y<=floor-8;y++) {
                        var stratum=level.getBlockState(new BlockPos(x,y,z));
                        require(!stratum.is(Blocks.STONE) && !stratum.is(Blocks.DEEPSLATE),"Stone left under sand "+x+","+y+","+z);
                    }
                }
                for(int y=-55;y<63;y++) {
                    BlockPos pos=new BlockPos(x,y,z);
                    var state=level.getBlockState(pos);
                    if(y>floor && y<60 && terrain.reef(x,z)) {
                        if(state.isAir()) { air++;if(air<8)System.out.println("REEF_AUDIT AIR "+pos+" biome="+level.getBiome(pos).unwrapKey()+" type="+state+" below="+level.getBlockState(pos.below())+" above="+level.getBlockState(pos.above())); }
                        if(state.is(Blocks.BUBBLE_COLUMN))bubbles++;
                    }
                    if(state.is(Blocks.MAGMA_BLOCK))magma++;
                    if(state.is(BlockTags.CORAL_BLOCKS)) {
                        if(state.getBlock() instanceof net.minecraft.world.level.block.CoralBlock) {
                            state.getBlock().tick(state,level,pos,level.random);
                            require(level.getBlockState(pos).equals(state),"Native tick killed coral "+pos+" neighbours="+java.util.Arrays.stream(Direction.values()).map(d->d+":"+level.getBlockState(pos.relative(d))).toList());
                        }
                        if(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().startsWith("dead_"))dead++;
                        corals++;low=Math.min(low,y);high=Math.max(high,y);
                        palette.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
                    }
                    String name=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    if(name.contains("coral") || name.contains("pickle") || name.contains("seagrass")) {
                        voxels.add((x-minX)+","+y+","+(z-minZ)+","+name);
                        if(!state.is(BlockTags.CORAL_BLOCKS)) plants++;
                    }
                    if(y<45 && SpawnPlacements.checkSpawnRules(ModEntities.CORAL_CRUSHER.get(), level,
                            MobSpawnType.NATURAL,pos,level.random) && level.noCollision(ModEntities.CORAL_CRUSHER.get().getAABB(x+.5,y,z+.5)))
                        clearWater.add(pos);
                }
                if(high>low)tallest=Math.max(tallest,high-low+1);
            }
            require(corals>1000 && tallest>=75,"No giant coral in native terrain: "+corals+" height="+tallest);
            require(maxDepth>=100 && deepColumns>1000,"Deep basin missing: "+maxDepth+" columns="+deepColumns);
            require(sedimentColumns>deepColumns*.9,"Sand/sandstone strata broken: "+sedimentColumns+"/"+deepColumns);
            require(dead==0,"Dead coral found");
            require(air==0 && magma==0 && bubbles==0,"Water defects remain: air="+air+" magma="+magma+" bubbles="+bubbles);
            require(plants>100,"Small reef decorations missing");
            require(!clearWater.isEmpty(),"No deep-water shark spawn positions");
            var biomeRegistry=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
            int vanillaChecked=0;
            BlockPos validReefWater=clearWater.get(0);
            for(var key:biomeRegistry.registryKeySet()) {
                if(!key.location().getNamespace().equals("minecraft"))continue;
                var holder=biomeRegistry.getHolderOrThrow(key);
                require(holder.value().getMobSettings().getMobs(ModEntities.CORAL_CRUSHER.get().getCategory()).unwrap().stream()
                        .noneMatch(entry -> entry.type==ModEntities.CORAL_CRUSHER.get()),"Shark listed in vanilla biome "+key);
                // Keep the valid water column constant and vary only the biome.
                var view=(net.minecraft.world.level.ServerLevelAccessor)java.lang.reflect.Proxy.newProxyInstance(
                        ReefNativeAudit.class.getClassLoader(),new Class[]{net.minecraft.world.level.ServerLevelAccessor.class},
                        (proxy,method,args) -> method.getName().equals("getBiome")?holder:method.invoke(level,args));
                require(!SpawnPlacements.checkSpawnRules(ModEntities.CORAL_CRUSHER.get(),view,MobSpawnType.NATURAL,
                        validReefWater,level.random),"Vanilla biome admitted natural shark "+key);
                vanillaChecked++;
            }
            System.out.println("REEF_AUDIT BIOME_RESTRICTION PASS vanillaBiomes="+vanillaChecked);
            // A player is required by NaturalSpawner's native distance checks.
            BlockPos spawn=clearWater.get(clearWater.size()/2);
            ServerPlayer player=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"reef-audit"));
            player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player) {
                @Override public void send(Packet<?> packet) { }
                @Override public void send(Packet<?> packet, PacketSendListener listener) { }
            };
            player.setPos(spawn.getX()+45,spawn.getY(),spawn.getZ()+.5);
            level.addNewPlayer(player);
            level.setDefaultSpawnPos(new BlockPos(0,80,0),0);
            var nearby=clearWater.stream().filter(pos -> pos.distSqr(spawn)<1600).toList();
            for(var entity:java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).toList())
                if(entity instanceof CoralCrusherEntity)entity.discard();
            for(int attempt=0;attempt<300;attempt++) {
                BlockPos pos=nearby.get(level.random.nextInt(nearby.size()));
                NaturalSpawner.spawnCategoryForPosition(MobCategory.WATER_CREATURE,level,pos);
                for(var reefPool:ModEntities.reefPools())NaturalSpawner.spawnCategoryForPosition(reefPool,level,pos);
            }
            // Random water-column samples can legitimately miss the thin sandy band.
            // Exercise both real habitats through the same native placement/finalize path.
            verifyNativeSkin(level,player,clearWater,true,minX,minZ);
            verifyNativeSkin(level,player,clearWater,false,minX,minZ);
            int sharkCount=0, sandySharks=0, blueSharks=0;
            for(var entity:level.getAllEntities())if(entity instanceof CoralCrusherEntity shark) {
                sharkCount++;
                require(shark.isSandy()==CoralCrusherEntity.sandyAtSpawn(level,shark.blockPosition()),
                        "Native shark skin does not match spawn habitat at "+shark.blockPosition());
                if(shark.isSandy())sandySharks++;else blueSharks++;
            }
            require(sharkCount>0,"Native NaturalSpawner did not create a shark");
            require(sandySharks>0 && blueSharks>0,"Native spawning must produce both habitat skins");
            System.out.println("REEF_AUDIT NATIVE_SKINS PASS sandy="+sandySharks+" blue="+blueSharks);
            var deep=clearWater.stream().filter(pos->pos.getY()<=-15 &&
                    Math.abs(pos.getX()-spawn.getX())<32 && Math.abs(pos.getZ()-spawn.getZ())<32).toList();
            require(!deep.isEmpty(),"No deep fauna positions");
            player.setPos(spawn.getX()+45,-25,spawn.getZ()+.5);
            for(int attempt=0;attempt<250;attempt++) {
                BlockPos pos=deep.get(level.random.nextInt(deep.size()));
                NaturalSpawner.spawnCategoryForPosition(MobCategory.WATER_CREATURE,level,pos);
                for(var reefPool:ModEntities.reefPools())NaturalSpawner.spawnCategoryForPosition(reefPool,level,pos);
                NaturalSpawner.spawnCategoryForPosition(MobCategory.WATER_AMBIENT,level,pos);
                NaturalSpawner.spawnCategoryForPosition(MobCategory.CREATURE,level,pos);
            }
            Map<String,Integer> fauna=new TreeMap<>();
            for(var entity:level.getAllEntities())if(entity.getY()<=-15 &&
                    (entity.getType()==net.minecraft.world.entity.EntityType.DOLPHIN ||
                    entity.getType()==net.minecraft.world.entity.EntityType.TURTLE ||
                    entity.getType()==net.minecraft.world.entity.EntityType.TROPICAL_FISH))
                fauna.merge(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),1,Integer::sum);
            require(fauna.size()==3,"Native deep fauna missing: "+fauna);
            System.out.println("REEF_AUDIT DEEP_FAUNA PASS y<=-15 native="+fauna);
            // The stress-test population is not a preview of production rates.
            // Leave the isolated save clean for photographic/exploration imports.
            for(var entity:java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).toList())
                if(entity.getType()==net.minecraft.world.entity.EntityType.DOLPHIN ||
                        entity.getType()==net.minecraft.world.entity.EntityType.TURTLE ||
                        entity.getType()==net.minecraft.world.entity.EntityType.TROPICAL_FISH)entity.discard();
            var tab=com.nhat.tidal_terror.TidalTerror.TIDAL_TERROR_TAB.get();
            tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(),true,level.registryAccess()));
            var expectedItems=Set.of("coral_crusher_spawn_egg","cathedral_ray_spawn_egg","veilglow_spawn_egg","shardback_spawn_egg",
                    "raw_coral_crusher_steak","cooked_coral_crusher_steak","raw_cathedral_ray_wing","cooked_cathedral_ray_wing",
                    "raw_veilglow_gel","cooked_veilglow_gel","raw_shardback_claw","cooked_shardback_claw",
                    "crusher_tooth","shardback_plate","fang_arrow","reef_spear","reef_compass","reef_helmet","reef_chestplate","reef_leggings","reef_boots","enchanted_book");
            var actualItems=tab.getDisplayItems().stream().map(stack -> net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath()).collect(java.util.stream.Collectors.toSet());
            require(tab.getDisplayItems().size()==26 && actualItems.equals(expectedItems),"Wrong creative tab contents: "+actualItems);
            require(!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new net.minecraft.resources.ResourceLocation("tidalterror","example_item")),"Template item remains");
            require(!net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(new net.minecraft.resources.ResourceLocation("tidalterror","example_block")),"Template block remains");
            require(tab.getDisplayItems().stream().filter(stack -> stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)).count()==5,"Missing spear enchantment book levels");
            System.out.println("REEF_AUDIT CREATIVE_TAB PASS all four spawn eggs, eight raw/cooked foods, seven equipment/material items and five spear books");
            require(!SpawnPlacements.checkSpawnRules(ModEntities.CORAL_CRUSHER.get(),level,MobSpawnType.NATURAL,
                    spawn.atY(level.getSeaLevel()),level.random),"Surface spawn allowed");
            Files.createDirectories(Path.of("../reef-audit-v4"));
            List<String> climateMap=new ArrayList<>();
            int reefSamples=0,deepSamples=0;
            for(int dx=-2048;dx<=2048;dx+=64)for(int dz=-2048;dz<=2048;dz+=64) {
                int x=center.getX()+dx,z=center.getZ()+dz;
                boolean reef=terrain.reef(x,z);int floor=reef?terrain.floor(x,z):63;
                climateMap.add(x+","+z+","+(reef?1:0)+","+floor);
                if(reef)reefSamples++;
                if(reef && floor<=-40)deepSamples++;
            }
            System.out.println("REEF_AUDIT FOOTPRINT_MEASURE reefSamples="+reefSamples+" deepSamples="+deepSamples);
            // At 64 m spacing, 32 deep samples represent about 0.13 km²;
            // the 192 m gradual shelves occupy the remainder of the large reef.
            require(reefSamples>200 && deepSamples>32,"Reef footprint too small");
            Files.write(Path.of("../reef-audit-v4/climate-footprint.csv"),climateMap);
            System.out.println("REEF_AUDIT FOOTPRINT reefSamples="+reefSamples+" deepSamples="+deepSamples+" area=4096x4096m step=64m");

            Files.write(Path.of("../reef-audit-v4/native-reef-voxels.csv"),voxels);
            String report="seed=7142026 biome="+center+" sampleOrigin="+minX+","+minZ+" coralBlocks="+corals
                    +" tallestColumn="+tallest+" decorationBlocks="+plants+" palette="+palette
                    +" airBlocks="+air+" bubbleColumnBlocks="+bubbles+" magmaBlocks="+magma+" maxDepth="+maxDepth+" deepColumns="+deepColumns+" sedimentColumns="+sedimentColumns+" deepSpawnPositions="+clearWater.size()+" nativeSharks="+sharkCount;
            Files.writeString(Path.of("../reef-audit-v4/report.txt"),report);
            int coastChecks=0,maximumSeamError=0;
            BlockPos photographCoast=null;
            for(int ray=0;ray<8;ray++) {
                double angle=ray*Math.PI/4;
                BlockPos last=center;
                for(int distance=4;distance<=4096;distance+=4) {
                    BlockPos next=new BlockPos(center.getX()+(int)Math.round(Math.cos(angle)*distance),32,
                            center.getZ()+(int)Math.round(Math.sin(angle)*distance));
                    if(terrain.province(next.getX(),next.getZ())) { last=next;continue; }
                    int nativeFloor=level.getChunkSource().getGenerator().getBaseHeight(last.getX(),last.getZ(),
                            net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,level,
                            level.getChunkSource().randomState())-1;
                    int target=terrain.floor(last.getX(),last.getZ());
                    int error=Math.abs(target-nativeFloor);
                    maximumSeamError=Math.max(maximumSeamError,error);
                    require(error<=2,"Reef boundary differs from native terrain: "+last+" target="+target+" native="+nativeFloor);
                    int actual=solidSeabed(level,last),outside=solidSeabed(level,next);
                    require(Math.abs(actual-outside)<=12,"Generated biome seam has a cliff: "+last+" reef="+actual+" outside="+outside);
                    System.out.println("REEF_AUDIT GENERATED_COAST reef="+last+" solidFloor="+actual+" outside="+outside);
                    coastChecks++;
                    if(photographCoast==null && nativeFloor<level.getSeaLevel()-5)photographCoast=last.atY(target);
                    break;
                }
            }
            require(coastChecks>=4,"Too few coastline checks");
            if(photographCoast==null)photographCoast=center.atY(-20);
            Files.writeString(Path.of("../reef-audit-v4/boundary.txt"),photographCoast.getX()+","+photographCoast.getY()+","+photographCoast.getZ());
            System.out.println("REEF_AUDIT COAST nativeBoundaryChecks="+coastChecks+" maximumHeightError="+maximumSeamError+" photo="+photographCoast);
            // Native survival was checked on every actual coral block above, including chunk seams.
            Files.writeString(Path.of("../reef-audit-v4/location.txt"),center.getX()+","+center.getZ());
            ReefStructureAudit.verify(level, center);
            System.out.println("REEF_AUDIT PASS "+report);
            Files.writeString(Path.of("../reef-audit-v4/world-path.txt"),Path.of("reef-audit-world").toAbsolutePath().normalize().toString());
            Files.writeString(Path.of("../reef-audit-v4/passed.txt"),report+" vanillaBiomesRejected="+vanillaChecked+" allGeneratedCoralNativeTicksSurvive=true");
        } catch(Throwable error) {
            error.printStackTrace();
            System.out.println("REEF_AUDIT FAIL "+error);
        } finally { if(!defer){for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);forced.clear();pending=null;server.halt(false);} }
    }
    private static void verifyNativeSkin(net.minecraft.server.level.ServerLevel level,ServerPlayer player,
            List<BlockPos> clearWater,boolean sandy,int minX,int minZ) {
        // Group members wander horizontally. Keep their chunks inside the active
        // fixture so freshly added entities participate in the aggregate lookup.
        BlockPos site=clearWater.stream()
                .filter(pos->pos.getX()>=minX+16 && pos.getX()<minX+96 && pos.getZ()>=minZ+16 && pos.getZ()<minZ+96)
                .filter(pos->CoralCrusherEntity.sandyAtSpawn(level,pos)==sandy)
                .findFirst().orElseThrow(()->new AssertionError("No clear native habitat for sandy="+sandy));
        player.setPos(site.getX()+45,site.getY(),site.getZ()+.5);
        level.random.setSeed(sandy?7142026L:7142027L);
        int[] created={0};
        for(int attempt=0;attempt<64 && created[0]==0;attempt++)
            NaturalSpawner.spawnCategoryForPosition(ModEntities.CORAL_CRUSHER.get().getCategory(),level,
                    level.getChunkAt(site),site,
                    (type,pos,chunk)->type==ModEntities.CORAL_CRUSHER.get() && CoralCrusherEntity.sandyAtSpawn(level,pos)==sandy,
                    (mob,chunk)->{
                        require(mob instanceof CoralCrusherEntity shark && shark.isSandy()==sandy,
                                "Native finalizeSpawn chose wrong habitat skin at "+mob.blockPosition());
                        require(level.getEntity(mob.getUUID())==mob,"Habitat shark not visible in active fixture at "+mob.blockPosition());
                        System.out.println("REEF_AUDIT HABITAT_ENTITY sandy="+sandy+" pos="+mob.blockPosition());
                        created[0]++;
                    });
        require(created[0]>0,"Native spawner failed explicit habitat sandy="+sandy+" at "+site);
        System.out.println("REEF_AUDIT HABITAT_SKIN sandy="+sandy+" created="+created[0]+" site="+site);
    }
    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
    private static int solidSeabed(net.minecraft.server.level.ServerLevel level,BlockPos column){
        level.getChunk(column.getX()>>4,column.getZ()>>4);
        for(int y=level.getSeaLevel()-1;y>=level.getMinBuildHeight();y--){
            BlockPos p=column.atY(y);var block=level.getBlockState(p);
            if(block.isSolidRender(level,p))return y;
        }
        return level.getMinBuildHeight();
    }
}
