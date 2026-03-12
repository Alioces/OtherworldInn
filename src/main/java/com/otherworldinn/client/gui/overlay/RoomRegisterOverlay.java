package com.otherworldinn.client.gui.overlay;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class RoomRegisterOverlay {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 注册到 HUD 管理器，优先级 10 (较低)
        ItemHudOverlay.register(10, (unused) -> shouldShow(), RoomRegisterOverlay::render);
    }

    private static boolean shouldShow() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return false;

        // 检查玩家是否副手持有房间登记册
        ItemStack offhandItem = player.getItemInHand(InteractionHand.OFF_HAND);
        return offhandItem.is(ModItems.ROOM_REGISTER.get());
    }

    private static void render(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        // [LMB] 删除房间 [RMB] 添加房间
        Component deleteText = Component.translatable("message.otherworldinn.room_register.overlay.delete_room");
        Component addText = Component.translatable("message.otherworldinn.room_register.overlay.add_room");
        
        // 使用通用的渲染方法
        ItemHudOverlay.renderMouseActions(guiGraphics, 
            new ItemHudOverlay.MouseAction(ItemHudOverlay.MouseButton.LEFT, deleteText), // 左键：删除
            new ItemHudOverlay.MouseAction(ItemHudOverlay.MouseButton.RIGHT, addText)      // 右键：添加
        );
    }
}
