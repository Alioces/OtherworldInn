package com.otherworldinn.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.economy.ItemSellPriceManager;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class DisplayPriceRenderer {
    public static void render(
            DepotBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay) {
        renderPriceLabel(blockEntity, poseStack, bufferSource);
    }

    public static void render(
            EjectorBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay) {
        renderPriceLabel(blockEntity, poseStack, bufferSource);
    }

    private static void renderPriceLabel(
            BlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource) {
        Level level = blockEntity.getLevel();
        if (level == null
                || !level.isClientSide
                || level.dimension() != TownDimensions.TOWN_LEVEL) {
            return;
        }

        int unitPrice =
                getDisplayUnitPrice(
                        level, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity);
        if (unitPrice <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null || mc.gameRenderer.getMainCamera() == null) {
            return;
        }

        Component text = Component.literal("\uE001" + unitPrice);
        float width = mc.font.width(text);
        float yaw = mc.gameRenderer.getMainCamera().getYRot();

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        mc.font.drawInBatch(
                text,
                -width / 2.0F,
                0.0F,
                0xFFFFFFFF,
                false,
                poseStack.last().pose(),
                bufferSource,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                0,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static int getDisplayUnitPrice(
            Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        IItemHandler itemHandler =
                level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, null);
        if (itemHandler == null) {
            return 0;
        }
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            var stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int unitPrice = ItemSellPriceManager.getPrice(stack);
            if (unitPrice > 0) {
                return unitPrice;
            }
        }
        return 0;
    }
}
