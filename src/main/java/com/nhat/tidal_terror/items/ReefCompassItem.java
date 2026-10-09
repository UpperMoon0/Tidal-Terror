package com.nhat.tidal_terror.items;

import com.nhat.tidal_terror.worldgen.*;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class ReefCompassItem extends CompassItem {
    public ReefCompassItem(){super(new Properties().stacksTo(1));}
    public static net.minecraft.core.GlobalPos target(ItemStack stack) {
        return stack.hasTag()?getLodestonePosition(stack.getTag()):null;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        var serverLevel=(ServerLevel)level;
        if(level.dimension()!=Level.OVERWORLD || !(serverLevel.getChunkSource().getGenerator().getBiomeSource() instanceof ReefProvinceAccess)) {
            player.displayClientMessage(Component.translatable("message.tidalterror.reef_compass_no_resonance"),true);
            return InteractionResultHolder.fail(stack);
        }
        ProvinceCompassService.seek((ServerPlayer)player,(ServerLevel)level,stack);
        return InteractionResultHolder.consume(stack);
    }
    // This compass follows a Cathedral, never a player-placed lodestone.
    @Override public InteractionResult useOn(UseOnContext context){return InteractionResult.PASS;}
    @Override public String getDescriptionId(ItemStack stack){return getDescriptionId();}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) {
        if(level instanceof ServerLevel server && stack.hasTag() && stack.getTag().contains("ReefCompassSeed")
                && level.dimension()==Level.OVERWORLD && stack.getTag().getLong("ReefCompassSeed")!=server.getSeed()) {
            stack.getTag().remove(TAG_LODESTONE_POS);stack.getTag().remove(TAG_LODESTONE_DIMENSION);
            stack.getTag().remove(TAG_LODESTONE_TRACKED);stack.getTag().remove("ReefCompassSeed");
        }
        super.inventoryTick(stack,level,entity,slot,selected);
    }
    public static void bind(ItemStack stack,ServerLevel level,ReefProvinceLayout.Center center) {
        var tag=stack.getOrCreateTag();
        tag.put(TAG_LODESTONE_POS,NbtUtils.writeBlockPos(new BlockPos(center.x(),level.getSeaLevel(),center.z())));
        tag.putString(TAG_LODESTONE_DIMENSION,level.dimension().location().toString());
        tag.putBoolean(TAG_LODESTONE_TRACKED,false);
        tag.putLong("ReefCompassSeed",level.getSeed());
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.tidalterror.reef_compass").withStyle(ChatFormatting.AQUA));
        if(target(stack)!=null)
            lines.add(Component.translatable("tooltip.tidalterror.reef_compass_attuned").withStyle(ChatFormatting.GRAY));
    }
}
