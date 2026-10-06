package com.nhat.tidal_terror.mixin;
import com.nhat.tidal_terror.worldgen.ReefDistribution;
import com.nhat.tidal_terror.worldgen.ReefWorldgen;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import terrablender.api.RegionType;
import terrablender.worldgen.noise.Area;
// TerraBlender merges these methods before injection preparation. Apply after its
// mixin, and leave native/non-reef candidates and other regions untouched.
@Mixin(value=Climate.ParameterList.class,priority=900)
public abstract class ReefDistributionMixin<T> {
    @Unique private Area tidalterror$reefMask;
    @Shadow public abstract T findValue(Climate.TargetPoint point);
    @Dynamic("Added by TerraBlender")
    @Inject(method="initializeForTerraBlender",at=@At("TAIL"),remap=false)
    private void reefMask(RegistryAccess registry,RegionType type,long seed,CallbackInfo ci) {
        if(type==RegionType.OVERWORLD && tidalterror$reefMask==null) tidalterror$reefMask=ReefDistribution.mask(seed);
    }
    @Dynamic("Added by TerraBlender")
    @SuppressWarnings("unchecked")
    @Inject(method="findValuePositional",at=@At("RETURN"),cancellable=true,remap=false)
    private void rareReef(Climate.TargetPoint point,int x,int y,int z,CallbackInfoReturnable<T> ci) {
        if(tidalterror$reefMask!=null && ci.getReturnValue() instanceof Holder<?> holder
                && ((Holder<Biome>)holder).is(ReefWorldgen.BIOME) && tidalterror$reefMask.get(x,z)==0)
            // The native parameter tree retains datapack biome mappings.
            ci.setReturnValue(findValue(point));
    }
}
