package com.otherworldinn.client.gui.overlay;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class RoomRegisterOverlay {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "room_register_overlay"), (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) return;

            // 检查玩家是否副手持有房间登记册
            ItemStack offhandItem = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!offhandItem.is(ModItems.ROOM_REGISTER.get())) {
                return;
            }

            // 如果主手持有地契，优先显示地契的 HUD
            if (player.getMainHandItem().is(ModItems.LAND_DEED.get())) {
                return;
            }

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            
            Font font = mc.font;
            int centerX = screenWidth / 2;
            int startY = 60;
            int lineHeight = 16;
            int iconSize = 16;
            int padding = 4;

            // [LMB] 删除房间 [RMB] 添加房间
            Component deleteText = Component.translatable("message.otherworldinn.room_register.overlay.delete_room");
            Component addText = Component.translatable("message.otherworldinn.room_register.overlay.add_room");
            
            int deleteTextWidth = font.width(deleteText);
            int addTextWidth = font.width(addText);
            
            int totalWidth = iconSize + padding + deleteTextWidth + padding * 3 + iconSize + padding + addTextWidth;
            int startX = centerX - totalWidth / 2;

            // 渲染删除房间部分
            AllIcons.I_LMB.render(guiGraphics, startX, startY);
            guiGraphics.drawString(font, deleteText, startX + iconSize + padding, startY + (iconSize - font.lineHeight) / 2 + 1, 0xFFFFFF, true);
            
            // 渲染添加房间部分
            int secondSectionX = startX + iconSize + padding + deleteTextWidth + padding * 3;
            AllIcons.I_RMB.render(guiGraphics, secondSectionX, startY);
            guiGraphics.drawString(font, addText, secondSectionX + iconSize + padding, startY + (iconSize - font.lineHeight) / 2 + 1, 0xFFFFFF, true);
        });
    }
}
