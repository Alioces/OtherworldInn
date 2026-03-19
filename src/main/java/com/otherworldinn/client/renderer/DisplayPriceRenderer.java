package com.otherworldinn.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.economy.service.ItemSellPriceManager;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
    private static final Map<Long, CacheEntry> PRICE_CACHE = new HashMap<>();
    private static Level cacheLevel;

    private record CacheEntry(long tick, int price) {}

    public static void render(
            DepotBlockEntity blockEntity,
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

        if (cacheLevel != level) {
            cacheLevel = level;
            PRICE_CACHE.clear();
        }
        long gameTime = level.getGameTime();
        if (gameTime % 40L == 0L && PRICE_CACHE.size() > 1024) {
            pruneCache(gameTime);
        }

        BlockPos pos = blockEntity.getBlockPos();
        int unitPrice = getCachedDisplayUnitPrice(level, pos, blockEntity.getBlockState(), blockEntity);
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
        poseStack.translate(0.5D, 1.8D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        mc.font.drawInBatch(
                text,
                -width / 2.0F,
                0.0F,
                ModColors.WHITE_ALPHA_FULL,
                false,
                poseStack.last().pose(),
                bufferSource,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                0,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static int getCachedDisplayUnitPrice(
            Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        long key = pos.asLong();
        long gameTime = level.getGameTime();
        CacheEntry cached = PRICE_CACHE.get(key);
        if (cached != null && cached.tick == gameTime) {
            return cached.price;
        }
        int computed = getDisplayUnitPrice(level, pos, state, blockEntity);
        PRICE_CACHE.put(key, new CacheEntry(gameTime, computed));
        return computed;
    }

    private static void pruneCache(long gameTime) {
        Iterator<Map.Entry<Long, CacheEntry>> iterator = PRICE_CACHE.entrySet().iterator();
        while (iterator.hasNext()) {
            CacheEntry entry = iterator.next().getValue();
            if (gameTime - entry.tick > 40L) {
                iterator.remove();
            }
        }
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
