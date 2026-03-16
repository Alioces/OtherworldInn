package com.otherworldinn.client.gui.overlay;

import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.base.GuestEntity;
import com.otherworldinn.world.inn.GuestData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
public class BroomGuestOverlay {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ItemHudOverlay.register(18, (unused) -> shouldShow(), BroomGuestOverlay::render);
    }

    private static boolean shouldShow() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            return false;
        }
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (!main.is(InitItems.BROOM.get()) && !off.is(InitItems.BROOM.get())) {
            return false;
        }
        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof EntityHitResult entityHitResult)
                || !(entityHitResult.getEntity() instanceof GuestEntity guest)) {
            return false;
        }
        if (guest.getGuestData().getState() != GuestData.GuestState.WAITING) {
            return false;
        }
        double range = player.blockInteractionRange();
        return player.distanceToSqr(guest) <= range * range;
    }

    private static void render(
            GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        ItemHudOverlay.renderMouseActions(
                guiGraphics,
                new ItemHudOverlay.MouseAction(
                        ItemHudOverlay.MouseButton.RIGHT,
                        Component.translatable("message.otherworldinn.broom.overlay.expel")));
    }
}
