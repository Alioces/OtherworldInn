package com.otherworldinn.client.renderer;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.store.MagicianEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

public class MagicianModel extends StoreHumanoidModel<MagicianEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "magician"), "main");

    public MagicianModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return StoreHumanoidModel.createBodyLayer();
    }

    @Override
    protected void applyIdlePose(MagicianEntity entity, float ageInTicks) {
        float t = ageInTicks * 0.1F;
        float stir = (float) Math.sin(t * 2.2F);
        float micro = (float) Math.sin(t * 0.7F);

        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;

        this.head.xRot += 0.42F + micro * 0.05F;
        this.head.yRot += stir * 0.08F;

        this.rightArm.xRot = -1.60F + stir * 0.34F;
        this.rightArm.yRot = -0.52F + stir * 0.18F;
        this.rightArm.zRot = 0.28F + stir * 0.14F;

        this.leftArm.xRot = -1.18F - stir * 0.14F;
        this.leftArm.yRot = 0.36F;
        this.leftArm.zRot = -0.20F;

        this.rightLeg.xRot = 0.12F;
        this.leftLeg.xRot = 0.10F;
    }
}
