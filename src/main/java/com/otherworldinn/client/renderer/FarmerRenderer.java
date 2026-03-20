package com.otherworldinn.client.renderer;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.store.FarmerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class FarmerRenderer extends MobRenderer<FarmerEntity, FarmerModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "textures/entity/store/farmer.png");

    public FarmerRenderer(EntityRendererProvider.Context context) {
        super(context, new FarmerModel(context.bakeLayer(FarmerModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(FarmerEntity entity) {
        return TEXTURE;
    }
}
