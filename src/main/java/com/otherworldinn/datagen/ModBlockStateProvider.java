package com.otherworldinn.datagen;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.BlockDataGenInfo;
import com.otherworldinn.init.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.Map;

/**
 * 方块状态与模型生成器
 * 负责生成 blockstates 和 models/block JSON 文件
 */
public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, OtherworldInn.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (Map.Entry<DeferredBlock<?>, BlockDataGenInfo> entry : ModBlocks.BLOCK_INFOS.entrySet()) {
            DeferredBlock<?> block = entry.getKey();
            BlockDataGenInfo info = entry.getValue();

            if (info.generateModel()) {
                if ("solid".equals(info.renderType())) {
                    simpleBlockWithItem(block.get(), cubeAll(block.get()));
                } else {
                    // 处理 cutout 或 translucent 方块 (使用 cubeAll 但指定 render_type)
                    ModelFile model = models().cubeAll(block.getId().getPath(), blockTexture(block.get()))
                            .renderType(info.renderType());
                    simpleBlockWithItem(block.get(), model);
                }
            }
        }
    }
    
    public void simpleBlockWithItem(Block block, ModelFile model) {
        simpleBlock(block, model);
        simpleBlockItem(block, model);
    }
}

