package com.otherworldinn.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.base.GuestEntity;
import com.otherworldinn.entity.base.VipGuestEntity;
import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.item.RoomKeyItem;
import com.otherworldinn.world.inn.GuestData;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.inn.RoomData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(
        modid = OtherworldInn.MODID,
        value = Dist.CLIENT)
public class GuestEntityTooltipOverlay {
    private static final int PANEL_PADDING = 6;
    private static final int LINE_GAP = 2;
    private static final int BAR_GAP = 6;
    private static final int BAR_WIDTH = 78;
    private static final int BAR_HEIGHT = 6;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int MARKER_HEIGHT_EXTRA = 2;
    private static final int MARKER_WIDTH = 2;
    private static final int MARKER_BAD_COLOR = 0xFFFF0000;
    private static final float HOLOGRAM_SCALE = 0.01F;
    private static final float SIDE_OFFSET = 1.2F;
    private static final float PANEL_WORLD_Z_OFFSET = 0.02F;
    private static final float Z_PANEL_BG = 0.0F;
    private static final float Z_BAR_BG = -0.002F;
    private static final float Z_BAR_FILL = -0.003F;
    private static final float Z_MARKER = -0.004F;
    private static final float Z_TEXT = -0.01F;
    private static final float Z_ITEM = -0.02F;
    private static final double HUD_FALLBACK_DISTANCE_SQR = 3.0D;

    @Nullable
    private static GuestEntity getTargetGuest(Minecraft mc) {
        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof EntityHitResult entityHitResult)) {
            return null;
        }
        if (entityHitResult.getEntity() instanceof GuestEntity guestEntity) {
            return guestEntity;
        }
        return null;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        GuestEntity guest = getTargetGuest(mc);
        if (guest == null) {
            return;
        }
        if (isHudFallbackRange(mc.player, guest)) {
            return;
        }

        Font font = mc.font;
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        Direction panelFacing = Direction.fromYRot(event.getCamera().getYRot());
        Direction panelSide = getPanelSidePerpendicular(mc.player, guest, panelFacing);
        double sideDistance = guest.getBbWidth() * 0.5 + SIDE_OFFSET + PANEL_WORLD_Z_OFFSET;
        Vec3 anchorPos =
                guest.position()
                        .add(
                                panelSide.getStepX() * sideDistance,
                                guest.getBbHeight() * 0.5,
                                panelSide.getStepZ() * sideDistance);
        float panelYaw = -panelFacing.toYRot();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(
                anchorPos.x - cameraPos.x,
                anchorPos.y - cameraPos.y,
                anchorPos.z - cameraPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(panelYaw));
        poseStack.scale(-HOLOGRAM_SCALE, -HOLOGRAM_SCALE, HOLOGRAM_SCALE);

        PanelData panelData = buildPanelData(mc, guest);
        PanelPainter worldPainter =
                new PanelPainter() {
                    @Override
                    public int measureText(Component text) {
                        return font.width(text);
                    }

                    @Override
                    public void drawRoundedRect(int x, int y, int width, int height, int color, float z) {
                        drawRoundedPixelRect(poseStack, x, y, width, height, color, z);
                    }

                    @Override
                    public void drawText(Component text, int x, int y, int color, float z) {
                        GuestEntityTooltipOverlay.drawText(
                                font, bufferSource, poseStack, text, x, y, color, z);
                    }

                    @Override
                    public void drawItem(ItemStack stack, int x, int y, float z) {
                        renderItemIcon(mc, bufferSource, poseStack, stack, x, y, z);
                    }
                };
        renderUnifiedPanel(worldPainter, panelData, 0, 0, true, true);
        bufferSource.endBatch();
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().getPath().equals("crosshair")) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        GuestEntity guest = getTargetGuest(mc);
        if (guest == null || !isHudFallbackRange(mc.player, guest)) {
            return;
        }
        PanelData panelData = buildPanelData(mc, guest);
        Font font = mc.font;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        PanelPainter hudPainter =
                new PanelPainter() {
                    @Override
                    public int measureText(Component text) {
                        return font.width(text);
                    }

                    @Override
                    public void drawRoundedRect(int x, int y, int width, int height, int color, float z) {
                        drawRoundedPixelRectHud(guiGraphics, x, y, width, height, color);
                    }

                    @Override
                    public void drawText(Component text, int x, int y, int color, float z) {
                        guiGraphics.drawString(font, text, x, y, color, false);
                    }

                    @Override
                    public void drawItem(ItemStack stack, int x, int y, float z) {
                        guiGraphics.renderItem(stack, x, y);
                    }
                };
        int panelX = guiGraphics.guiWidth() / 2 + 12;
        int panelY = guiGraphics.guiHeight() / 2 - 12;
        renderUnifiedPanel(hudPainter, panelData, panelX, panelY, false, false);
    }

    private static PanelData buildPanelData(Minecraft mc, GuestEntity guest) {
        GuestData data = guest.getGuestData();
        GuestData.IntRange comfort = data.getComfortPreference();
        GuestData.IntRange light = data.getLightPreference();
        GuestData.IntRange humidity = data.getHumidityPreference();
        RoomAttributes roomAttributes = resolveRoomAttributesForMarker(mc, guest, data);

        List<ItemStack> rewardIcons = new java.util.ArrayList<>();
        for (GuestData.RewardItem reward : data.getRewardItems()) {
            BuiltInRegistries.ITEM
                    .getOptional(reward.item())
                    .ifPresent(item -> rewardIcons.add(new ItemStack(item)));
        }

        return new PanelData(
                buildGuestTitle(guest),
                Component.literal("  ")
                        .append(
                                Component.translatable(
                                                "message.otherworldinn.guest.tooltip.preference.comfort",
                                                comfort.min(),
                                                comfort.max())
                                        .withStyle(style -> style.withColor(ModColors.COMFORT))),
                Component.literal("  ")
                        .append(
                                Component.translatable(
                                                "message.otherworldinn.guest.tooltip.preference.light",
                                                light.min(),
                                                light.max())
                                        .withStyle(style -> style.withColor(ModColors.LIGHT))),
                Component.literal("  ")
                        .append(
                                Component.translatable(
                                                "message.otherworldinn.guest.tooltip.preference.humidity",
                                                humidity.min(),
                                                humidity.max())
                                        .withStyle(style -> style.withColor(ModColors.HUMIDITY))),
                Component.literal("  ")
                        .append(
                                Component.translatable(
                                                "message.otherworldinn.guest.tooltip.budget",
                                                guest instanceof VipGuestEntity ? "∞" : guest.getBudget())
                                        .withStyle(style -> style.withColor(ModColors.YELLOW))),
                Component.translatable("message.otherworldinn.guest.tooltip.rewards")
                        .withStyle(style -> style.withColor(ModColors.SUCCESS)),
                Component.literal("  ")
                        .append(
                                Component.translatable("message.otherworldinn.guest.tooltip.rewards.none")
                                        .withStyle(style -> style.withColor(ModColors.GRAY_LIGHT))),
                comfort.min(),
                comfort.max(),
                light.min(),
                light.max(),
                humidity.min(),
                humidity.max(),
                roomAttributes != null ? roomAttributes.comfort() : null,
                roomAttributes != null ? roomAttributes.light() : null,
                roomAttributes != null ? roomAttributes.humidity() : null,
                rewardIcons);
    }

    private static Component buildGuestTitle(GuestEntity guest) {
        Component base =
                Component.translatable("message.otherworldinn.guest.tooltip.title")
                        .withStyle(style -> style.withColor(ModColors.INFO));
        if (!(guest instanceof VipGuestEntity)) {
            return base;
        }
        return Component.empty()
                .append(base)
                .append(Component.literal("-"))
                .append(
                        Component.translatable("message.otherworldinn.guest.tooltip.vip")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    private static void renderUnifiedPanel(
            PanelPainter painter, PanelData data, int anchorX, int anchorY, boolean centerX, boolean centerY) {
        int attrsWidth =
                Math.max(
                        painter.measureText(data.comfortText()),
                        Math.max(
                                painter.measureText(data.lightText()),
                                painter.measureText(data.humidityText())));
        int plainWidth =
                Math.max(
                        painter.measureText(data.title()),
                        Math.max(painter.measureText(data.budgetText()), painter.measureText(data.rewardsTitle())));
        if (!data.hasRewards()) {
            plainWidth = Math.max(plainWidth, painter.measureText(data.rewardsNoneText()));
        }
        int rewardsWidth =
                data.hasRewards()
                        ? (ICON_SIZE * data.rewardIcons().size()
                                        + ICON_GAP * Math.max(0, data.rewardIcons().size() - 1))
                                + 4
                        : 0;
        int contentWidth = Math.max(Math.max(plainWidth, rewardsWidth), attrsWidth + BAR_GAP + BAR_WIDTH);

        int lineHeight = 9 + LINE_GAP;
        int lineCount = 1 + 3 + 1 + 1;
        int contentHeight = lineCount * lineHeight - LINE_GAP;
        int rewardsAreaHeight = data.hasRewards() ? ICON_SIZE + LINE_GAP : lineHeight;
        contentHeight += rewardsAreaHeight;

        int panelWidth = contentWidth + PANEL_PADDING * 2;
        int panelHeight = contentHeight + PANEL_PADDING * 2;
        int panelX = centerX ? anchorX - panelWidth / 2 : anchorX;
        int panelY = centerY ? anchorY - panelHeight / 2 : anchorY;
        painter.drawRoundedRect(panelX, panelY, panelWidth, panelHeight, ModColors.BLACK_ALPHA_62, Z_PANEL_BG);

        int textX = panelX + PANEL_PADDING;
        int y = panelY + PANEL_PADDING;
        painter.drawText(data.title(), textX, y, ModColors.WHITE, Z_TEXT);
        y += lineHeight;
        y = drawAttributeLineCommon(painter, textX, y, data.comfortText(), data.comfortMin(), data.comfortMax(), ModColors.COMFORT, data.roomComfort());
        y = drawAttributeLineCommon(painter, textX, y, data.lightText(), data.lightMin(), data.lightMax(), ModColors.LIGHT, data.roomLight());
        y = drawAttributeLineCommon(
                painter, textX, y, data.humidityText(), data.humidityMin(), data.humidityMax(), ModColors.HUMIDITY, data.roomHumidity());
        painter.drawText(data.budgetText(), textX, y, ModColors.WHITE, Z_TEXT);
        y += lineHeight;
        painter.drawText(data.rewardsTitle(), textX, y, ModColors.WHITE, Z_TEXT);
        y += lineHeight;
        if (data.hasRewards()) {
            int iconX = textX + 2;
            for (ItemStack rewardIcon : data.rewardIcons()) {
                painter.drawItem(rewardIcon, iconX, y, Z_ITEM);
                iconX += ICON_SIZE + ICON_GAP;
            }
        } else {
            painter.drawText(data.rewardsNoneText(), textX, y, ModColors.WHITE, Z_TEXT);
        }
    }

    private static int drawAttributeLineCommon(
            PanelPainter painter,
            int x,
            int y,
            Component text,
            int min,
            int max,
            int color,
            @Nullable Integer markerValue) {
        painter.drawText(text, x, y, ModColors.WHITE, Z_TEXT);
        int barX = x + painter.measureText(text) + BAR_GAP;
        int barY = y + Math.max(0, (9 - BAR_HEIGHT) / 2);
        drawRangeBarCommon(painter, barX, barY, BAR_WIDTH, BAR_HEIGHT, min, max, color, markerValue);
        return y + 9 + LINE_GAP;
    }

    private static void drawRangeBarCommon(
            PanelPainter painter,
            int x,
            int y,
            int width,
            int height,
            int min,
            int max,
            int color,
            @Nullable Integer markerValue) {
        int start = Math.max(0, Math.min(100, Math.min(min, max)));
        int end = Math.max(0, Math.min(100, Math.max(min, max)));
        painter.drawRoundedRect(x, y, width, height, ModColors.BLACK_DARK_64, Z_BAR_BG);
        if (end > start) {
            int fillStart = x + Math.round(width * (start / 100.0f));
            int fillEnd = x + Math.round(width * (end / 100.0f));
            if (fillEnd <= fillStart) {
                fillEnd = fillStart + 1;
            }
            painter.drawRoundedRect(fillStart, y, fillEnd - fillStart, height, color, Z_BAR_FILL);
        }
        if (markerValue != null) {
            drawBarMarkerCommon(painter, x, y, width, height, markerValue, start, end);
        }
    }

    private static void drawBarMarkerCommon(
            PanelPainter painter,
            int x,
            int y,
            int width,
            int height,
            int value,
            int prefMin,
            int prefMax) {
        int clamped = Math.max(0, Math.min(100, value));
        int markerX = x + Math.round(width * (clamped / 100.0f));
        markerX = Math.max(x, Math.min(x + width - MARKER_WIDTH, markerX));
        int markerY = y - 1;
        int markerHeight = height + MARKER_HEIGHT_EXTRA;
        int markerColor = clamped >= prefMin && clamped <= prefMax ? ModColors.WHITE : MARKER_BAD_COLOR;
        painter.drawRoundedRect(
                markerX + 1, markerY + 1, MARKER_WIDTH, markerHeight, ModColors.BLACK_ALPHA_20, Z_MARKER);
        painter.drawRoundedRect(markerX, markerY, MARKER_WIDTH, markerHeight, markerColor, Z_MARKER);
    }

    private interface PanelPainter {
        int measureText(Component text);

        void drawRoundedRect(int x, int y, int width, int height, int color, float z);

        void drawText(Component text, int x, int y, int color, float z);

        void drawItem(ItemStack stack, int x, int y, float z);
    }

    private record PanelData(
            Component title,
            Component comfortText,
            Component lightText,
            Component humidityText,
            Component budgetText,
            Component rewardsTitle,
            Component rewardsNoneText,
            int comfortMin,
            int comfortMax,
            int lightMin,
            int lightMax,
            int humidityMin,
            int humidityMax,
            @Nullable Integer roomComfort,
            @Nullable Integer roomLight,
            @Nullable Integer roomHumidity,
            List<ItemStack> rewardIcons) {
        private boolean hasRewards() {
            return !rewardIcons.isEmpty();
        }
    }

    @Nullable
    private static RoomAttributes resolveRoomAttributesForMarker(
            Minecraft mc, GuestEntity guest, GuestData data) {
        TeamData team = TeamManager.getInstance().getClientPlayerTeam();
        if (team == null) {
            return null;
        }
        InnData innData = team.getInnData();
        RoomData room = null;
        if (isGuestCheckedIn(data)) {
            room = getCheckedInRoom(innData, guest.getUUID(), data.getRoomId());
        } else {
            ItemStack keyStack = getBoundRoomKeyInHand(mc.player);
            if (!keyStack.isEmpty()) {
                Optional<Integer> roomIdOpt = RoomKeyItem.getBoundRoomId(keyStack);
                if (roomIdOpt.isPresent()) {
                    room = innData.getRoom(roomIdOpt.get());
                    if (room != null) {
                        Optional<UUID> roomUuidOpt = RoomKeyItem.getBoundRoomUUID(keyStack);
                        if (roomUuidOpt.isPresent() && !roomUuidOpt.get().equals(room.getUuid())) {
                            room = null;
                        }
                    }
                }
            }
        }
        if (room == null) {
            return null;
        }
        return new RoomAttributes(room.getComfort(), room.getLight(), room.getHumidity());
    }

    private static boolean isGuestCheckedIn(GuestData data) {
        return data.getState() == GuestData.GuestState.CHECKED_IN || data.getRoomId() != -1;
    }

    @Nullable
    private static RoomData getCheckedInRoom(InnData innData, UUID guestUuid, int roomId) {
        if (roomId != -1) {
            RoomData room = innData.getRoom(roomId);
            if (room != null) {
                return room;
            }
        }
        for (RoomData room : innData.getRooms().values()) {
            if (room.getCurrentGuests().contains(guestUuid)) {
                return room;
            }
        }
        return null;
    }

    private static ItemStack getBoundRoomKeyInHand(@Nullable Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.getItem() instanceof RoomKeyItem && RoomKeyItem.getBoundRoomId(main).isPresent()) {
            return main;
        }
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (off.getItem() instanceof RoomKeyItem && RoomKeyItem.getBoundRoomId(off).isPresent()) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    private static Direction getPanelSidePerpendicular(
            Player player, GuestEntity guest, Direction panelFacing) {
        Direction rightSide = panelFacing.getClockWise();
        Direction leftSide = panelFacing.getCounterClockWise();
        double sideDistance = guest.getBbWidth() * 0.5 + SIDE_OFFSET + PANEL_WORLD_Z_OFFSET;
        double anchorY = guest.getY() + guest.getBbHeight() * 0.5;
        double rightXPos = guest.getX() + rightSide.getStepX() * sideDistance;
        double rightZPos = guest.getZ() + rightSide.getStepZ() * sideDistance;
        double leftXPos = guest.getX() + leftSide.getStepX() * sideDistance;
        double leftZPos = guest.getZ() + leftSide.getStepZ() * sideDistance;

        double rightDistSqr = player.distanceToSqr(rightXPos, anchorY, rightZPos);
        double leftDistSqr = player.distanceToSqr(leftXPos, anchorY, leftZPos);
        return rightDistSqr <= leftDistSqr ? rightSide : leftSide;
    }

    private static boolean isHudFallbackRange(Player player, GuestEntity guest) {
        return player.distanceToSqr(guest) <= HUD_FALLBACK_DISTANCE_SQR;
    }

    private static void drawRoundedPixelRectHud(
            GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int renderColor = ensureVisibleFillColor(color);
        if (width < 3 || height < 3) {
            guiGraphics.fill(x, y, x + width, y + height, renderColor);
            return;
        }
        guiGraphics.fill(x + 1, y, x + width - 1, y + height, renderColor);
        guiGraphics.fill(x, y + 1, x + 1, y + height - 1, renderColor);
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height - 1, renderColor);
    }

    private static void drawRoundedPixelRect(
            PoseStack poseStack, int x, int y, int width, int height, int color, float z) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int renderColor = ensureVisibleFillColor(color);
        if (width < 3 || height < 3) {
            drawSolidRect(poseStack, x, y, x + width, y + height, z, renderColor);
            return;
        }
        drawSolidRect(poseStack, x + 1, y, x + width - 1, y + height, z, renderColor);
        drawSolidRect(poseStack, x, y + 1, x + 1, y + height - 1, z, renderColor);
        drawSolidRect(poseStack, x + width - 1, y + 1, x + width, y + height - 1, z, renderColor);
    }

    private static void drawSolidRect(
            PoseStack poseStack, float x1, float y1, float x2, float y2, float z, int color) {
        float a = ((color >> 24) & 0xFF) / 255.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder =
                tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.addVertex(matrix, x1, y1, z).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, x1, y2, z).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, x2, y2, z).setColor(r, g, b, a);
        bufferBuilder.addVertex(matrix, x2, y1, z).setColor(r, g, b, a);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    private static void drawText(
            Font font,
            MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack,
            Component text,
            float x,
            float y,
            int color,
            float z) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, z);
        font.drawInBatch(
                text,
                x,
                y,
                ensureVisibleFillColor(color),
                false,
                poseStack.last().pose(),
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                0,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void renderItemIcon(
            Minecraft mc,
            MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack,
            ItemStack stack,
            int x,
            int y,
            float z) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.translate(8.0F, 8.0F, 0.0F);
        poseStack.scale(1.0F, -1.0F, 1.0F);
        poseStack.scale(16.0F, 16.0F, 16.0F);
        mc.getItemRenderer()
                .renderStatic(
                        stack,
                        ItemDisplayContext.GUI,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        bufferSource,
                        mc.level,
                        0);
        poseStack.popPose();
    }

    private static int ensureVisibleFillColor(int color) {
        if ((color & 0xFF000000) == 0) {
            return color | 0xFF000000;
        }
        return color;
    }

    private record RoomAttributes(int comfort, int light, int humidity) {}
}
