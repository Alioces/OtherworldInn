package com.otherworldinn.client.gui.overlay;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.item.ExpeditionChartItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
        modid = OtherworldInn.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public class ExpeditionChartOverlay {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ItemHudOverlay.register(13, (unused) -> shouldShow(), ExpeditionChartOverlay::render);
    }

    private static boolean shouldShow() {
        return !getHeldRelevantStack().isEmpty();
    }

    private static ItemStack getHeldRelevantStack() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (isRelevant(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (isRelevant(offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static boolean isRelevant(ItemStack stack) {
        return stack.is(ModItems.PIONEER_CHART.get());
    }

    private static void render(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        ItemStack stack = getHeldRelevantStack();
        if (stack.isEmpty()) {
            return;
        }

        renderChart(guiGraphics, stack);
    }

    private static void renderChart(GuiGraphics guiGraphics, ItemStack stack) {
        String state = ExpeditionChartItem.getChartState(stack);
        if ("recruiting".equals(state)) {
            ItemHudOverlay.renderMouseActions(
                    guiGraphics,
                    new ItemHudOverlay.MouseAction(
                            ItemHudOverlay.MouseButton.LEFT,
                            Component.translatable("message.otherworldinn.expedition.overlay.cancel")),
                    new ItemHudOverlay.MouseAction(
                            ItemHudOverlay.MouseButton.RIGHT,
                            Component.translatable("message.otherworldinn.expedition.overlay.launch")));
            return;
        }

        ItemHudOverlay.renderMouseActions(
                guiGraphics,
                new ItemHudOverlay.MouseAction(
                        ItemHudOverlay.MouseButton.RIGHT,
                        Component.translatable("message.otherworldinn.expedition.overlay.use")));
    }
}
