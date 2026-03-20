package com.otherworldinn.client.renderer;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.store.FarmerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class FarmerRenderer extends HumanoidMobRenderer<FarmerEntity, FarmerModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "textures/entity/store/farmer.png");

    public FarmerRenderer(EntityRendererProvider.Context context) {
        super(context, new FarmerModel(context.bakeLayer(FarmerModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(
                new HumanoidArmorLayer<>(
                        this,
                        new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                        new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                        context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(FarmerEntity entity) {
        return TEXTURE;
    }
}
