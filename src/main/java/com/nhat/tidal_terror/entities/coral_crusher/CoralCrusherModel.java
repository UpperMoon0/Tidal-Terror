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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class CoralCrusherModel<T extends Entity> extends EntityModel<T> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(TidalTerror.MODID, "coral_crusher"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart root;
    private final ModelPart upperJaw;
    private final ModelPart lowerJaw;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart leftRearFin;
    private final ModelPart rightRearFin;

    public CoralCrusherModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.tail = body.getChild("tail");
        this.upperJaw = head.getChild("upperjaw");
        this.lowerJaw = head.getChild("lowerjaw");
        this.leftFin = body.getChild("sidefinleft1");
        this.rightFin = body.getChild("sidefinright1");
        this.leftRearFin = body.getChild("sidefinleft2");
        this.rightRearFin = body.getChild("sidefinright2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -18.0F, -11.0F, 18.0F, 18.0F, 22.0F, new CubeDeformation(0.0F))
                .texOffs(58, 0).addBox(-7.0F, -16.0F, -19.0F, 14.0F, 14.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(0, 40).addBox(-7.0F, -16.0F, 11.0F, 14.0F, 14.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        // Rebase the existing cubes around their joints without changing the rest shape or UVs.
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(80, 22)
                .addBox(-6.0F, -5.0F, -4.0F, 12.0F, 10.0F, 4.0F), PartPose.offset(0.0F, -10.0F, -19.0F));
        head.addOrReplaceChild("upperjaw", CubeListBuilder.create().texOffs(90, 36)
                .addBox(-5.0F, -3.0F, -4.0F, 10.0F, 4.0F, 6.0F)
                .texOffs(0, 68).addBox(-6.0F, -4.0F, -14.0F, 12.0F, 5.0F, 10.0F),
                PartPose.offset(0.0F, -1.0F, -4.0F));
        head.addOrReplaceChild("lowerjaw", CubeListBuilder.create().texOffs(102, 71)
                .addBox(-4.0F, 0.0F, -4.0F, 8.0F, 4.0F, 6.0F)
                .texOffs(93, 98).addBox(-5.0F, 0.0F, -9.0F, 10.0F, 6.0F, 5.0F),
                PartPose.offset(0.0F, 2.0F, -4.0F));

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

        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(60, 40).addBox(-4.0F, -5.0F, 0.0F, 8.0F, 10.0F, 14.0F, new CubeDeformation(0.0F))
                .texOffs(0, 83).addBox(-3.0F, -2.0F, 14.0F, 6.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -9.0F, 25.0F));

        PartDefinition cube_r7 = tail.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(32, 75).addBox(-1.0F, 18.0F, 54.0F, 2.0F, 5.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(37, 49).addBox(-1.0F, 18.0F, 35.0F, 2.0F, 7.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 9.0F, -25.0F, 0.7854F, 0.0F, 0.0F));

        PartDefinition cube_r8 = tail.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(65, 64).addBox(-1.0F, -41.0F, 25.0F, 2.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 9.0F, -25.0F, -0.7854F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float swimAmount = Mth.clamp((float) entity.getDeltaMovement().length() * 4.0F, 0.0F, 1.0F);
        animatePose(entity.isInWaterOrBubble(), swimAmount, ageInTicks, netHeadYaw, headPitch, this.attackTime);
    }

    void animatePose(boolean inWater, float swimAmount, float ageInTicks, float netHeadYaw, float headPitch, float attackProgress) {
        // Models are shared between entities: every frame must start from the baked pose.
        this.root.getAllParts().forEach(ModelPart::resetPose);
        float stroke = ageInTicks * (inWater ? 0.25F : 0.6F);
        float wave = Mth.sin(stroke);
        float strength = inWater ? 0.08F + 0.30F * swimAmount : 0.45F;
        this.tail.yRot = wave * strength;
        this.body.yRot = wave * strength * 0.12F;
        this.body.xRot = inWater ? Mth.clamp(headPitch, -70.0F, 70.0F) * Mth.DEG_TO_RAD : 0.0F;
        this.body.y += inWater ? 0.35F * Mth.sin(ageInTicks * 0.12F) : 0.0F;
        this.body.zRot = inWater ? wave * 0.025F * swimAmount : wave * 0.10F;
        this.head.yRot = Mth.clamp(netHeadYaw, -20.0F, 20.0F) * Mth.DEG_TO_RAD;
        float flutter = Mth.sin(stroke + 0.8F) * (0.06F + 0.12F * swimAmount);
        this.leftFin.zRot += flutter;
        this.rightFin.zRot -= flutter;
        this.leftRearFin.zRot -= flutter * 0.6F;
        this.rightRearFin.zRot += flutter * 0.6F;
        this.body.getChild("upperfin1").yRot = wave * 0.035F;
        this.body.getChild("upperfin2").yRot = -wave * 0.025F;
        // LivingEntityRenderer supplies attackTime from the synchronized melee swing.
        float bite = Mth.sin(Mth.clamp(attackProgress, 0.0F, 1.0F) * Mth.PI);
        this.lowerJaw.xRot = 0.035F + 0.015F * Mth.sin(ageInTicks * 0.10F) + bite * 0.85F;
        this.upperJaw.xRot = -bite * 0.12F;
        this.head.xRot = -bite * 0.06F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
