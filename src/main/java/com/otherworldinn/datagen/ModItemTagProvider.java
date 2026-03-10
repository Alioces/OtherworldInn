package com.otherworldinn.datagen;

import com.otherworldinn.OtherworldInn;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * 物品标签生成器
 * <p>
 * 负责生成 tags/item JSON 文件。
 */
public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, OtherworldInn.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // 定义“城镇维度禁用”标签
        tag(OtherworldInn.BANNED_IN_TOWN)
                .add(Items.TNT)
                .add(Items.FLINT_AND_STEEL)
                .add(Items.LAVA_BUCKET)
                .add(Items.END_CRYSTAL)
                //.addOptional(ResourceLocation.parse("create:cart_assembler"))
                //.addOptional(ResourceLocation.parse("create:super_glue"))
                //.addOptional(ResourceLocation.parse("create:mechanical_piston"))
                //.addOptional(ResourceLocation.parse("create:sticky_mechanical_piston"))
                //.addOptional(ResourceLocation.parse("create:mechanical_bearing"))
                //.addOptional(ResourceLocation.parse("create:clockwork_bearing"))
                //.addOptional(ResourceLocation.parse("create:gantry_carriage"))
                ;
    }
}
