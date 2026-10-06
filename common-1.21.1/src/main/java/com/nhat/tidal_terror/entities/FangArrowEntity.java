package com.nhat.tidal_terror.entities;

import com.nhat.tidal_terror.effects.ModEffects;
import com.nhat.tidal_terror.items.ModEquipment;
import com.nhat.tidal_terror.particles.ModParticles;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Native arrow physics, piercing, pickup and save behavior; bleed only after accepted damage. */
public final class FangArrowEntity extends AbstractArrow {
    public FangArrowEntity(EntityType<? extends FangArrowEntity> type, Level level) { super(type, level); }
    public FangArrowEntity(Level level, LivingEntity shooter,ItemStack ammo,ItemStack weapon) { super(ModEntities.FANG_ARROW.get(), shooter, level,ammo,weapon); }
    @Override protected void doPostHurtEffects(LivingEntity victim) {
        super.doPostHurtEffects(victim);
        if (!level().isClientSide && victim.isAlive()) {
            ModEffects.applyBleeding(victim, getOwner(), 80, 0);
            ModParticles.bleed(victim, 12);
        }
    }

    @Override protected ItemStack getDefaultPickupItem() { return new ItemStack(ModEquipment.FANG_ARROW.get()); }
}
