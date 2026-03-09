package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式选项卡注册中心
 */
public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OtherworldInn.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OTHERWORLD_INN_TAB = CREATIVE_MODE_TABS.register("otherworld_inn_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.otherworldinn"))
            .icon(() -> new ItemStack(ModItems.ITEMS.getEntries().stream().findFirst().map(deferredItem -> deferredItem.get()).orElse(net.minecraft.world.item.Items.AIR))) // 默认使用第一个注册的物品作为图标，稍后可修改
            .displayItems((parameters, output) -> {
                // 在这里手动添加物品以控制顺序
                // output.accept(ModItems.EXAMPLE_ITEM.get());
                // output.accept(ModBlocks.EXAMPLE_BLOCK.get());
            }).build());
}
