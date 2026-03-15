package com.otherworldinn.client.renderer;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.store.BlacksmithEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

public class BlacksmithModel extends StoreHumanoidModel<BlacksmithEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "blacksmith"),
                    "main");

    public BlacksmithModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return StoreHumanoidModel.createBodyLayer();
    }

    @Override
    protected void applyIdlePose(BlacksmithEntity entity, float ageInTicks) {
        float t = ageInTicks * 0.08F;
        float cycle = (ageInTicks % 160.0F) / 160.0F;
        boolean rightPhase = cycle < 0.5F;
        float localPhase = rightPhase ? cycle * 2.0F : (cycle - 0.5F) * 2.0F;
        float inspectRaw = localPhase < 0.5F ? localPhase * 2.0F : (1.0F - localPhase) * 2.0F;
        float inspect = smoothStep(inspectRaw);

        float rightInspect = rightPhase ? inspect : 0.0F;
        float leftInspect = rightPhase ? 0.0F : inspect;
        float handTwist = (float) Math.sin(ageInTicks * 0.22F) * 0.08F;
        float handRoll = (float) Math.cos(ageInTicks * 0.19F) * 0.05F;

        this.head.yRot += (float) Math.sin(t * 0.55F) * 0.05F + (rightInspect - leftInspect) * 0.10F;
        this.head.xRot += -0.03F + (rightInspect + leftInspect) * 0.08F;

        this.rightArm.xRot =
                (-0.40F + (float) Math.sin(t * 1.7F) * 0.06F) - rightInspect * 1.05F;
        this.rightArm.yRot = -0.16F - rightInspect * (0.22F - handTwist);
        this.rightArm.zRot = rightInspect * (0.08F + handRoll);

        this.leftArm.xRot =
                (-0.42F + (float) Math.cos(t * 1.6F) * 0.06F) - leftInspect * 1.05F;
        this.leftArm.yRot = 0.16F + leftInspect * (0.22F - handTwist);
        this.leftArm.zRot = -leftInspect * (0.08F + handRoll);

        this.rightLeg.xRot = -0.08F;
        this.leftLeg.xRot = -0.03F;
    }

    private static float smoothStep(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
}
