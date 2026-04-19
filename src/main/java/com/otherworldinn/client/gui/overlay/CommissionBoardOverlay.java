package com.otherworldinn.client.gui.overlay;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
        modid = OtherworldInn.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public class CommissionBoardOverlay {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ItemHudOverlay.register(30, (unused) -> shouldShow(), CommissionBoardOverlay::render);
    }

    private static boolean shouldShow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return false;
        }
        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return false;
        }
        return mc.level.getBlockState(blockHitResult.getBlockPos()).is(ModBlocks.COMMISSION_BOARD.get());
    }

    private static void render(
            net.minecraft.client.gui.GuiGraphics guiGraphics,
            net.minecraft.client.DeltaTracker deltaTracker) {
        ItemHudOverlay.renderMouseActions(
                guiGraphics,
                new ItemHudOverlay.MouseAction(
                        ItemHudOverlay.MouseButton.LEFT,
                        Component.translatable("message.otherworldinn.commission.overlay.submit")),
                new ItemHudOverlay.MouseAction(
                        ItemHudOverlay.MouseButton.RIGHT,
                        Component.translatable("message.otherworldinn.commission.overlay.view")));
    }
}
