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
        this.head.yRot += (float) Math.sin(t * 0.7F) * 0.06F;
        this.rightArm.xRot = -0.25F + (float) Math.sin(t * 1.6F) * 0.22F;
        this.rightArm.yRot = -0.22F + (float) Math.sin(t * 1.2F) * 0.10F;
        this.leftArm.xRot = -0.35F + (float) Math.cos(t * 1.4F) * 0.24F;
        this.leftArm.yRot = 0.20F + (float) Math.cos(t * 1.1F) * 0.08F;
        this.leftArm.zRot = -0.03F + (float) Math.sin(t * 1.3F) * 0.08F;
    }
}
