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
        this.head.yRot += (float) Math.sin(t * 0.8F) * 0.05F;
        this.head.xRot += -0.02F;
        this.rightArm.xRot = -0.35F + (float) Math.sin(t * 1.3F) * 0.05F;
        this.rightArm.yRot = -0.1F;
        this.leftArm.xRot = -0.45F + (float) Math.cos(t * 1.15F) * 0.05F;
        this.leftArm.yRot = 0.1F;
        this.rightLeg.xRot = -0.02F;
        this.leftLeg.xRot = 0.02F;
    }
}
