package com.otherworldinn.world.economy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * 物品售价管理器
 * <p>
 * 用于管理玩家向旅社出售物品的价格。
 * 使用静态 Map 存储
 * </p>
 */
public class ItemSellPriceManager {

    private static final Map<ResourceLocation, Integer> PRICES = new HashMap<>();

    static {
        // 初始化默认价格
        // 农作物
        addPrice("minecraft:wheat", 2);
        addPrice("minecraft:carrot", 2);
        addPrice("minecraft:potato", 2);
        addPrice("minecraft:beetroot", 2);
        addPrice("minecraft:melon_slice", 1);
        addPrice("minecraft:pumpkin", 4);
        addPrice("minecraft:apple", 3);
        
        // 畜牧产品
        addPrice("minecraft:beef", 3);
        addPrice("minecraft:porkchop", 3);
        addPrice("minecraft:chicken", 2);
        addPrice("minecraft:mutton", 2);
        addPrice("minecraft:leather", 4);
        addPrice("minecraft:white_wool", 2);
        addPrice("minecraft:egg", 1);
        
        // 杂项
        addPrice("minecraft:rotten_flesh", 1);
        addPrice("minecraft:bone", 1);
        addPrice("minecraft:string", 1);
        addPrice("minecraft:spider_eye", 2);
        addPrice("minecraft:gunpowder", 3);
        addPrice("minecraft:ender_pearl", 10);
    }

    /**
     * 添加物品售价
     *
     * @param itemId 物品 ID (例如 "minecraft:apple")
     * @param price 售价
     */
    public static void addPrice(String itemId, int price) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl != null) {
            PRICES.put(rl, price);
        }
    }

    /**
     * 获取物品的单价
     *
     * @param itemStack 物品栈
     * @return 单价，如果未定义则返回 0
     */
    public static int getPrice(ItemStack itemStack) {
        if (itemStack.isEmpty()) return 0;
        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        return PRICES.getOrDefault(rl, 0);
    }
}
