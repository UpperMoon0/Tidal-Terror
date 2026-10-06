package com.nhat.tidal_terror.mixin;

import com.nhat.tidal_terror.effects.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class BleedingLifecycleMixin {
    @Inject(method="onEffectAdded",at=@At("TAIL")) private void added(MobEffectInstance effect,net.minecraft.world.entity.Entity source,CallbackInfo ci) {
        var self=(LivingEntity)(Object)this;
        if(!self.level().isClientSide && effect.getEffect()==ModEffects.REEF_BLEEDING.get())ModEffects.beginBleeding(self);
    }
    @Inject(method="onEffectRemoved",at=@At("TAIL")) private void removed(MobEffectInstance effect,CallbackInfo ci) {
        if(effect.getEffect()==ModEffects.REEF_BLEEDING.get())ModEffects.clearBleedingClock((LivingEntity)(Object)this);
    }
    @Inject(method="tick",at=@At("TAIL")) private void trail(CallbackInfo ci) {
        var self=(LivingEntity)(Object)this;var effect=self.getEffect(ModEffects.REEF_BLEEDING.get());
        if(!self.level().isClientSide && self.tickCount%8==0 && self.isAlive() && effect!=null)
            com.nhat.tidal_terror.particles.ModParticles.bleed(self,3+net.minecraft.util.Mth.clamp(effect.getAmplifier(),0,3));
    }
    @Inject(method="knockback",at=@At("HEAD"),cancellable=true) private void statusKnockback(double strength,double x,double z,CallbackInfo ci) {
        if(ModEffects.isBleedingDamage((LivingEntity)(Object)this))ci.cancel();
    }
    @ModifyVariable(method="knockback",at=@At("HEAD"),argsOnly=true,ordinal=0) private double anchor(double strength) {
        var self=(LivingEntity)(Object)this;if(!self.isInWater()||!self.onGround())return strength;
        int pieces=0;for(var stack:self.getArmorSlots())if(stack.getItem() instanceof com.nhat.tidal_terror.items.ReefArmorItem)pieces++;
        return strength*(1-.05*pieces);
    }
}
