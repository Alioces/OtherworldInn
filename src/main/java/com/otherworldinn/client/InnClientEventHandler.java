package com.otherworldinn.client;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

/**
 * 客户端事件处理器
 * <p>
 * 处理仅在客户端发生的逻辑，例如音效播放。
 * </p>
 */
@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
public class InnClientEventHandler {

    /**
     * 监听装备变更事件
     * <p>
     * 当房间登记册进入副手时，播放翻页音效。
     * 这通常发生在玩家将物品从主手切换到副手时。
     * </p>
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player && player.level().isClientSide) {
            // 避免登录时触发
            if (player.tickCount < 10) return;

            // 仅关注副手变化
            if (event.getSlot() == EquipmentSlot.OFFHAND) {
                ItemStack to = event.getTo();
                ItemStack from = event.getFrom();

                // 检查是否切换到了房间登记册
                if (to.is(ModItems.ROOM_REGISTER.get())) {
                    // 避免重复播放 (例如从一个登记册切换到另一个登记册)
                    if (!from.is(ModItems.ROOM_REGISTER.get())) {
                        player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
                    }
                }
            }
        }
    }
}
