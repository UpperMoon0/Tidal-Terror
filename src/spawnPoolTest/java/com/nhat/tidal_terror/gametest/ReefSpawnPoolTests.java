package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.entities.ModEntities;
import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("tidalterror") @PrefixGameTestTemplate(false)
public class ReefSpawnPoolTests {
    @GameTest(template="coral_crusher_pool", timeoutTicks=200, batch="reef_distribution")
    public static void reefRarityPreservesNativeBiomes(GameTestHelper h) {
        ReefDistributionTests.verify(h,h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS));
    }

    @GameTest(template="coral_crusher_pool", timeoutTicks=200, batch="crab_spawn")
    public static void crabNaturalSpawnTargetsSeabed(GameTestHelper h) {
        CrabSpawnTargetTests.verify(h,h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ReefWorldgen.BIOME),level->{
            var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"crab-spawn-test"));
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player){
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
            };
            level.addNewPlayer(player);return player;
        });
    }

    @GameTest(template="coral_crusher_pool", timeoutTicks=40)
    public static void nativeCategoriesAndBiomeData(GameTestHelper h) throws Exception {
        var types=List.of(ModEntities.CORAL_CRUSHER.get(),ModEntities.CATHEDRAL_RAY.get(),
                ModEntities.VEILGLOW.get(),ModEntities.SHARDBACK.get());
        var reef=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(ReefWorldgen.BIOME).value().getMobSettings();
        var dispatch=NaturalSpawner.class.getDeclaredField("SPAWNING_CATEGORIES");dispatch.setAccessible(true);
        var nativePools=Arrays.asList((MobCategory[])dispatch.get(null));
        h.assertTrue(new HashSet<>(ModEntities.reefPools()).size()==4,"Pools are not independent");
        for(int i=0;i<types.size();i++){
            var type=types.get(i);var category=ModEntities.reefPools().get(i);
            h.assertTrue(type.getCategory()==category&&nativePools.contains(category),"Native dispatch missing "+type);
            var encoded=MobCategory.CODEC.encodeStart(JsonOps.INSTANCE,category).result().orElseThrow();
            h.assertTrue(MobCategory.CODEC.parse(JsonOps.INSTANCE,encoded).result().orElseThrow()==category,"Category codec lost "+category);
            var entries=reef.getMobs(category).unwrap();
            h.assertTrue(entries.size()==1&&entries.get(0).type==type,"Biome pool mismatches entity category");
            h.assertTrue(reef.getMobs(MobCategory.WATER_CREATURE).unwrap().stream().noneMatch(e->e.type==type),"Still in shared aquatic pool");
        }
        h.assertTrue(reef.getMobs(MobCategory.MONSTER).unwrap().stream()
                .anyMatch(e->e.type==EntityType.DROWNED&&e.getWeight().asInt()==1),"Drowned reduction not loaded");
        h.succeed();
    }

    @GameTest(template="coral_crusher_pool", timeoutTicks=40)
    public static void nativeGlobalAndLocalCapIsolation(GameTestHelper h) throws Exception {
        var level=h.getLevel();var pos=h.absolutePos(new net.minecraft.core.BlockPos(10,5,10));
        var chunk=level.getChunkAt(pos);var chunkPos=chunk.getPos();
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"pool-counter"));
        var types=List.of(ModEntities.CORAL_CRUSHER.get(),ModEntities.CATHEDRAL_RAY.get(),
                ModEntities.VEILGLOW.get(),ModEntities.SHARDBACK.get(),EntityType.SQUID);
        var gate=NaturalSpawner.SpawnState.class.getDeclaredMethod("canSpawnForCategory",MobCategory.class,ChunkPos.class);
        gate.setAccessible(true);
        for(var candidate:types){
            var local=new LocalMobCapCalculator(level.getChunkSource().chunkMap);
            var cache=LocalMobCapCalculator.class.getDeclaredField("playersNearChunk");cache.setAccessible(true);
            @SuppressWarnings("unchecked") var nearby=(it.unimi.dsi.fastutil.longs.Long2ObjectMap<List<net.minecraft.server.level.ServerPlayer>>)cache.get(local);
            nearby.put(chunkPos.toLong(),List.of(player));
            List<Entity> counted=new ArrayList<>();
            // Fill every other species, including the vanilla aquatic pool.
            for(var type:types)if(type!=candidate)for(int i=0;i<type.getCategory().getMaxInstancesPerChunk();i++){
                var mob=type.create(level);mob.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);counted.add(mob);
            }
            var state=NaturalSpawner.createState(289,counted,(key,accept)->accept.accept(chunk),local);
            h.assertTrue((boolean)gate.invoke(state,candidate.getCategory(),chunkPos),"Other full pools block "+candidate);
            for(var other:types)if(other!=candidate)
                h.assertTrue(!(boolean)gate.invoke(state,other.getCategory(),chunkPos),"Filled pool was not counted: "+other);
            for(int i=0;i<candidate.getCategory().getMaxInstancesPerChunk();i++)local.addMob(chunkPos,candidate.getCategory());
            h.assertTrue(!local.canSpawn(candidate.getCategory(),chunkPos),"Native local cap ignored for "+candidate);
            h.assertTrue(!(boolean)gate.invoke(state,candidate.getCategory(),chunkPos),"Full local pool still admitted "+candidate);
        }
        h.succeed();
    }
}
