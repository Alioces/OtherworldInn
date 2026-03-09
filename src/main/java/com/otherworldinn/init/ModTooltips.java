package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.BlockDataGenInfo;
import com.otherworldinn.foundation.ItemDataGenInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Map;

/**
 * 客户端工具提示处理器
 * 自动为注册的物品和方块添加工具提示
 */
@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
public class ModTooltips {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();

        // 检查物品注册表
        for (Map.Entry<DeferredItem<?>, ItemDataGenInfo> entry : ModItems.ITEM_INFOS.entrySet()) {
            if (entry.getKey().get() == item) {
                int count = entry.getValue().enTooltips().size();
                for (int i = 0; i < count; i++) {
                    event.getToolTip().add(Component.translatable(item.getDescriptionId() + ".tooltip." + i));
                }
                return;
            }
        }

        // 检查方块物品注册表
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            for (Map.Entry<DeferredBlock<?>, BlockDataGenInfo> entry : ModBlocks.BLOCK_INFOS.entrySet()) {
                if (entry.getKey().get() == block) {
                    int count = entry.getValue().enTooltips().size();
                    for (int i = 0; i < count; i++) {
                        event.getToolTip().add(Component.translatable(block.getDescriptionId() + ".tooltip." + i));
                    }
                    return;
                }
            }
        }
    }
}

