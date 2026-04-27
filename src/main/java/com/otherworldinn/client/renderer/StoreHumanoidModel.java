package com.otherworldinn.client.renderer;

import com.otherworldinn.entity.base.StoreEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public abstract class StoreHumanoidModel<T extends StoreEntity> extends HumanoidModel<T> {
    protected StoreHumanoidModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return createBodyLayer(false);
    }

    public static LayerDefinition createSlimBodyLayer() {
        return createBodyLayer(true);
    }

    private static LayerDefinition createBodyLayer(boolean slimArms) {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        float armWidth = slimArms ? 3.0F : 4.0F;
        float rightArmX = slimArms ? -4.0F : -5.0F;
        float leftArmX = slimArms ? 4.0F : 5.0F;
        float rightArmMinX = slimArms ? -3.0F : -4.0F;
        float leftArmMinX = 0.0F;

        partdefinition.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE)
                        .texOffs(32, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)),
                PartPose.ZERO);
        partdefinition.addOrReplaceChild(
                "hat",
                CubeListBuilder.create(),
                PartPose.ZERO);
        partdefinition.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(16, 32)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.ZERO);
        partdefinition.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(
                                rightArmMinX,
                                -2.0F,
                                -2.0F,
                                armWidth,
                                12.0F,
                                4.0F,
                                CubeDeformation.NONE)
                        .texOffs(40, 32)
                        .addBox(
                                rightArmMinX,
                                -2.0F,
                                -2.0F,
                                armWidth,
                                12.0F,
                                4.0F,
                                new CubeDeformation(0.25F)),
                PartPose.offset(rightArmX, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create()
                        .texOffs(32, 48)
                        .addBox(
                                leftArmMinX,
                                -2.0F,
                                -2.0F,
                                armWidth,
                                12.0F,
                                4.0F,
                                CubeDeformation.NONE)
                        .texOffs(48, 48)
                        .addBox(
                                leftArmMinX,
                                -2.0F,
                                -2.0F,
                                armWidth,
                                12.0F,
                                4.0F,
                                new CubeDeformation(0.25F)),
                PartPose.offset(leftArmX, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(0, 32)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        partdefinition.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create()
                        .texOffs(16, 48)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(0, 48)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    protected abstract void applyIdlePose(T entity, float ageInTicks);

    @Override
    public void setupAnim(
            T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F);
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        this.head.zRot = 0.0F;
        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        this.rightArm.xRot = 0.0F;
        this.rightArm.yRot = 0.0F;
        this.rightArm.zRot = 0.0F;
        this.leftArm.xRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightLeg.xRot = 0.0F;
        this.rightLeg.yRot = 0.0F;
        this.rightLeg.zRot = 0.0F;
        this.leftLeg.xRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        this.leftLeg.zRot = 0.0F;
        this.applyIdlePose(entity, ageInTicks);
        this.hat.copyFrom(this.head);
    }
}
