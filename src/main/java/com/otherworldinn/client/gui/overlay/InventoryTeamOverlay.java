package com.otherworldinn.client.gui.overlay;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
public class InventoryTeamOverlay {
    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_HEIGHT = 166;
    private static final int CONTENT_INSET = 6;
    private static final int BAR_WIDTH = INVENTORY_WIDTH;
    private static final int BAR_HEIGHT = 16;
    private static final int RATING_ROWS = 6;
    private static final ResourceLocation RATING_BACKGROUND_ATLAS =
            ResourceLocation.fromNamespaceAndPath(
                    OtherworldInn.MODID, "textures/gui/overlay/inventory_team_bg_atlas.png");

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        TeamData team = TeamManager.getInstance().getClientPlayerTeam();
        if (team == null) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int guiLeft = (guiGraphics.guiWidth() - INVENTORY_WIDTH) / 2;
        int guiTop = (guiGraphics.guiHeight() - INVENTORY_HEIGHT) / 2;
        int barLeft = guiLeft;
        int contentLeft = barLeft + CONTENT_INSET;
        int contentRight = barLeft + BAR_WIDTH - CONTENT_INSET;
        int barY = guiTop - BAR_HEIGHT;
        int y = guiTop - 10;

        int displayRating = Math.max(0, team.getInnData().getRating());
        Component ratingText =
                displayRating == 0
                        ? Component.literal("0")
                        : Component.literal(String.valueOf('\uE005').repeat(displayRating));
        int clampedRating = Math.min(5, displayRating);
        int atlasV = clampedRating * BAR_HEIGHT;
        guiGraphics.blit(
                RATING_BACKGROUND_ATLAS,
                barLeft,
                barY,
                0,
                atlasV,
                BAR_WIDTH,
                BAR_HEIGHT,
                BAR_WIDTH,
                BAR_HEIGHT * RATING_ROWS);
        Component coinsText =
                Component.translatable("message.otherworldinn.inventory.overlay.coins", team.getCoins());
        int coinsX = contentRight - mc.font.width(coinsText);

        guiGraphics.drawString(mc.font, ratingText, contentLeft, y, ModColors.WHITE, false);
        guiGraphics.drawString(
                mc.font,
                coinsText,
                coinsX,
                y,
                ModColors.WHITE,
                true);

        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();
        int ratingWidth = mc.font.width(ratingText);
        int lineHeight = mc.font.lineHeight;
        if (mouseX >= contentLeft
                && mouseX <= contentLeft + ratingWidth
                && mouseY >= y
                && mouseY <= y + lineHeight) {
            Component tooltip =
                    Component.translatable(
                            "message.otherworldinn.inventory.overlay.reputation_detail",
                            team.getInnData().getReputation(),
                            team.getInnData().getMaxReputation(displayRating));
            guiGraphics.renderTooltip(mc.font, tooltip, mouseX, mouseY);
            return;
        }

        int coinsWidth = mc.font.width(coinsText);
        if (mouseX >= coinsX
                && mouseX <= coinsX + coinsWidth
                && mouseY >= y
                && mouseY <= y + lineHeight) {
            InnData innData = team.getInnData();
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(
                    Component.translatable("message.otherworldinn.inventory.overlay.income.title")
                            .withStyle(ChatFormatting.GOLD));
            tooltip.add(
                    Component.translatable("message.otherworldinn.inventory.overlay.income.total")
                            .withStyle(ChatFormatting.YELLOW));
            tooltip.add(
                    Component.literal("  ")
                            .append(
                                    Component.translatable(
                                                    "message.otherworldinn.inventory.overlay.income.lodging",
                                                    innData.getTotalLodgingIncome())
                                            .withStyle(ChatFormatting.GRAY)));
            tooltip.add(
                    Component.literal("  ")
                            .append(
                                    Component.translatable(
                                                    "message.otherworldinn.inventory.overlay.income.dining",
                                                    innData.getTotalDiningIncome())
                                            .withStyle(ChatFormatting.GRAY)));
            tooltip.add(
                    Component.translatable("message.otherworldinn.inventory.overlay.income.yesterday")
                            .withStyle(ChatFormatting.AQUA));
            tooltip.add(
                    Component.literal("  ")
                            .append(
                                    Component.translatable(
                                                    "message.otherworldinn.inventory.overlay.income.lodging",
                                                    innData.getYesterdayLodgingIncome())
                                            .withStyle(ChatFormatting.DARK_AQUA)));
            tooltip.add(
                    Component.literal("  ")
                            .append(
                                    Component.translatable(
                                                    "message.otherworldinn.inventory.overlay.income.dining",
                                                    innData.getYesterdayDiningIncome())
                                            .withStyle(ChatFormatting.DARK_AQUA)));
            List<FormattedCharSequence> tooltipLines = new ArrayList<>();
            for (Component line : tooltip) {
                tooltipLines.add(Language.getInstance().getVisualOrder(line));
            }
            guiGraphics.renderTooltip(mc.font, tooltipLines, mouseX, mouseY);
        }
    }
}
