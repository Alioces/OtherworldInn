package com.otherworldinn;

import com.mojang.logging.LogUtils;
import com.otherworldinn.foundation.ClientConfig;
import com.otherworldinn.init.ModBlocks;
import com.otherworldinn.init.ModCreativeModeTabs;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.init.ModKeyBindings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

/**
 * 模组主类
 */
@Mod(OtherworldInn.MODID)
public class OtherworldInn {
    public static final String MODID = "otherworldinn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OtherworldInn(IEventBus modEventBus, ModContainer modContainer) {
        // 注册物品和方块
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // 注册配置
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}

