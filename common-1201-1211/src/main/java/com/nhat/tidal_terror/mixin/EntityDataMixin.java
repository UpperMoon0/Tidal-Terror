package com.nhat.tidal_terror.mixin;
import com.nhat.tidal_terror.platform.ReefEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class EntityDataMixin implements ReefEntityData {
    @Unique private CompoundTag tidalData=new CompoundTag();
    public CompoundTag reefData() { return tidalData; }
    @Inject(method="saveWithoutId",at=@At("RETURN")) private void save(CompoundTag tag,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<CompoundTag> ci) { if(!tidalData.isEmpty())tag.put("tidalterror:data",tidalData.copy()); }
    @Inject(method="load",at=@At("TAIL")) private void load(CompoundTag tag,CallbackInfo ci) { tidalData=tag.getCompound("tidalterror:data"); }
}
