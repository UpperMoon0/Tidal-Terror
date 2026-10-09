package com.nhat.tidal_terror.worldgen;

import com.nhat.tidal_terror.items.*;
import net.minecraft.world.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Native locator persistence, recipe, legacy codec, and chunk-admission regressions. */
public final class ReefCompassAudit {
    private static void require(boolean c,String message){if(!c)throw new AssertionError(message);}
    public static void verify(ServerLevel level,ReefProvinceBiomeSource source) {
        require(source.placementVersion()==2,"New preset did not select balanced layout");
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var encoded=net.minecraft.world.level.biome.BiomeSource.CODEC.encodeStart(ops,source).getOrThrow(false,m->{});
        var decoded=(ReefProvinceBiomeSource)net.minecraft.world.level.biome.BiomeSource.CODEC.parse(ops,encoded).getOrThrow(false,m->{});
        require(decoded.placementVersion()==2,"Balanced placement version not saved");
        var legacy=encoded.deepCopy();legacy.getAsJsonObject().remove("placement_version");
        var old=(ReefProvinceBiomeSource)net.minecraft.world.level.biome.BiomeSource.CODEC.parse(ops,legacy).getOrThrow(false,m->{});
        require(old.placementVersion()==1,"Legacy saved source migrated unexpectedly");
        var c=ReefProvinceLayout.center(0,-5,3,1);
        require(c.equals(new ReefProvinceLayout.Center(-55689,43535)),"Original center moved");
        require(old.province(0,c.x(),c.z(),level.getChunkSource().randomState().sampler())!=null,"Original seed0 province lost");
        int loaded=level.getChunkSource().getLoadedChunksCount();
        long started=System.nanoTime();
        var target=ReefLocator.find(level,0,0,()->false);
        require(new ReefProvinceLayout.Center(-4317,37119).equals(target),"Balanced seed0 target changed when TerraBlender was installed");
        require(level.getChunkSource().getLoadedChunksCount()==loaded,"Locator generated distant chunks");
        var stack=new ItemStack(ModEquipment.REEF_COMPASS.get());
        require(ReefCompassItem.target(stack)==null,"Unbound compass invented a target");
        var tooltip=new java.util.ArrayList<net.minecraft.network.chat.Component>();
        ((ReefCompassItem)stack.getItem()).appendHoverText(stack,level,tooltip,TooltipFlag.NORMAL);
        require(tooltip.size()==1,"Unbound compass tooltip failed");
        ReefCompassItem.bind(stack,level,target);
        var restored=ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));
        var pos=CompassItem.getLodestonePosition(restored.getTag());
        require(pos!=null&&pos.dimension()==level.dimension()&&pos.pos().equals(new BlockPos(target.x(),level.getSeaLevel(),target.z())),"Compass target lost on native NBT roundtrip");
        require(!restored.getTag().getBoolean(CompassItem.TAG_LODESTONE_TRACKED),"Compass would require a lodestone block");
        ((ReefCompassItem)restored.getItem()).inventoryTick(restored,level,null,0,true);
        require(CompassItem.getLodestonePosition(restored.getTag()).equals(pos),"Native compass tick erased unattached target");
        require(restored.getDescriptionId().equals("item.tidalterror.reef_compass"),"Bound compass renamed itself to lodestone compass");
        require(level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("tidalterror","reef_compass")).isPresent(),"Crafting recipe missing");
        var recipe=level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("tidalterror","reef_compass")).orElseThrow();
        require(recipe.getResultItem(level.registryAccess()).is(ModEquipment.REEF_COMPASS.get()),"Recipe produces wrong item");
        restored.getTag().putLong("ReefCompassSeed",level.getSeed()+1);
        ((ReefCompassItem)restored.getItem()).inventoryTick(restored,level,null,0,true);
        require(CompassItem.getLodestonePosition(restored.getTag())==null,"Foreign-world target remained bound");
        System.out.println("REEF_COMPASS_NATIVE_PASS target="+target+" chunksBefore="+loaded+" chunksAfter="+level.getChunkSource().getLoadedChunksCount()+" searchMs="+(System.nanoTime()-started)/1e6+" recipe,codec,legacy,inventoryTick,foreignSeed");
    }
}
