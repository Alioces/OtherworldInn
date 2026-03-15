package com.otherworldinn.client.renderer;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.store.MagicianEntity;
import net.minecraft.client.animation.AnimationDefinition;
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
    protected AnimationDefinition getIdleAnimation(MagicianEntity entity) {
        return MagicianAnimation.LOOP;
    }
}
