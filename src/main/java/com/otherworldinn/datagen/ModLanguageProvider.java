package com.otherworldinn.datagen;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.BlockDataGenInfo;
import com.otherworldinn.foundation.ItemDataGenInfo;
import com.otherworldinn.init.ModBlocks;
import com.otherworldinn.init.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.Map;

/**
 * 语言文件生成器
 * 负责生成 en_us.json 和 zh_cn.json
 */
public class ModLanguageProvider extends LanguageProvider {
    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, OtherworldInn.MODID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        boolean isZh = "zh_cn".equals(locale);
        
        if (isZh) {
            add("itemGroup.otherworldinn", "异界旅社");
            add("key.categories.otherworldinn", "异界旅社");
            add("key.otherworldinn.strategy_mode", "地图视图");
        } else {
            add("itemGroup.otherworldinn", "Otherworld Inn");
            add("key.categories.otherworldinn", "Otherworld Inn");
            add("key.otherworldinn.strategy_mode", "Map View");
        }

        // 生成方块语言键
        for (Map.Entry<DeferredBlock<?>, BlockDataGenInfo> entry : ModBlocks.BLOCK_INFOS.entrySet()) {
            DeferredBlock<?> block = entry.getKey();
            BlockDataGenInfo info = entry.getValue();

            String name = isZh ? info.cnName() : info.enName();
            if (name != null && !name.isEmpty()) {
                add(block.get(), name);
            }
            
            List<String> tooltips = isZh ? info.cnTooltips() : info.enTooltips();
            for (int i = 0; i < tooltips.size(); i++) {
                add(block.get().getDescriptionId() + ".tooltip." + i, tooltips.get(i));
            }
        }

        // 生成物品语言键
        for (Map.Entry<DeferredItem<?>, ItemDataGenInfo> entry : ModItems.ITEM_INFOS.entrySet()) {
            DeferredItem<?> item = entry.getKey();
            ItemDataGenInfo info = entry.getValue();

            String name = isZh ? info.cnName() : info.enName();
            if (name != null && !name.isEmpty()) {
                add(item.get(), name);
            }
            
            List<String> tooltips = isZh ? info.cnTooltips() : info.enTooltips();
            for (int i = 0; i < tooltips.size(); i++) {
                add(item.get().getDescriptionId() + ".tooltip." + i, tooltips.get(i));
            }
        }
    }
}

