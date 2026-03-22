package com.otherworldinn.client.renderer;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.store.FarmerEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

public class FarmerModel extends StoreHumanoidModel<FarmerEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "farmer"), "main");

    public FarmerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return StoreHumanoidModel.createBodyLayer();
    }

    @Override
    protected void applyIdlePose(FarmerEntity entity, float ageInTicks) {
        float t = ageInTicks * 0.08F;
        float sway = (float) Math.sin(t * 0.9F);
        float breathe = (float) Math.sin(t * 1.4F);

        this.body.xRot = breathe * 0.015F;
        this.body.yRot = sway * 0.03F;

        this.head.yRot += sway * 0.04F;
        this.head.xRot += -0.03F + breathe * 0.01F;

        this.rightArm.xRot = -0.32F + breathe * 0.04F;
        this.rightArm.yRot = -0.08F + sway * 0.03F;
        this.rightArm.zRot = 0.02F;

        this.leftArm.xRot = -0.38F - breathe * 0.04F;
        this.leftArm.yRot = 0.08F - sway * 0.03F;
        this.leftArm.zRot = -0.02F;

        this.rightLeg.xRot = -0.015F;
        this.leftLeg.xRot = 0.015F;
    }
}
