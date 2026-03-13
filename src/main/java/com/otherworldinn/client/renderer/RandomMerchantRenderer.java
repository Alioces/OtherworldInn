package com.otherworldinn.client.renderer;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.store.RandomMerchantEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 随机商人渲染器
 * <p>
 * 暂时使用玩家模型作为占位符。
 * </p>
 */
public class RandomMerchantRenderer extends HumanoidMobRenderer<RandomMerchantEntity, PlayerModel<RandomMerchantEntity>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "textures/entity/store/blacksmith.png"); // 暂时复用铁匠纹理

    public RandomMerchantRenderer(EntityRendererProvider.Context context) {
        // 使用标准 Player 模型
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(RandomMerchantEntity entity) {
        return TEXTURE;
    }
}
