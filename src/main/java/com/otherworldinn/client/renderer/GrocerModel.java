package com.otherworldinn.client.renderer;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.store.GrocerEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

public class GrocerModel extends StoreHumanoidModel<GrocerEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "grocer"), "main");

    public GrocerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return StoreHumanoidModel.createSlimBodyLayer();
    }

    @Override
    protected void applyIdlePose(GrocerEntity entity, float ageInTicks) {
        float t = ageInTicks * 0.08F;
        float sway = (float) Math.sin(t * 0.9F);
        float breathe = (float) Math.sin(t * 1.3F);

        this.body.yRot = sway * 0.02F;
        this.head.yRot += sway * 0.03F;
        this.head.xRot += breathe * 0.01F;

        this.rightArm.xRot = -0.28F + breathe * 0.03F;
        this.leftArm.xRot = -0.34F - breathe * 0.03F;
    }
}
