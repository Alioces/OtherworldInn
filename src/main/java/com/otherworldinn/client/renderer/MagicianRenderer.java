package com.otherworldinn.client.renderer;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.store.MagicianEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class MagicianRenderer extends MobRenderer<MagicianEntity, MagicianModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    OtherworldInn.MODID, "textures/entity/store/magician.png");

    public MagicianRenderer(EntityRendererProvider.Context context) {
        super(context, new MagicianModel(context.bakeLayer(MagicianModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(MagicianEntity entity) {
        return TEXTURE;
    }
}
