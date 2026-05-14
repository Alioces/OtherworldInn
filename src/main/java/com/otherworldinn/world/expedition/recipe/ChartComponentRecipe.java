package com.otherworldinn.world.expedition.recipe;

import com.otherworldinn.item.ChartComponentItem;
import com.otherworldinn.world.expedition.ChartComponentType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import com.otherworldinn.world.expedition.ExpeditionNbtHelper;

public class ChartComponentRecipe extends CustomRecipe {

    public static RecipeSerializer<?> SERIALIZER;

    private static final Map<Item, String> ITEM_TO_COMPONENT = new HashMap<>();
    private static final Set<Item> RICH_MINES_ORES = new HashSet<>();

    static {
        ITEM_TO_COMPONENT.put(Items.PRISMARINE_SHARD, "vast_ocean");
        ITEM_TO_COMPONENT.put(Items.SNOWBALL, "frozen_waste");
        ITEM_TO_COMPONENT.put(Items.EMERALD, "thriving_realm");
        ITEM_TO_COMPONENT.put(Items.RED_MUSHROOM, "mushroom_haven");
        ITEM_TO_COMPONENT.put(Items.BROWN_MUSHROOM, "mushroom_haven");
        ITEM_TO_COMPONENT.put(Items.SCULK, "sculk_depths");
        ITEM_TO_COMPONENT.put(Items.MAGMA_CREAM, "nether_molten");
        ITEM_TO_COMPONENT.put(Items.CRIMSON_NYLIUM, "nether_crimson");
        ITEM_TO_COMPONENT.put(Items.WARPED_NYLIUM, "nether_warped");
        ITEM_TO_COMPONENT.put(Items.NETHER_BRICKS, "nether_fortress");
        ITEM_TO_COMPONENT.put(Items.SOUL_SAND, "nether_soul_abyss");
        ITEM_TO_COMPONENT.put(Items.GLOWSTONE_DUST, "nether_crystal");
        ITEM_TO_COMPONENT.put(Items.END_STONE, "end_floating");
        ITEM_TO_COMPONENT.put(Items.SHULKER_SHELL, "end_ancient");

        ITEM_TO_COMPONENT.put(Items.IRON_ORE, "rich_mines");
        ITEM_TO_COMPONENT.put(Items.COPPER_ORE, "rich_mines");
        ITEM_TO_COMPONENT.put(Items.GOLD_ORE, "rich_mines");
        ITEM_TO_COMPONENT.put(Items.COAL, "rich_mines");
        ITEM_TO_COMPONENT.put(Items.REDSTONE, "rich_mines");
        ITEM_TO_COMPONENT.put(Items.LAPIS_LAZULI, "rich_mines");
        ITEM_TO_COMPONENT.put(Items.DIAMOND, "rich_mines");
        // EMERALD already mapped to "thriving_realm" above, so it maps as rich_mines too:
        // Wait, EMERALD is above mapped to "thriving_realm". The design says emerald is part of
        // rich_mines. But emerald alone (x8) maps to "thriving_realm". We need a way to
        // distinguish 8 emeralds (thriving_realm) from 1 emerald + 7 other ores (rich_mines).
        // This is handled specially in matches().

        RICH_MINES_ORES.add(Items.IRON_ORE);
        RICH_MINES_ORES.add(Items.COPPER_ORE);
        RICH_MINES_ORES.add(Items.GOLD_ORE);
        RICH_MINES_ORES.add(Items.COAL);
        RICH_MINES_ORES.add(Items.REDSTONE);
        RICH_MINES_ORES.add(Items.LAPIS_LAZULI);
        RICH_MINES_ORES.add(Items.EMERALD);
        RICH_MINES_ORES.add(Items.DIAMOND);
    }

    public ChartComponentRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack blank = ItemStack.EMPTY;
        Set<Item> seenOres = new HashSet<>();
        boolean hasRichMinesMix = true;
        boolean allSameComponent = true;
        String targetComponent = null;
        int materialCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof ChartComponentItem
                    && "blank".equals(ChartComponentItem.getComponentType(stack))) {
                if (!blank.isEmpty()) return false;
                blank = stack;
            } else {
                String comp = resolveComponent(stack);
                if (comp == null) return false;

                if (targetComponent == null) {
                    targetComponent = comp;
                } else if (!targetComponent.equals(comp)) {
                    allSameComponent = false;
                    if (!RICH_MINES_ORES.contains(stack.getItem())) hasRichMinesMix = false;
                }

                if (RICH_MINES_ORES.contains(stack.getItem())) {
                    if (!seenOres.add(stack.getItem())) hasRichMinesMix = false;
                } else {
                    hasRichMinesMix = false;
                }

                materialCount++;
            }
        }

        if (blank.isEmpty() || materialCount != 8) return false;

        if ("rich_mines".equals(targetComponent)) {
            return hasRichMinesMix && seenOres.size() >= 8;
        }

        return allSameComponent;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack blank = ItemStack.EMPTY;
        Set<Item> seenOres = new HashSet<>();
        boolean allOres = true;
        String target = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ChartComponentItem) {
                if ("blank".equals(ChartComponentItem.getComponentType(stack))) blank = stack;
                continue;
            }
            if (RICH_MINES_ORES.contains(stack.getItem())) {
                seenOres.add(stack.getItem());
            } else {
                allOres = false;
            }
            if (target == null) {
                target = resolveComponent(stack);
            }
        }

        if (blank.isEmpty() || target == null) return ItemStack.EMPTY;

        if ("rich_mines".equals(target) && allOres && seenOres.size() >= 8) {
            target = "rich_mines";
        } else if ("rich_mines".equals(target) && seenOres.size() < 8) {
            return ItemStack.EMPTY;
        }

        ChartComponentType type = ChartComponentType.byId(target);
        if (type == null) return ItemStack.EMPTY;

        ItemStack result = new ItemStack(blank.getItem());
        CompoundTag tag = ExpeditionNbtHelper.readTag(result);
        tag.putString("component_type", type.id());
        ExpeditionNbtHelper.writeTag(result, tag);
        return result;
    }

    private String resolveComponent(ItemStack stack) {
        String comp = ITEM_TO_COMPONENT.get(stack.getItem());
        if (comp == null) {
            if (stack.is(net.minecraft.tags.ItemTags.LOGS)) comp = "dense_woods";
            else if (stack.is(net.minecraft.tags.ItemTags.FLOWERS)) comp = "blossom_valley";
            else if (stack.is(net.minecraft.tags.ItemTags.STONE_CRAFTING_MATERIALS)
                    && stack.getItem() != Items.COBBLESTONE) comp = "stone_spires";
            else if (isFertileSeed(stack.getItem())) comp = "fertile_fields";
            else if (stack.getItem() == Items.SAND || stack.getItem() == Items.RED_SAND)
                comp = "scorched_basin";
        }
        return comp;
    }

    private boolean isFertileSeed(Item item) {
        return item == Items.WHEAT_SEEDS || item == Items.CARROT || item == Items.POTATO
                || item == Items.BEETROOT_SEEDS || item == Items.PUMPKIN_SEEDS
                || item == Items.MELON_SEEDS;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 9;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}