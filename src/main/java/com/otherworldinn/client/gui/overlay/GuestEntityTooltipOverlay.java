package com.otherworldinn.client.gui.overlay;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.base.GuestEntity;
import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.world.inn.GuestData;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.inn.RoomData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import com.otherworldinn.item.RoomKeyItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
        modid = OtherworldInn.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
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


    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ItemHudOverlay.register(35, (unused) -> shouldShow(), GuestEntityTooltipOverlay::render);
    }

    private static boolean shouldShow() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.screen == null && getTargetGuest(mc) != null;
    }

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

    private static void render(
            GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        GuestEntity guest = getTargetGuest(mc);
        if (guest == null) {
            return;
        }

        Font font = mc.font;
        GuestData data = guest.getGuestData();
        GuestData.IntRange comfort = data.getComfortPreference();
        GuestData.IntRange light = data.getLightPreference();
        GuestData.IntRange humidity = data.getHumidityPreference();
        RoomAttributes roomAttributes = resolveRoomAttributesForMarker(mc, guest, data);

        Component title =
                Component.translatable("message.otherworldinn.guest.tooltip.title")
                        .withStyle(style -> style.withColor(ModColors.INFO));
        Component comfortText =
                Component.literal("  ")
                        .append(
                                Component.translatable(
                                                "message.otherworldinn.guest.tooltip.preference.comfort",
                                                comfort.min(),
                                                comfort.max())
                                        .withStyle(style -> style.withColor(ModColors.COMFORT)));
        Component lightText =
                Component.literal("  ")
                        .append(
                                Component.translatable(
                                                "message.otherworldinn.guest.tooltip.preference.light",
                                                light.min(),
                                                light.max())
                                        .withStyle(style -> style.withColor(ModColors.LIGHT)));
        Component humidityText =
                Component.literal("  ")
                        .append(
                                Component.translatable(
                                                "message.otherworldinn.guest.tooltip.preference.humidity",
                                                humidity.min(),
                                                humidity.max())
                                        .withStyle(style -> style.withColor(ModColors.HUMIDITY)));
        Component budgetText =
                Component.literal("  ")
                        .append(
                                Component.translatable(
                                                "message.otherworldinn.guest.tooltip.budget",
                                                guest.getBudget())
                                        .withStyle(style -> style.withColor(ModColors.YELLOW)));
        Component rewardsTitle =
                Component.translatable("message.otherworldinn.guest.tooltip.rewards")
                        .withStyle(style -> style.withColor(ModColors.SUCCESS));

        List<ItemStack> rewardIcons = new java.util.ArrayList<>();
        boolean hasRewards = false;
        if (data.getRewardItems().isEmpty()) {
            hasRewards = false;
        } else {
            hasRewards = true;
            for (GuestData.RewardItem reward : data.getRewardItems()) {
                Optional<net.minecraft.world.item.Item> itemOptional =
                        BuiltInRegistries.ITEM.getOptional(reward.item());
                itemOptional.ifPresent(item -> rewardIcons.add(new ItemStack(item)));
            }
        }

        int attrsWidth =
                Math.max(
                        font.width(comfortText),
                        Math.max(font.width(lightText), font.width(humidityText)));
        int plainWidth =
                Math.max(font.width(title), Math.max(font.width(budgetText), font.width(rewardsTitle)));
        int rewardsWidth = hasRewards ? (ICON_SIZE * rewardIcons.size() + ICON_GAP * Math.max(0, rewardIcons.size() - 1)) + 4 : 0;
        int contentWidth = Math.max(Math.max(plainWidth, rewardsWidth), attrsWidth + BAR_GAP + BAR_WIDTH);

        int lineHeight = font.lineHeight + LINE_GAP;
        int lineCount = 1 + 3 + 1 + 1;
        int contentHeight = lineCount * lineHeight - LINE_GAP;
        int rewardsAreaHeight = hasRewards ? ICON_SIZE + LINE_GAP : 0;
        contentHeight += rewardsAreaHeight;

        int panelX = guiGraphics.guiWidth() / 2 + 12;
        int panelY = guiGraphics.guiHeight() / 2 - 12;
        int panelWidth = contentWidth + PANEL_PADDING * 2;
        int panelHeight = contentHeight + PANEL_PADDING * 2;
        drawRoundedPixelRect(guiGraphics, panelX, panelY, panelWidth, panelHeight, ModColors.BLACK_ALPHA_62);

        int textX = panelX + PANEL_PADDING;
        int y = panelY + PANEL_PADDING;
        guiGraphics.drawString(font, title, textX, y, ModColors.WHITE, false);
        y += lineHeight;

        y = drawAttributeLine(
                guiGraphics,
                font,
                textX,
                y,
                comfortText,
                comfort.min(),
                comfort.max(),
                ModColors.COMFORT,
                roomAttributes != null ? roomAttributes.comfort() : null);
        y = drawAttributeLine(
                guiGraphics,
                font,
                textX,
                y,
                lightText,
                light.min(),
                light.max(),
                ModColors.LIGHT,
                roomAttributes != null ? roomAttributes.light() : null);
        y = drawAttributeLine(
                guiGraphics,
                font,
                textX,
                y,
                humidityText,
                humidity.min(),
                humidity.max(),
                ModColors.HUMIDITY,
                roomAttributes != null ? roomAttributes.humidity() : null);

        guiGraphics.drawString(font, budgetText, textX, y, ModColors.WHITE, false);
        y += lineHeight;
        guiGraphics.drawString(font, rewardsTitle, textX, y, ModColors.WHITE, false);
        y += lineHeight;
        if (hasRewards) {
            int iconY = y;
            int iconX = textX + 2;
            for (ItemStack rewardIcon : rewardIcons) {
                guiGraphics.renderItem(rewardIcon, iconX, iconY);
                iconX += ICON_SIZE + ICON_GAP;
            }
        }
    }

    private static int drawAttributeLine(
            GuiGraphics guiGraphics,
            Font font,
            int x,
            int y,
            Component text,
            int min,
            int max,
            int color,
            @Nullable Integer markerValue) {
        guiGraphics.drawString(font, text, x, y, ModColors.WHITE, false);
        int barX = x + font.width(text) + BAR_GAP;
        int barY = y + Math.max(0, (font.lineHeight - BAR_HEIGHT) / 2);
        drawRangeBar(guiGraphics, barX, barY, BAR_WIDTH, BAR_HEIGHT, min, max, color, markerValue);
        return y + font.lineHeight + LINE_GAP;
    }

    private static void drawRangeBar(
            GuiGraphics guiGraphics,
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
        drawRoundedPixelRect(guiGraphics, x + 1, y + 1, width, height, ModColors.BLACK_ALPHA_20);
        drawRoundedPixelRect(guiGraphics, x, y, width, height, ModColors.BLACK_DARK_64);
        if (end <= start) {
            return;
        }
        int fillStart = x + Math.round(width * (start / 100.0f));
        int fillEnd = x + Math.round(width * (end / 100.0f));
        if (fillEnd <= fillStart) {
            fillEnd = fillStart + 1;
        }
        drawRoundedPixelRect(guiGraphics, fillStart, y, fillEnd - fillStart, height, color);
        if (markerValue != null) {
            drawBarMarker(guiGraphics, x, y, width, height, markerValue, start, end);
        }
    }

    private static void drawBarMarker(
            GuiGraphics guiGraphics,
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
        boolean inRange = clamped >= prefMin && clamped <= prefMax;
        int markerColor = inRange ? ModColors.WHITE : MARKER_BAD_COLOR;
        drawRoundedPixelRect(
                guiGraphics,
                markerX + 1,
                markerY + 1,
                MARKER_WIDTH,
                markerHeight,
                ModColors.BLACK_ALPHA_20);
        drawRoundedPixelRect(guiGraphics, markerX, markerY, MARKER_WIDTH, markerHeight, markerColor);
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

    private static void drawRoundedPixelRect(
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

    private static int ensureVisibleFillColor(int color) {
        if ((color & 0xFF000000) == 0) {
            return color | 0xFF000000;
        }
        return color;
    }

    private record RoomAttributes(int comfort, int light, int humidity) {}
}
