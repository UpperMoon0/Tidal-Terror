package com.nhat.tidal_terror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Loaded only by Item.initializeClient; server gameplay never initializes client classes. */
public final class ReefEquipmentClient {
    private static int resourceGeneration;
    public static void resetModels() { resourceGeneration++; }
    public static IClientItemExtensions armor() {
        return new IClientItemExtensions() {
            private EntityModelSet modelSet;
            private int generation = -1;
            private final EnumMap<EquipmentSlot, ReefArmorModel> models = new EnumMap<>(EquipmentSlot.class);
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                EntityModelSet current = Minecraft.getInstance().getEntityModels();
                if (modelSet != current || generation != resourceGeneration) {
                    models.clear(); modelSet = current; generation = resourceGeneration;
                }
                return models.computeIfAbsent(slot, s -> new ReefArmorModel(current.bakeLayer(ReefArmorModel.layer(s))));
            }
        };
    }

    public static IClientItemExtensions spear() {
        return new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new SpearRenderer();
                return renderer;
            }
        };
    }

    private static final class SpearRenderer extends BlockEntityWithoutLevelRenderer {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("tidalterror", "textures/item/reef_spear_model.png");
        private ModelPart model;
        private EntityModelSet modelSet;
        private int generation = -1;
        private SpearRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()); }
        @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
            EntityModelSet current = Minecraft.getInstance().getEntityModels();
            if (modelSet != current || model == null || generation != resourceGeneration) {
                modelSet = current; generation = resourceGeneration; model = current.bakeLayer(ReefSpearModel.LAYER);
            }
            poses.pushPose();
            poses.translate(0.5, 0.5, 0.5);
            poses.mulPose(Axis.XP.rotationDegrees(180));
            model.render(poses, ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutoutNoCull(TEXTURE), false, stack.hasFoil()), light, overlay);
            poses.popPose();
        }
    }
    private ReefEquipmentClient() {}
}
