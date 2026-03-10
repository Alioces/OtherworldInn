package com.otherworldinn.datagen;

import com.otherworldinn.OtherworldInn;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * DataGen 入口类
 * <p>
 * 监听 GatherDataEvent 事件以注册各种数据提供者（Provider）。
 * 包含客户端（模型、语言）和服务端（标签、战利品表）数据的生成。
 */
@EventBusSubscriber(modid = OtherworldInn.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        var lookupProvider = event.getLookupProvider();

        // 客户端数据提供者 (Client Providers)
        
        // BlockState & Block Models
        event.getGenerator().addProvider(
                event.includeClient(),
                new ModBlockStateProvider(packOutput, existingFileHelper)
        );

        // Item Models
        event.getGenerator().addProvider(
                event.includeClient(),
                new ModItemModelProvider(packOutput, existingFileHelper)
        );

        // Languages (EN_US)
        event.getGenerator().addProvider(
                event.includeClient(),
                new ModLanguageProvider(packOutput, "en_us")
        );

        // Languages (ZH_CN)
        event.getGenerator().addProvider(
                event.includeClient(),
                new ModLanguageProvider(packOutput, "zh_cn")
        );
        
        // 服务端数据提供者 (Server Providers)
        
        // Block Tags
        ModBlockTagProvider blockTagProvider = new ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper);
        event.getGenerator().addProvider(
                event.includeServer(),
                blockTagProvider
        );
        
        // Item Tags
        event.getGenerator().addProvider(
                event.includeServer(),
                new ModItemTagProvider(packOutput, lookupProvider, blockTagProvider.contentsGetter(), existingFileHelper)
        );
        
        // Loot Tables
        event.getGenerator().addProvider(
                event.includeServer(),
                new ModLootTableProvider(packOutput, lookupProvider)
        );
    }
}

