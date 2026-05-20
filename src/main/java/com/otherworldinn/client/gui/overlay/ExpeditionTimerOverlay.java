package com.otherworldinn.client.gui.overlay;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.client.ExpeditionTimerClientManager;
import com.otherworldinn.world.expedition.ExpeditionDimensions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(
        modid = OtherworldInn.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public class ExpeditionTimerOverlay {

    private static final int COLOR_DEFAULT = 0xFFFFFF;
    private static final int COLOR_WARNING = 0xFFAA00;
    private static final int COLOR_DANGER = 0xFF5555;
    private static final long WARNING_SECONDS = 60;
    private static final long DANGER_SECONDS = 20;

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "expedition_timer"),
                ExpeditionTimerOverlay::render);
    }

    private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (!ExpeditionDimensions.isExpeditionDimension(mc.level.dimension())) {
            ExpeditionTimerClientManager.clearDeadline();
            return;
        }

        long remainingTicks = ExpeditionTimerClientManager.getRemainingTicks(mc.level.getGameTime());
        if (remainingTicks < 0) return;

        long totalSeconds = remainingTicks / 20;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        String text = String.format("%d:%02d", minutes, seconds);

        int color;
        if (totalSeconds <= DANGER_SECONDS) {
            color = COLOR_DANGER;
        } else if (totalSeconds <= WARNING_SECONDS) {
            color = COLOR_WARNING;
        } else {
            color = COLOR_DEFAULT;
        }

        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int x = screenWidth / 2;
        int y = 35;

        guiGraphics.drawString(
                font,
                Component.literal(text),
                x - font.width(text) / 2,
                y,
                color,
                true);
    }
}
