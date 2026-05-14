package com.otherworldinn.datagen;

import com.otherworldinn.init.ModItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import com.otherworldinn.world.expedition.recipe.ChartComponentRecipe;
import com.otherworldinn.world.expedition.recipe.ChartAttachmentRecipe;
import com.otherworldinn.world.expedition.recipe.ChartDimensionRecipe;

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

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.WRITABLE_BOOK, 1)
                .requires(Items.WRITTEN_BOOK)
                .unlockedBy("has_written_book", has(Items.WRITTEN_BOOK))
                .save(recipeOutput);

        SpecialRecipeBuilder.special(
                        (CraftingBookCategory cat) -> new ChartComponentRecipe(cat))
                .save(recipeOutput, "chart_component");
        SpecialRecipeBuilder.special(
                        (CraftingBookCategory cat) -> new ChartAttachmentRecipe(cat))
                .save(recipeOutput, "chart_attachment");
        SpecialRecipeBuilder.special(
                        (CraftingBookCategory cat) -> new ChartDimensionRecipe(cat))
                .save(recipeOutput, "chart_dimension");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CHART_COMPONENT.get(), 2)
                .requires(Items.PAPER, 4)
                .requires(Items.FEATHER, 2)
                .requires(Items.INK_SAC, 2)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(recipeOutput, "blank_chart_component");
    }
}
