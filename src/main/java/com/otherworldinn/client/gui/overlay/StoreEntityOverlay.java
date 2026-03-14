package com.otherworldinn.client.gui.overlay;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.base.StoreEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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
public class StoreEntityOverlay {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ItemHudOverlay.register(25, (unused) -> shouldShow(), StoreEntityOverlay::render);
    }

    private static boolean shouldShow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return false;
        }

        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof EntityHitResult entityHitResult)) {
            return false;
        }
        return entityHitResult.getEntity() instanceof StoreEntity;
    }

    private static void render(
            GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        ItemHudOverlay.renderMouseActions(
                guiGraphics,
                new ItemHudOverlay.MouseAction(
                        ItemHudOverlay.MouseButton.RIGHT,
                        Component.translatable("message.otherworldinn.store.overlay.open")));
    }
}
