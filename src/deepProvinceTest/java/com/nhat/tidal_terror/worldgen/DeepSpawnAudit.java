package com.nhat.tidal_terror.worldgen;

import com.nhat.tidal_terror.entities.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.NaturalSpawner;

/** Real native spawn dispatch in the generated sparse basin, with a nearby player. */
final class DeepSpawnAudit {
    static void verify(ServerLevel level, BlockPos center) {
        var terrain=new ReefTerrain(level,level.getChunkSource().getGenerator());
        var chunk=level.getChunkAt(center);
        int x=chunk.getPos().getMinBlockX(),z=chunk.getPos().getMinBlockZ();
        int floor=terrain.floor(x+8,z+8),ceiling=ReefSpawnHabitat.ceiling(level,terrain,x+8,z+8);
        if(ceiling>=-64 || ceiling<=floor+20)throw new AssertionError("Not a deep canopy fixture");
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)level.getChunk((x>>4)+dx,(z>>4)+dz);
        var player=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"deep-spawn-audit"));
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        level.addNewPlayer(player);
        try {
            level.getRandom().setSeed(20261009L);
            for(var category:ModEntities.reefPools()) {
                player.setPos(x+48.5,floor+40,z+8.5);
                int[] count={0};
                for(int attempt=0;attempt<256;attempt++)NaturalSpawner.spawnCategoryForChunk(category,level,chunk,(type,pos,c)->true,(mob,c)->{
                    if(mob.getY()>=-64 || !ReefSpawnHabitat.allowed(level,mob.blockPosition()) || !level.noCollision(mob))
                        throw new AssertionError("Custom spawn escaped deep canopy: "+mob);
                    count[0]++;mob.discard();
                });
                if(count[0]==0)throw new AssertionError("No native deep spawns for "+category);
                var type=category==ModEntities.CRUSHER_POOL?ModEntities.CORAL_CRUSHER.get():category==ModEntities.RAY_POOL?
                        ModEntities.CATHEDRAL_RAY.get():category==ModEntities.VEILGLOW_POOL?ModEntities.VEILGLOW.get():ModEntities.SHARDBACK.get();
                if(SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,new BlockPos(x+8,ceiling+10,z+8),level.random))
                    throw new AssertionError("Custom placement accepted upper water");
                int wastesX=center.getX()+(int)Math.round(340*ReefProvinceLayout.SCALE);
                var wastes=new BlockPos(wastesX,terrain.floor(wastesX,center.getZ())+20,center.getZ());
                if(SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,wastes,level.random))
                    throw new AssertionError("Custom placement accepted Wastes");
                int before=count[0];
                for(int attempt=0;attempt<32;attempt++)NaturalSpawner.spawnCategoryForChunk(category,level,chunk,(t,p,c)->false,(m,c)->{count[0]++;m.discard();});
                if(count[0]!=before)throw new AssertionError("Bypassed native spawn predicate");
                System.out.println("DEEP_SPAWN custom="+category+" count="+count[0]);
            }
            int[] vanilla={0,0};
            var lowerSpecies=new java.util.HashSet<net.minecraft.world.entity.EntityType<?>>();
            for(int zone=0;zone<2;zone++) {
                player.setPos(x+48.5,zone==0?floor+40:level.getSeaLevel()-6,z+8.5);
                final int expected=zone;
                for(var category:java.util.List.of(MobCategory.WATER_AMBIENT,MobCategory.WATER_CREATURE,MobCategory.UNDERGROUND_WATER_CREATURE,MobCategory.CREATURE))
                for(int attempt=0;attempt<1024;attempt++)NaturalSpawner.spawnCategoryForChunk(category,level,chunk,(t,p,c)->true,(mob,c)->{
                    boolean below=mob.getY()<=ReefSpawnHabitat.ceiling(level,terrain,mob.getBlockX(),mob.getBlockZ());
                    if(below==(expected==0))vanilla[expected]++;
                    if(below)lowerSpecies.add(mob.getType());
                    mob.discard();
                });
                if(vanilla[zone]==0)throw new AssertionError("No native vanilla fish spawns in zone "+zone);
            }
            for(var type:java.util.List.of(net.minecraft.world.entity.EntityType.TROPICAL_FISH,net.minecraft.world.entity.EntityType.PUFFERFISH,
                    net.minecraft.world.entity.EntityType.SQUID,net.minecraft.world.entity.EntityType.DOLPHIN,
                    net.minecraft.world.entity.EntityType.GLOW_SQUID,net.minecraft.world.entity.EntityType.TURTLE))
                if(!lowerSpecies.contains(type))throw new AssertionError("No deep vanilla spawn for "+type);
            var entries=level.getBiome(center).value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
            if(entries.stream().noneMatch(e->e.type==net.minecraft.world.entity.EntityType.DROWNED && e.getWeight().asInt()==1))
                throw new AssertionError("Drowned reduction lost");
            player.setPos(x+48.5,floor+40,z+8.5);
            int[] drowned={0};
            var probe=new BlockPos(x+8,floor+40,z+8);
            System.out.println("DEEP_DROWNED_PROBE sky="+level.getBrightness(net.minecraft.world.level.LightLayer.SKY,probe)
                    +" block="+level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,probe)+" raw="+level.getMaxLocalRawBrightness(probe)
                    +" difficulty="+level.getDifficulty());
            // Weight 1 competes with the full legacy monster table, then the
            // native Drowned rule accepts only 1/40 trials. Stop at a witness.
            for(int attempt=0;attempt<131072 && drowned[0]==0;attempt++)NaturalSpawner.spawnCategoryForChunk(MobCategory.MONSTER,level,chunk,(t,p,c)->true,(mob,c)->{
                if(mob instanceof net.minecraft.world.entity.monster.Drowned && mob.getY()<ceiling)drowned[0]++;
                mob.discard();
            });
            if(drowned[0]==0)throw new AssertionError("No deep native Drowned spawn");
            System.out.println("DEEP_SPAWN_PASS vanillaBelow="+vanilla[0]+" vanillaAbove="+vanilla[1]+" lowerSpecies="+lowerSpecies+" drowned="+drowned[0]+" drownedWeight=1 floor="+floor+" ceiling="+ceiling);
        } finally { player.discard(); }
    }
    private DeepSpawnAudit() {}
}
