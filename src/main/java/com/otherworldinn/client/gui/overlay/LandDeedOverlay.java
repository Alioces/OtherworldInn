package com.otherworldinn.client.gui.overlay;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import com.otherworldinn.item.LandDeedItem;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class LandDeedOverlay {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "land_deed_overlay"), (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) return;

            // 检查玩家是否主手或副手持有地契
            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            boolean holding = stack.is(ModItems.LAND_DEED.get());
            if (!holding) {
                stack = player.getItemInHand(InteractionHand.OFF_HAND);
                holding = stack.is(ModItems.LAND_DEED.get());
            }

            if (!holding) {
                return;
            }

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            Font font = mc.font;
            int centerX = screenWidth / 2;
            int startY = 60;
            int iconSize = 16;
            int padding = 4;

            // 根据状态显示提示
            // 状态：Pos1 未定 -> Pos2 未定 -> 确认
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            
            Component text;
            int color = 0xFFFFFF;
            
            boolean hasPos1 = tag.contains("Pos1");
            
            if (!hasPos1) {
                // [RMB] 设置第一点
                text = Component.translatable("message.otherworldinn.land_deed.overlay.set_pos1");
            } else if (!tag.contains("Pos2")) {
                // [RMB] 设置第二点 (显示价格)
                BlockPos pos1 = BlockPos.of(tag.getLong("Pos1"));
                
                // 实时获取光标位置
                BlockPos pos2 = null;
                HitResult hitResult = mc.hitResult;
                if (hitResult instanceof BlockHitResult blockHitResult) {
                    pos2 = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
                }
                
                if (pos2 != null) {
                    TeamData team = TeamManager.getInstance().getClientPlayerTeam();
                    
                    // 检查是否超出最大范围
                    if (!LandDeedItem.isWithinBounds(pos1, pos2)) {
                        text = Component.translatable("message.otherworldinn.land_deed.fail_out_of_bounds");
                        color = 0xFF5555; // 红色
                    } else {
                        int price = LandDeedItem.calculatePrice(team, pos1, pos2);
                        int coins = team != null ? team.getCoins() : 0;
                        
                        if (price > coins) {
                            color = 0xFF5555; // 红色
                        }
                        
                        text = Component.translatable("message.otherworldinn.land_deed.overlay.set_pos2_with_cost", price);
                    }
                } else {
                    text = Component.translatable("message.otherworldinn.land_deed.overlay.set_pos2");
                }
            } else {
                // [RMB] 确认扩展 (显示价格)
                BlockPos pos1 = BlockPos.of(tag.getLong("Pos1"));
                BlockPos pos2 = BlockPos.of(tag.getLong("Pos2"));
                
                TeamData team = TeamManager.getInstance().getClientPlayerTeam();
                
                // 检查是否超出最大范围 (双重保险)
                if (!LandDeedItem.isWithinBounds(pos1, pos2)) {
                     text = Component.translatable("message.otherworldinn.land_deed.fail_out_of_bounds");
                     color = 0xFF5555;
                } else {
                    int price = LandDeedItem.calculatePrice(team, pos1, pos2);
                    int coins = team != null ? team.getCoins() : 0;
                    
                    if (price > coins) {
                        color = 0xFF5555; // 红色
                    }
                    
                    text = Component.translatable("message.otherworldinn.land_deed.overlay.confirm_with_cost", price);
                }
            }
            
            int textWidth = font.width(text);
            
            // 如果已设置 Pos1，则显示取消选项
            if (hasPos1) {
                Component cancelText = Component.translatable("message.otherworldinn.land_deed.overlay.cancel");
                int cancelTextWidth = font.width(cancelText);
                
                int totalWidth = iconSize + padding + cancelTextWidth + padding * 3 + iconSize + padding + textWidth;
                int startX = centerX - totalWidth / 2;
                
                // 渲染取消部分 [LMB]
                AllIcons.I_LMB.render(guiGraphics, startX, startY);
                guiGraphics.drawString(font, cancelText, startX + iconSize + padding, startY + (iconSize - font.lineHeight) / 2 + 1, 0xFFFFFF, true);
                
                // 渲染确认/设置部分 [RMB]
                int secondSectionX = startX + iconSize + padding + cancelTextWidth + padding * 3;
                AllIcons.I_RMB.render(guiGraphics, secondSectionX, startY);
                guiGraphics.drawString(font, text, secondSectionX + iconSize + padding, startY + (iconSize - font.lineHeight) / 2 + 1, color, true);
            } else {
                int totalWidth = iconSize + padding + textWidth;
                int startX = centerX - totalWidth / 2;

                // 渲染
                AllIcons.I_RMB.render(guiGraphics, startX, startY);
                guiGraphics.drawString(font, text, startX + iconSize + padding, startY + (iconSize - font.lineHeight) / 2 + 1, color, true);
            }
        });
    }
}
