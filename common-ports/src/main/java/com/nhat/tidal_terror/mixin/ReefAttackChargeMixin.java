package com.nhat.tidal_terror.mixin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class ReefAttackChargeMixin implements com.nhat.tidal_terror.platform.ReefAttackCharge {
    @Unique private float reefCharge;
    public float reefAttackCharge() { return reefCharge; }
    @Inject(method="attack",at=@At("HEAD"))private void beforeReset(net.minecraft.world.entity.Entity target,CallbackInfo ci) {
        reefCharge=((Player)(Object)this).getAttackStrengthScale(.5F);
    }
}
