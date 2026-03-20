package com.otherworldinn.client.renderer;

import com.otherworldinn.entity.base.GuestEntity;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 旅客实体渲染器
 *
 * <p>使用玩家模型渲染旅客。 支持 Steve (default) 和 Alex (slim) 模型。
 */
public class GuestRenderer<T extends GuestEntity> extends HumanoidMobRenderer<T, PlayerModel<T>> {

    private final PlayerModel<T> defaultModel;
    private final PlayerModel<T> slimModel;
    private final ItemRenderer itemRenderer;

    public GuestRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.defaultModel = this.model;
        this.itemRenderer = context.getItemRenderer();
        // PlayerModel 的构造函数接受 ModelPart 和 boolean (slim)
        // 但在新版本中，ModelLayers.PLAYER_SLIM 对应的 layer definition 结构可能不同
        // 此处假设 PlayerModel 能正确处理
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(
            T entity,
            float entityYaw,
            float partialTicks,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffer,
            int packedLight) {
        if ("slim".equals(entity.getModelType())) {
            this.model = this.slimModel;
        } else {
            this.model = this.defaultModel;
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        ItemStack headItem = entity.getHeadDisplayItem();
        if (!headItem.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.0D, entity.getBbHeight() + 0.65D, 0.0D);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.scale(0.55F, 0.55F, 0.55F);
            this.itemRenderer.renderStatic(
                    headItem,
                    ItemDisplayContext.GROUND,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    entity.level(),
                    entity.getId());
            poseStack.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.getSkinTexture();
    }
}
