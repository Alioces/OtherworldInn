package com.otherworldinn.client.gui.overlay;

import com.otherworldinn.OtherworldInn;
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
public class RecipeItemOverlay {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ItemHudOverlay.register(13, (unused) -> shouldShow(), RecipeItemOverlay::render);
    }

    private static boolean shouldShow() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return false;

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (isRecipeItem(stack)) return true;
        stack = player.getItemInHand(InteractionHand.OFF_HAND);
        return isRecipeItem(stack);
    }

    private static boolean isRecipeItem(ItemStack stack) {
        return stack.is(com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems.RECIPE_ITEM.get());
    }

    private static void render(
            GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        ItemHudOverlay.renderMouseActions(
                guiGraphics,
                new ItemHudOverlay.MouseAction(
                        ItemHudOverlay.MouseButton.RIGHT,
                        Component.translatable("message.otherworldinn.recipe_item.overlay.use")));
    }
}
