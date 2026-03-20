package com.otherworldinn.datagen;

import com.otherworldinn.init.ModItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(
            PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROOM_KEY.get(), 2)
                .pattern("G ")
                .pattern(" I")
                .define('G', Items.GOLD_INGOT)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT))
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INN_KEY.get(), 1)
                .pattern("G ")
                .pattern(" G")
                .define('G', Items.GOLD_INGOT)
                .unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BED_SHEET.get(), 1)
                .pattern("WWW")
                .define('W', ItemTags.WOOL)
                .unlockedBy("has_wool", has(ItemTags.WOOL))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NETHER_SPACE_SPHERE.get(), 1)
                .pattern(" N ")
                .pattern("NSN")
                .pattern(" N ")
                .define('N', Items.NETHERRACK)
                .define('S', ModItems.SPACE_SPHERE.get())
                .unlockedBy("has_space_sphere", has(ModItems.SPACE_SPHERE.get()))
                .unlockedBy("has_netherrack", has(Items.NETHERRACK))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.END_SPACE_SPHERE.get(), 1)
                .pattern(" E ")
                .pattern("ESE")
                .pattern(" E ")
                .define('E', Items.END_STONE)
                .define('S', ModItems.SPACE_SPHERE.get())
                .unlockedBy("has_space_sphere", has(ModItems.SPACE_SPHERE.get()))
                .unlockedBy("has_end_stone", has(Items.END_STONE))
                .save(recipeOutput);
    }
}
