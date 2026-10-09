package com.nhat.tidal_terror.worldgen;

import com.mojang.serialization.JsonOps;
import com.nhat.tidal_terror.items.ModEquipment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Files;
import java.nio.file.Path;

/** Boots the same production sources with and without Endless in isolated normal worlds. */
@Mod.EventBusSubscriber(modid="tidalterror")
public final class OptionalEndlessAudit {
    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        var server=event.getServer();var level=server.overworld();
        try {
            boolean endless=ModList.get().isLoaded("endless");
            require(endless==Boolean.getBoolean("tidalterror.expectEndless"),"Runtime dependency matrix incorrect");
            var generator=level.getChunkSource().getGenerator();
            require(generator.getBiomeSource() instanceof MultiNoiseBiomeSource,"Normal world switched to province generation");
            require(!ReefSpawnHabitat.deep(level),"Normal world acquired deep spawning rules");
            require(level.getSectionsCount()==24,"Normal dense core widened");
            var presets=level.registryAccess().registryOrThrow(Registries.WORLD_PRESET);
            require(presets.containsKey(new ResourceLocation("tidalterror","reef_province_deep"))==endless,"Conditional world type registry incorrect");
            require(server.getPackRepository().getAvailableIds().contains(EndlessProvincePack.ID)==endless,"Optional pack offered incorrectly");
            var biomes=level.registryAccess().registryOrThrow(Registries.BIOME);
            var reef=biomes.getHolderOrThrow(ReefWorldgen.BIOME);
            var wastes=biomes.getHolderOrThrow(ReefWorldgen.WASTES);
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
            var shallow=new ReefProvinceBiomeSource(generator.getBiomeSource(),reef,wastes,false);
            var encoded=BiomeSource.CODEC.encodeStart(ops,shallow).getOrThrow(false,m->{});
            var decoded=BiomeSource.CODEC.parse(ops,encoded).getOrThrow(false,m->{});
            require(decoded instanceof ReefProvinceAccess access&&!access.deep(),"Shallow saved source no longer decodes");
            require(encoded.getAsJsonObject().has("delegate")&&!encoded.getAsJsonObject().has("value"),"Saved source codec shape changed");
            encoded.getAsJsonObject().addProperty("deep",true);
            var deep=BiomeSource.CODEC.parse(ops,encoded);
            require(deep.result().isPresent()==endless,"Deep save silently loaded without its terrain adapter");
            if(!endless)require(deep.error().orElseThrow().message().contains("Endless 0.9.3"),"Missing dependency error unclear");
            FeatureSorter.buildFeaturesPerStep(java.util.List.copyOf(generator.getBiomeSource().possibleBiomes()),b->b.value().getGenerationSettings().features(),true);
            require(level.getRecipeManager().byKey(new ResourceLocation("tidalterror","reef_compass")).orElseThrow()
                    .getResultItem(level.registryAccess()).is(ModEquipment.REEF_COMPASS.get()),"Normal compass recipe missing");
            int before=level.getChunkSource().getLoadedChunksCount();
            var found=ReefLocator.find(level,0,0,()->false);
            require(found!=null,"Legacy TerraBlender reef does not generate naturally");
            require(before==level.getChunkSource().getLoadedChunksCount(),"Normal compass lookup loaded distant chunks");
            var site=new BlockPos(found.x(),32,found.z());var terrain=new ReefTerrain(level,generator);
            require(terrain.provinceSample(site.getX(),site.getZ())==null,"Normal Cathedral became a province");
            require(terrain.reef(site.getX(),site.getZ()),"Legacy source misses located reef");
            level.getChunkAt(site);
            require(level.getBlockState(site.atY(-64)).is(Blocks.BEDROCK),"Normal bedrock floor removed");
            LegacyCathedralInventory.verify(level);
            var spawns=reef.value().getMobSettings();
            require(spawns.getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(s->s.type==EntityType.DROWNED&&s.getWeight().asInt()==1),"Reduced drowned weight lost");
            require(spawns.getMobs(MobCategory.WATER_AMBIENT).unwrap().stream().anyMatch(s->s.type==EntityType.TROPICAL_FISH),"Vanilla fish pool lost");
            System.out.println("OPTIONAL_ENDLESS_NATIVE_PASS endless="+endless+" normalReef="+site+" chunksBefore="+before+" chunksAfterLookup="+before+" preset,codec,features,spawns,compass,bedrock");
            Files.writeString(Path.of("passed.txt"),"endless="+endless+" normalReef="+site+"\n");
        } catch(Throwable failure) {
            failure.printStackTrace();
            try {Files.writeString(Path.of("failure.txt"),failure.toString());}catch(Exception ignored){}
        } finally {server.halt(false);}
    }
}
