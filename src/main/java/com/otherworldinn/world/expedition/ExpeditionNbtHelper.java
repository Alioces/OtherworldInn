package com.otherworldinn.world.expedition;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ExpeditionNbtHelper {

    private ExpeditionNbtHelper() {}

    public static CompoundTag readTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void writeTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void updateTag(ItemStack stack, Consumer<CompoundTag> updater) {
        CompoundTag tag = readTag(stack);
        updater.accept(tag);
        writeTag(stack, tag);
    }
}
