package com.otherworldinn.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.otherworldinn.entity.base.StoreEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.HumanoidArm;

public abstract class StoreHumanoidModel<T extends StoreEntity> extends HierarchicalModel<T>
        implements ArmedModel, HeadedModel {
    protected final ModelPart root;
    protected final ModelPart waist;
    protected final ModelPart body;
    protected final ModelPart head;
    protected final ModelPart rightArm;
    protected final ModelPart leftArm;

    protected StoreHumanoidModel(ModelPart root) {
        this.root = root;
        this.waist = root.getChild("Waist");
        this.body = this.waist.getChild("Body");
        this.head = this.body.getChild("Head");
        this.rightArm = this.body.getChild("RightArm");
        this.leftArm = this.body.getChild("LeftArm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition waist =
                partdefinition.addOrReplaceChild(
                        "Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

        PartDefinition body =
                waist.addOrReplaceChild(
                        "Body",
                        CubeListBuilder.create()
                                .texOffs(16, 16)
                                .addBox(
                                        -4.0F,
                                        -11.0F,
                                        -2.0F,
                                        8.0F,
                                        12.0F,
                                        4.0F,
                                        new CubeDeformation(0.0F))
                                .texOffs(16, 32)
                                .addBox(
                                        -4.0F,
                                        -11.0F,
                                        -2.0F,
                                        8.0F,
                                        12.0F,
                                        4.0F,
                                        new CubeDeformation(0.25F)),
                        PartPose.offset(0.0F, -1.0F, 0.0F));

        body.addOrReplaceChild(
                "Head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, -11.0F, 0.0F));

        body.addOrReplaceChild(
                "RightArm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 32)
                        .addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(-5.0F, -9.0F, 0.0F));

        body.addOrReplaceChild(
                "LeftArm",
                CubeListBuilder.create()
                        .texOffs(32, 48)
                        .addBox(0.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 48)
                        .addBox(0.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(4.0F, -9.0F, 0.0F));

        partdefinition.addOrReplaceChild(
                "RightLeg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 32)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        partdefinition.addOrReplaceChild(
                "LeftLeg",
                CubeListBuilder.create()
                        .texOffs(16, 48)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 48)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    protected abstract AnimationDefinition getIdleAnimation(T entity);

    @Override
    public void setupAnim(
            T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.animate(entity.idleAnimationState, this.getIdleAnimation(entity), ageInTicks);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public ModelPart getHead() {
        return this.head;
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.waist.translateAndRotate(poseStack);
        this.body.translateAndRotate(poseStack);
        if (arm == HumanoidArm.RIGHT) {
            this.rightArm.translateAndRotate(poseStack);
        } else {
            this.leftArm.translateAndRotate(poseStack);
        }
    }
}
