package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.ItemDataGenInfo;
import com.otherworldinn.foundation.ItemReg;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.otherworldinn.item.RecallScrollItem;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 物品注册中心
 * <p>
 * 负责注册模组中的所有物品。
 */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OtherworldInn.MODID);

    /** 存储物品的数据生成信息，用于 DataGen */
    public static final Map<DeferredItem<?>, ItemDataGenInfo> ITEM_INFOS = new HashMap<>();

    // --- 物品注册 ---

    public static final ItemReg<RecallScrollItem> RECALL_SCROLL_REG = new ItemReg<>("recall_scroll", RecallScrollItem::new)
            .rarity(Rarity.EPIC)
            .stacksTo(1)
            .fireResistant()
            .lang("Recall Scroll", "回程卷轴");
    public static final DeferredItem<RecallScrollItem> RECALL_SCROLL = RECALL_SCROLL_REG.register();

    // --- 辅助方法 ---

    /**
     * 开始一个物品的链式注册 (自定义物品类)
     *
     * @param name    物品注册名
     * @param factory 物品工厂方法 (例如 CustomItem::new)
     * @param <T>     物品类型
     * @return ItemReg 构建器
     */
    public static <T extends Item> ItemReg<T> register(String name, Function<Item.Properties, T> factory) {
        return new ItemReg<>(name, factory);
    }
    
    /**
     * 开始一个普通物品的链式注册 (使用默认 Item 类)
     *
     * @param name 物品注册名
     * @return ItemReg 构建器
     */
    public static ItemReg<Item> register(String name) {
        return new ItemReg<>(name, Item::new);
    }
}

