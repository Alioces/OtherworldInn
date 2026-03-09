package com.otherworldinn.datagen;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.ItemDataGenInfo;
import com.otherworldinn.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Map;

/**
 * 物品模型生成器
 * 负责生成 models/item JSON 文件
 */
public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, OtherworldInn.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // 大多数方块物品的模型由 ModBlockStateProvider.simpleBlockWithItem 处理
        // 此处主要处理独立物品
        
        // 注册 ModItems 中的物品
        for (Map.Entry<DeferredItem<?>, ItemDataGenInfo> entry : ModItems.ITEM_INFOS.entrySet()) {
            DeferredItem<?> item = entry.getKey();
            ItemDataGenInfo info = entry.getValue();

            if (info.generateModel()) {
                if ("handheld".equals(info.modelType())) {
                    handheldItem(item);
                } else {
                    simpleItem(item);
                }
            }
        }
    }
    
    private void simpleItem(DeferredItem<?> item) {
        withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "item/" + item.getId().getPath()));
    }

    private void handheldItem(DeferredItem<?> item) {
        withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "item/" + item.getId().getPath()));
    }
}

