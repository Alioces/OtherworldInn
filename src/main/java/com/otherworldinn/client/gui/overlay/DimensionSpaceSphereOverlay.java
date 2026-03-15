package com.otherworldinn.client.gui.overlay;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
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
public class DimensionSpaceSphereOverlay {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ItemHudOverlay.register(13, (unused) -> shouldShow(), DimensionSpaceSphereOverlay::render);
    }

    private static boolean shouldShow() {
        return getHeldSphereType() != null;
    }

    private static String getHeldSphereType() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return null;

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.is(ModItems.SPACE_SPHERE.get())) return "base";
        if (stack.is(ModItems.NETHER_SPACE_SPHERE.get())) return "nether";
        if (stack.is(ModItems.END_SPACE_SPHERE.get())) return "end";

        stack = player.getItemInHand(InteractionHand.OFF_HAND);
        if (stack.is(ModItems.SPACE_SPHERE.get())) return "base";
        if (stack.is(ModItems.NETHER_SPACE_SPHERE.get())) return "nether";
        if (stack.is(ModItems.END_SPACE_SPHERE.get())) return "end";

        return null;
    }

    private static void render(
            GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        String type = getHeldSphereType();
        if (type == null) return;

        Component text;
        ItemHudOverlay.MouseButton button;
        if ("base".equals(type)) {
            text = Component.translatable("message.otherworldinn.space_sphere.overlay.use");
            button = ItemHudOverlay.MouseButton.LEFT;
        } else {
            text =
                    "nether".equals(type)
                            ? Component.translatable(
                                    "message.otherworldinn.nether_space_sphere.overlay.use")
                            : Component.translatable(
                                    "message.otherworldinn.end_space_sphere.overlay.use");
            button = ItemHudOverlay.MouseButton.RIGHT;
        }
        ItemHudOverlay.renderMouseActions(
                guiGraphics,
                new ItemHudOverlay.MouseAction(button, text));
    }
}
