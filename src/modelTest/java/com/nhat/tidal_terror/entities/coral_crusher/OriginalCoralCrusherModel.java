package com.nhat.tidal_terror.entities.coral_crusher;

// Made with Blockbench 4.8.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class OriginalCoralCrusherModel<T extends Entity> extends EntityModel<T> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(TidalTerror.MODID, "coral_crusher"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart tail;

    public OriginalCoralCrusherModel(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(80, 22).addBox(-6.0F, -15.0F, -23.0F, 12.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition upperjaw = head.addOrReplaceChild("upperjaw", CubeListBuilder.create().texOffs(90, 36).addBox(-5.0F, -14.0F, -27.0F, 10.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(102, 71).addBox(-4.0F, -8.0F, -27.0F, 8.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(0, 68).addBox(-6.0F, -15.0F, -37.0F, 12.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(93, 98).addBox(-5.0F, -8.0F, -32.0F, 10.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition lowerjaw = head.addOrReplaceChild("lowerjaw", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -18.0F, -11.0F, 18.0F, 18.0F, 22.0F, new CubeDeformation(0.0F))
                .texOffs(58, 0).addBox(-7.0F, -16.0F, -19.0F, 14.0F, 14.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(0, 40).addBox(-7.0F, -16.0F, 11.0F, 14.0F, 14.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition upperfin1 = body.addOrReplaceChild("upperfin1", CubeListBuilder.create(), PartPose.offset(0.0F, 1.0F, 3.0F));

        PartDefinition cube_r1 = upperfin1.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 99).addBox(-1.0F, -8.0F, 26.0F, 2.0F, 7.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(88, 73).addBox(-1.0F, -11.0F, 16.0F, 2.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.309F, 0.0F, 0.0F));

        PartDefinition upperfin2 = body.addOrReplaceChild("upperfin2", CubeListBuilder.create(), PartPose.offset(-2.0F, -6.0F, 0.0F));

        PartDefinition cube_r2 = upperfin2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(36, 99).addBox(1.0F, 15.0F, 13.0F, 2.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.309F, 0.0F, 0.0F));

        PartDefinition sidefinleft1 = body.addOrReplaceChild("sidefinleft1", CubeListBuilder.create(), PartPose.offsetAndRotation(-10.0F, -9.0F, -2.0F, 0.0F, 0.0F, 1.9199F));

        PartDefinition cube_r3 = sidefinleft1.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(94, 54).addBox(-1.0F, -8.0F, 26.0F, 2.0F, 7.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(74, 83).addBox(-1.0F, -11.0F, 16.0F, 2.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.309F, 0.0F, 0.0F));

        PartDefinition sidefinleft2 = body.addOrReplaceChild("sidefinleft2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.9199F));

        PartDefinition cube_r4 = sidefinleft2.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(102, 0).addBox(-7.0F, 15.0F, 9.0F, 2.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.309F, 0.0F, 0.0F));

        PartDefinition sidefinright1 = body.addOrReplaceChild("sidefinright1", CubeListBuilder.create(), PartPose.offsetAndRotation(10.0F, -9.0F, -2.0F, 0.0F, 0.0F, -1.9199F));

        PartDefinition cube_r5 = sidefinright1.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(22, 92).addBox(-1.0F, -8.0F, 26.0F, 2.0F, 7.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(50, 82).addBox(-1.0F, -11.0F, 16.0F, 2.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.309F, 0.0F, 0.0F));

        PartDefinition sidefinright2 = body.addOrReplaceChild("sidefinright2", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.0F, -6.0F, 0.0F, 0.0F, 0.0F, -1.9199F));

        PartDefinition cube_r6 = sidefinright2.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(60, 102).addBox(-1.0F, 15.0F, 9.0F, 2.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.309F, 0.0F, 0.0F));

        PartDefinition tail = partdefinition.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(60, 40).addBox(-4.0F, -14.0F, 25.0F, 8.0F, 10.0F, 14.0F, new CubeDeformation(0.0F))
                .texOffs(0, 83).addBox(-3.0F, -11.0F, 39.0F, 6.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition cube_r7 = tail.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(32, 75).addBox(-1.0F, 18.0F, 54.0F, 2.0F, 5.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(37, 49).addBox(-1.0F, 18.0F, 35.0F, 2.0F, 7.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F));

        PartDefinition cube_r8 = tail.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(65, 64).addBox(-1.0F, -41.0F, 25.0F, 2.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        tail.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
