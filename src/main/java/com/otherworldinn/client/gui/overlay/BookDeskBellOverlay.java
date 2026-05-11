package com.otherworldinn.client.gui.overlay;

import com.otherworldinn.OtherworldInn;
import com.simibubi.create.content.redstone.deskBell.DeskBellBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
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
public class BookDeskBellOverlay {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ItemHudOverlay.register(35, (unused) -> shouldShow(), BookDeskBellOverlay::render);
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
        BlockEntity be = mc.level.getBlockEntity(blockHitResult.getBlockPos());
        if (!(be instanceof DeskBellBlockEntity)) {
            return false;
        }
        Player player = mc.player;
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        return isBookItem(main) || isBookItem(off);
    }

    private static boolean isBookItem(ItemStack stack) {
        return stack.is(Items.BOOK)
                || stack.is(Items.WRITABLE_BOOK)
                || stack.is(Items.WRITTEN_BOOK);
    }

    private static void render(
            net.minecraft.client.gui.GuiGraphics guiGraphics,
            net.minecraft.client.DeltaTracker deltaTracker) {
        ItemHudOverlay.renderMouseActions(
                guiGraphics,
                new ItemHudOverlay.MouseAction(
                        ItemHudOverlay.MouseButton.RIGHT,
                        Component.translatable("message.otherworldinn.book.desk_bell")));
    }
}
