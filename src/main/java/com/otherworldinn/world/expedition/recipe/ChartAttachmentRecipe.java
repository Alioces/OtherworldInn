package com.otherworldinn.world.expedition.recipe;

import com.otherworldinn.item.ChartComponentItem;
import com.otherworldinn.item.ExpeditionChartItem;
import com.otherworldinn.world.expedition.ChartComponentType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import com.otherworldinn.world.expedition.ExpeditionNbtHelper;

public class ChartAttachmentRecipe extends CustomRecipe {

    public static RecipeSerializer<?> SERIALIZER;

    public ChartAttachmentRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack chartStack = ItemStack.EMPTY;
        List<String> newCompTypes = new ArrayList<>();

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ExpeditionChartItem) {
                if (!chartStack.isEmpty()) return false;
                chartStack = stack;
            } else if (stack.getItem() instanceof ChartComponentItem) {
                String compType = ChartComponentItem.getComponentType(stack);
                if ("blank".equals(compType)) return false;
                newCompTypes.add(compType);
            } else {
                return false;
            }
        }

        if (chartStack.isEmpty() || newCompTypes.isEmpty()) return false;

        int maxSlots = ExpeditionNbtHelper.readTag(chartStack).getInt("max_slots");
        if (maxSlots <= 0) maxSlots = 1;

        List<String> existing = ExpeditionChartItem.getComponentIds(chartStack);
        if (existing.size() + newCompTypes.size() > maxSlots) return false;

        Set<String> dupCheck = new HashSet<>(existing);
        List<String> combined = new ArrayList<>(existing);
        for (String compType : newCompTypes) {
            if (dupCheck.contains(compType)) return false;
            dupCheck.add(compType);

            ChartComponentType ct = ChartComponentType.byId(compType);
            if (ct == null) return false;

            if (ChartComponentType.conflictsWithExisting(combined, ct)) return false;
            combined.add(compType);

            ChartComponentType.DimensionCategory chartDim =
                    ExpeditionChartItem.getChartDimension(chartStack);
            if (!ChartComponentType.componentMatchesChartDimension(compType, chartDim))
                return false;
        }

        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack chartStack = ItemStack.EMPTY;
        List<String> newCompTypes = new ArrayList<>();

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ExpeditionChartItem) {
                chartStack = stack;
            } else if (stack.getItem() instanceof ChartComponentItem) {
                String compType = ChartComponentItem.getComponentType(stack);
                if (!"blank".equals(compType)) {
                    newCompTypes.add(compType);
                }
            }
        }

        if (chartStack.isEmpty() || newCompTypes.isEmpty()) return ItemStack.EMPTY;

        ItemStack result = chartStack.copy();
        CompoundTag chartTag = ExpeditionNbtHelper.readTag(result);
        ListTag compList = chartTag.getList("components", CompoundTag.TAG_COMPOUND);
        if (compList.isEmpty() && chartTag.contains("components")) {
            compList = chartTag.getList("components", CompoundTag.TAG_COMPOUND);
        }

        ListTag newList = new ListTag();
        for (int i = 0; i < compList.size(); i++) {
            newList.add(compList.getCompound(i).copy());
        }

        for (String compType : newCompTypes) {
            CompoundTag newComp = new CompoundTag();
            newComp.putString("type", compType);
            ChartComponentType ct = ChartComponentType.byId(compType);
            newComp.putString("rarity", ct != null ? ct.rarity().name().toLowerCase() : "common");
            newList.add(newComp);
        }

        chartTag.put("components", newList);
        ExpeditionNbtHelper.writeTag(result, chartTag);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
