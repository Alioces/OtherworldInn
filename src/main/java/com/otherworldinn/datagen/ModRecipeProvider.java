package com.otherworldinn.datagen;

import com.otherworldinn.init.ModItems;
import com.otherworldinn.world.expedition.ChartComponentType;
import com.otherworldinn.world.expedition.ExpeditionNbtHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import com.otherworldinn.world.expedition.recipe.ChartAttachmentRecipe;
import com.otherworldinn.world.expedition.recipe.ChartComponentRecipe;
import com.otherworldinn.world.expedition.recipe.ChartDimensionRecipe;

public class ModRecipeProvider extends RecipeProvider {

    private record ComponentCraftEntry(Ingredient corner, Ingredient edge) {
        static ComponentCraftEntry of(Item corner, Item edge) {
            return new ComponentCraftEntry(Ingredient.of(corner), Ingredient.of(edge));
        }
        static ComponentCraftEntry ofCorner(Item corner, Ingredient edge) {
            return new ComponentCraftEntry(Ingredient.of(corner), edge);
        }
    }

    private static final Map<String, ComponentCraftEntry> COMPONENT_CRAFT = new LinkedHashMap<>();

    static {
        COMPONENT_CRAFT.put("surface_world",
                ComponentCraftEntry.of(Items.GRASS_BLOCK, Items.DIRT));
        COMPONENT_CRAFT.put("floating_islands",
                ComponentCraftEntry.of(Items.END_STONE, Items.OBSIDIAN));
        COMPONENT_CRAFT.put("amplified_world",
                ComponentCraftEntry.of(Items.MOSSY_COBBLESTONE, Items.COBBLESTONE));
        COMPONENT_CRAFT.put("cave_world",
                ComponentCraftEntry.of(Items.DEEPSLATE, Items.STONE_PICKAXE));
        COMPONENT_CRAFT.put("nether_cave",
                ComponentCraftEntry.of(Items.NETHER_BRICKS, Items.NETHERRACK));
        COMPONENT_CRAFT.put("end_void",
                ComponentCraftEntry.of(Items.END_STONE_BRICKS, Items.CHORUS_FRUIT));

        COMPONENT_CRAFT.put("plains_biome",
                ComponentCraftEntry.of(Items.SUNFLOWER, Items.WHEAT_SEEDS));
        COMPONENT_CRAFT.put("forests_biome",
                ComponentCraftEntry.of(Items.OAK_SAPLING, Items.OAK_LEAVES));
        COMPONENT_CRAFT.put("taigas_biome",
                ComponentCraftEntry.of(Items.SPRUCE_SAPLING, Items.SPRUCE_LEAVES));
        COMPONENT_CRAFT.put("savannas_biome",
                ComponentCraftEntry.of(Items.ACACIA_SAPLING, Items.ACACIA_LEAVES));
        COMPONENT_CRAFT.put("desert_biome",
                ComponentCraftEntry.of(Items.SAND, Items.CACTUS));
        COMPONENT_CRAFT.put("snowy_biome",
                ComponentCraftEntry.of(Items.SNOWBALL, Items.SNOW_BLOCK));
        COMPONENT_CRAFT.put("jungle_biome",
                ComponentCraftEntry.of(Items.JUNGLE_SAPLING, Items.JUNGLE_LEAVES));
        COMPONENT_CRAFT.put("swamp_biome",
                ComponentCraftEntry.of(Items.LILY_PAD, Items.VINE));
        COMPONENT_CRAFT.put("ocean_biome",
                ComponentCraftEntry.of(Items.WATER_BUCKET, Items.KELP));
        COMPONENT_CRAFT.put("mountain_biome",
                ComponentCraftEntry.of(Items.SNOW_BLOCK, Items.STONE));
        COMPONENT_CRAFT.put("mushroom_biome",
                ComponentCraftEntry.of(Items.RED_MUSHROOM, Items.MYCELIUM));
        COMPONENT_CRAFT.put("dark_forest_biome",
                ComponentCraftEntry.of(Items.DARK_OAK_SAPLING, Items.DARK_OAK_LEAVES));
        COMPONENT_CRAFT.put("sculk_biome",
                ComponentCraftEntry.of(Items.SCULK_CATALYST, Items.SCULK));

        COMPONENT_CRAFT.put("nether_wastes_biome",
                ComponentCraftEntry.of(Items.NETHERRACK, Items.NETHER_QUARTZ_ORE));
        COMPONENT_CRAFT.put("crimson_biome",
                ComponentCraftEntry.of(Items.CRIMSON_FUNGUS, Items.CRIMSON_NYLIUM));
        COMPONENT_CRAFT.put("warped_biome",
                ComponentCraftEntry.of(Items.WARPED_FUNGUS, Items.WARPED_NYLIUM));
        COMPONENT_CRAFT.put("basalt_biome",
                ComponentCraftEntry.of(Items.BASALT, Items.MAGMA_BLOCK));
        COMPONENT_CRAFT.put("soul_valley_biome",
                ComponentCraftEntry.of(Items.SOUL_SAND, Items.SOUL_SOIL));

        COMPONENT_CRAFT.put("end_highlands_biome",
                ComponentCraftEntry.of(Items.CHORUS_FLOWER, Items.END_STONE));
        COMPONENT_CRAFT.put("end_islands_biome",
                ComponentCraftEntry.of(Items.CHORUS_FRUIT, Items.POPPED_CHORUS_FRUIT));

        COMPONENT_CRAFT.put("stone_base",
                ComponentCraftEntry.of(Items.STONE, Items.COBBLESTONE));
        COMPONENT_CRAFT.put("deepslate_base",
                ComponentCraftEntry.of(Items.POLISHED_DEEPSLATE, Items.COBBLED_DEEPSLATE));
        COMPONENT_CRAFT.put("granite_base",
                ComponentCraftEntry.of(Items.GRANITE, Items.POLISHED_GRANITE));
        COMPONENT_CRAFT.put("andesite_base",
                ComponentCraftEntry.of(Items.ANDESITE, Items.POLISHED_ANDESITE));
        COMPONENT_CRAFT.put("diorite_base",
                ComponentCraftEntry.of(Items.DIORITE, Items.POLISHED_DIORITE));
        COMPONENT_CRAFT.put("sandstone_base",
                ComponentCraftEntry.of(Items.SANDSTONE, Items.CHISELED_SANDSTONE));
        COMPONENT_CRAFT.put("tuff_base",
                ComponentCraftEntry.of(Items.TUFF, Items.POLISHED_TUFF));

        COMPONENT_CRAFT.put("thunderstorm",
                ComponentCraftEntry.of(Items.TRIDENT, Items.LIGHTNING_ROD));
        COMPONENT_CRAFT.put("eternal_day",
                ComponentCraftEntry.of(Items.GLOWSTONE_DUST, Items.TORCH));
        COMPONENT_CRAFT.put("eternal_night",
                ComponentCraftEntry.of(Items.CLOCK, Items.OBSIDIAN));
        COMPONENT_CRAFT.put("thriving_realm",
                ComponentCraftEntry.of(Items.EMERALD, Items.GOLD_INGOT));
        COMPONENT_CRAFT.put("lava_flood",
                ComponentCraftEntry.of(Items.LAVA_BUCKET, Items.MAGMA_BLOCK));
        COMPONENT_CRAFT.put("gravity_low",
                ComponentCraftEntry.of(Items.FEATHER, Items.STRING));
        COMPONENT_CRAFT.put("dry_land",
                ComponentCraftEntry.of(Items.DEAD_BUSH, Items.SAND));
        COMPONENT_CRAFT.put("water_world",
                ComponentCraftEntry.of(Items.TROPICAL_FISH, Items.PRISMARINE_SHARD));
        COMPONENT_CRAFT.put("one_hp",
                ComponentCraftEntry.of(Items.GOLDEN_APPLE, Items.TOTEM_OF_UNDYING));
        COMPONENT_CRAFT.put("universal_anger",
                ComponentCraftEntry.of(Items.IRON_SWORD, Items.ROTTEN_FLESH));
        COMPONENT_CRAFT.put("eternal_rain",
                ComponentCraftEntry.of(Items.CAULDRON, Items.WATER_BUCKET));
        COMPONENT_CRAFT.put("insomniacs",
                ComponentCraftEntry.ofCorner(Items.PHANTOM_MEMBRANE,
                        Ingredient.of(ItemTags.WOOL)));
        COMPONENT_CRAFT.put("no_drops",
                ComponentCraftEntry.of(Items.BONE, Items.ROTTEN_FLESH));
        COMPONENT_CRAFT.put("fish_out_of_water",
                ComponentCraftEntry.of(Items.COD, Items.SALMON));
        COMPONENT_CRAFT.put("wednesday_frogs",
                ComponentCraftEntry.of(Items.FROGSPAWN, Items.SLIME_BALL));
    }

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

        for (var entry : COMPONENT_CRAFT.entrySet()) {
            String componentId = entry.getKey();
            ComponentCraftEntry materials = entry.getValue();
            ChartComponentType type = ChartComponentType.byId(componentId);
            if (type == null) continue;

            ItemStack result = new ItemStack(ModItems.CHART_COMPONENT.get());
            CompoundTag tag = ExpeditionNbtHelper.readTag(result);
            tag.putString("component_type", componentId);
            ExpeditionNbtHelper.writeTag(result, tag);

            Map<Character, Ingredient> keys = Map.of(
                    'C', materials.corner(),
                    'E', materials.edge(),
                    'B', Ingredient.of(ModItems.CHART_COMPONENT.get()));

            ShapedRecipePattern pattern = ShapedRecipePattern.of(
                    keys,
                    List.of("CEC", "EBE", "CEC"));

            ShapedRecipe recipe = new ShapedRecipe("", CraftingBookCategory.MISC,
                    pattern, result);

            recipeOutput.accept(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            "otherworldinn", "chart_component_" + componentId),
                    recipe,
                    null);
        }
    }
}
