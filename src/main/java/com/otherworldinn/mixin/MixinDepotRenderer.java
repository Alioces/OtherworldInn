package com.otherworldinn.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.otherworldinn.client.renderer.DisplayPriceRenderer;
import com.otherworldinn.util.KaleidoscopeRenderTargets;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DepotRenderer.class)
public class MixinDepotRenderer {
    private static final ResourceLocation COLD_CUT_HAM_SLICES_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "cold_cut_ham_slices_block");
    private static final ModelResourceLocation COLD_CUT_HAM_SLICES_FORCED_MODEL_INVENTORY =
            ModelResourceLocation.inventory(COLD_CUT_HAM_SLICES_MODEL_ID);
    private static final ModelResourceLocation COLD_CUT_HAM_SLICES_FORCED_MODEL_ITEM_PATH =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            "kaleidoscope_cookery", "item/cold_cut_ham_slices_block"));
    private static final ModelResourceLocation COLD_CUT_HAM_SLICES_FORCED_MODEL_STANDALONE =
            ModelResourceLocation.standalone(COLD_CUT_HAM_SLICES_MODEL_ID);

    @Inject(method = "renderSafe", at = @At("TAIL"), remap = false)
    private void injectRenderSafe(
            DepotBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay,
            CallbackInfo ci) {
        DisplayPriceRenderer.render(
                blockEntity, partialTicks, poseStack, bufferSource, light, overlay);
    }

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectRenderBlockItemAsBlock(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay,
            ItemStack itemStack,
            int angle,
            Random random,
            Vec3 itemPosition,
            boolean alwaysUpright,
            CallbackInfo ci) {
        if (!(itemStack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        if (!KaleidoscopeRenderTargets.isTargetBlock(blockItem.getBlock())) {
            return;
        }

        BlockState blockState =
                KaleidoscopeRenderTargets.resolveFoodRenderState(blockItem.getBlock().defaultBlockState());
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        boolean bottleBlock = KaleidoscopeRenderTargets.isBottleBlock(blockItem.getBlock());

        if (!bottleBlock) {
            if (KaleidoscopeRenderTargets.isColdCutHamSlicesBlock(blockItem.getBlock())) {
                float scale = 0.33f;
                poseStack.pushPose();
                poseStack.translate(-0.0f, 0.04f, -0.0f);
                poseStack.scale(scale, scale, scale);
                Minecraft mc = Minecraft.getInstance();
                BakedModel bakedModel = resolveColdCutHamSlicesModel(mc);
                mc.getItemRenderer()
                        .render(
                                itemStack,
                                ItemDisplayContext.FIXED,
                                false,
                                poseStack,
                                bufferSource,
                                light,
                                OverlayTexture.NO_OVERLAY,
                                bakedModel);
                poseStack.popPose();
                ci.cancel();
                return;
            }
            float scale = 0.7f * KaleidoscopeRenderTargets.getFoodScaleMultiplier(blockItem.getBlock());
            if (KaleidoscopeRenderTargets.isOneByTwoFoodBlock(blockItem.getBlock())) {
                BlockState[] states = KaleidoscopeRenderTargets.resolveOneByTwoStates(blockState);
                int pairCount = itemStack.getCount() >= 2 ? 2 : 1;
                float[] zOffsets =
                        pairCount == 2
                                ? new float[] {-0.5f * scale, 0.5f * scale}
                                : new float[] {0f};
                for (int pair = 0; pair < pairCount; pair++) {
                    float pairOffsetZ = zOffsets[pair];
                    for (int i = 0; i < 2; i++) {
                        poseStack.pushPose();
                        poseStack.translate(-scale + i * scale, -0.1f, -0.5f * scale + pairOffsetZ);
                        poseStack.scale(scale, scale, scale);
                        blockRenderer.renderSingleBlock(states[i], poseStack, bufferSource, light, overlay);
                        poseStack.popPose();
                    }
                }
                ci.cancel();
                return;
            }
            poseStack.pushPose();
            poseStack.translate(-0.5f * scale, -0.12f, -0.5f * scale);
            poseStack.scale(scale, scale, scale);
            blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, light, overlay);
            poseStack.popPose();
            ci.cancel();
            return;
        }

        int renderCount = Math.min(4, Math.max(1, itemStack.getCount()));
        float spacing = 0.38f;
        float[][] offsets;
        if (renderCount == 1) {
            offsets = new float[][] {{0f, 0f}};
        } else if (renderCount == 2) {
            offsets = new float[][] {{-spacing / 2f, 0f}, {spacing / 2f, 0f}};
        } else if (renderCount == 3) {
            offsets = new float[][] {
                {-spacing / 2f, -spacing / 2f}, {spacing / 2f, -spacing / 2f}, {0f, spacing / 2f}
            };
        } else {
            offsets = new float[][] {
                {-spacing / 2f, -spacing / 2f},
                {spacing / 2f, -spacing / 2f},
                {-spacing / 2f, spacing / 2f},
                {spacing / 2f, spacing / 2f}
            };
        }

        for (int i = 0; i < renderCount; i++) {
            float offsetX = offsets[i][0];
            float offsetZ = offsets[i][1];
            poseStack.pushPose();
            poseStack.translate(-0.35f + offsetX, -0.12f, -0.35f + offsetZ);
            poseStack.scale(0.7f, 0.7f, 0.7f);
            blockRenderer.renderSingleBlock(blockState, poseStack, bufferSource, light, overlay);
            poseStack.popPose();
        }

        ci.cancel();
    }

    private static BakedModel resolveColdCutHamSlicesModel(Minecraft mc) {
        BakedModel missingModel = mc.getModelManager().getMissingModel();
        BakedModel model = mc.getModelManager().getModel(COLD_CUT_HAM_SLICES_FORCED_MODEL_INVENTORY);
        if (model != missingModel) {
            return model;
        }
        model = mc.getModelManager().getModel(COLD_CUT_HAM_SLICES_FORCED_MODEL_ITEM_PATH);
        if (model != missingModel) {
            return model;
        }
        model = mc.getModelManager().getModel(COLD_CUT_HAM_SLICES_FORCED_MODEL_STANDALONE);
        if (model != missingModel) {
            return model;
        }
        return missingModel;
    }
}
