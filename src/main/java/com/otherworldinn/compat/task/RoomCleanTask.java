package com.otherworldinn.compat.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidArriveAtBlockTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidMoveToPredicateBlockTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.mojang.datafixers.util.Pair;
import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.ModBlockProperties;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.item.BedSheetItem;
import com.otherworldinn.item.MessyBedSheetItem;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class RoomCleanTask implements IMaidTask{
        /**
     * 唯一标识符，用于区分不同的任务
     * <p>
     * Unique identifier for distinguishing different tasks
     */
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "clean_room");

    /**
     * 任务图标
     */
    private static final ItemStack ICON = new ItemStack(InitItems.BROOM.get());

    /**
     * 获取任务的 ID
     * <p>
     * Get the unique identifier of the task
     */
    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    /**
     * 获取任务的图标
     * <p>
     * Get the icon of the task
     */
    @Override
    public ItemStack getIcon() {
        return ICON;
    }

    /**
     * 获取女仆在该任务时的音效，可以为 null
     * <p>
     * Get the sound when the maid in this task, can be null
     */
    @Override
    @Nullable
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return null;
    }

    /**
     * 创建女仆 AI，这一块通过 Minecraft 原版的 BehaviorControl 来实现
     */
    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        return List.of(
                Pair.of(
                        5,
                        new MaidMoveToPredicateBlockTask(
                                0.8f,
                                IMaidTask.VERTICAL_SEARCH_RANGE,
                                RoomCleanTask::shouldCleanBeds,
                                RoomCleanTask::isMessyBed)),
                Pair.of(6, new MaidArriveAtBlockTask(2.2, RoomCleanTask::cleanMessyBedAt)),
                Pair.of(
                        7,
                        new MaidMoveToPredicateBlockTask(
                                0.8f,
                                IMaidTask.VERTICAL_SEARCH_RANGE,
                                RoomCleanTask::shouldWashDirtySheets,
                                RoomCleanTask::isWashTarget)),
                Pair.of(8, new MaidArriveAtBlockTask(2.2, RoomCleanTask::washMessySheetAt)));
    }

    private static boolean hasCleanSheet(EntityMaid maid) {
        return findFirstSlot(maid.getMaidInv(), ModItems.BED_SHEET.get()) >= 0;
    }

    private static boolean hasDirtySheet(EntityMaid maid) {
        return findFirstSlot(maid.getMaidInv(), ModItems.MESSY_BED_SHEET.get()) >= 0;
    }

    private static boolean shouldCleanBeds(EntityMaid maid) {
        return hasCleanSheet(maid) && !shouldWashDirtySheets(maid);
    }

    private static boolean shouldWashDirtySheets(EntityMaid maid) {
        int dirtyCount = countItem(maid.getMaidInv(), ModItems.MESSY_BED_SHEET.get());
        if (dirtyCount <= 0) {
            return false;
        }
        int cleanCount = countItem(maid.getMaidInv(), ModItems.BED_SHEET.get());
        int totalCount = cleanCount + dirtyCount;
        return dirtyCount * 3 > totalCount;
    }

    private static boolean isMessyBed(EntityMaid maid, BlockPos pos) {
        BlockState state = maid.level().getBlockState(pos);
        return state.getBlock() instanceof BedBlock
                && state.hasProperty(ModBlockProperties.MESSY)
                && state.getValue(ModBlockProperties.MESSY);
    }

    private static boolean isWashTarget(EntityMaid maid, BlockPos pos) {
        return MessyBedSheetItem.isWashTarget(maid.level().getBlockState(pos));
    }

    private static void cleanMessyBedAt(EntityMaid maid, BlockPos pos) {
        if (!(maid.level() instanceof ServerLevel level)) {
            return;
        }
        int cleanSlot = findFirstSlot(maid.getMaidInv(), ModItems.BED_SHEET.get());
        if (cleanSlot < 0) {
            clearTarget(maid);
            return;
        }
        ItemStack cleanSheet = maid.getMaidInv().getStackInSlot(cleanSlot);
        if (!BedSheetItem.cleanMessyBed(level, pos)) {
            clearTarget(maid);
            return;
        }
        ItemStack dirtySheet = BedSheetItem.createDirtySheet(cleanSheet, 1);
        consumeOne(maid.getMaidInv(), cleanSlot);
        insertOrDrop(maid, dirtySheet);
        maid.swing(InteractionHand.MAIN_HAND, true);
        clearTarget(maid);
    }

    private static void washMessySheetAt(EntityMaid maid, BlockPos pos) {
        if (!(maid.level() instanceof ServerLevel level)) {
            return;
        }
        BlockState targetState = level.getBlockState(pos);
        if (!MessyBedSheetItem.isWashTarget(targetState)) {
            clearTarget(maid);
            return;
        }
        int dirtySlot = findFirstSlot(maid.getMaidInv(), ModItems.MESSY_BED_SHEET.get());
        if (dirtySlot < 0) {
            clearTarget(maid);
            return;
        }
        ItemStack dirtySheet = maid.getMaidInv().getStackInSlot(dirtySlot);
        ItemStack cleanSheet = MessyBedSheetItem.createCleanSheet(dirtySheet, false);
        consumeOne(maid.getMaidInv(), dirtySlot);
        insertOrDrop(maid, cleanSheet);
        MessyBedSheetItem.consumeCauldronWaterIfNeeded(level, pos);
        maid.swing(InteractionHand.MAIN_HAND, true);
        clearTarget(maid);
    }

    private static void clearTarget(EntityMaid maid) {
        maid.getBrain().eraseMemory(InitEntities.TARGET_POS.get());
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    private static int findFirstSlot(ItemStackHandler inv, net.minecraft.world.item.Item item) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(item)) {
                return i;
            }
        }
        return -1;
    }

    private static int countItem(ItemStackHandler inv, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void consumeOne(ItemStackHandler inv, int slot) {
        ItemStack stack = inv.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return;
        }
        if (stack.getCount() <= 1) {
            inv.setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            ItemStack copy = stack.copy();
            copy.shrink(1);
            inv.setStackInSlot(slot, copy);
        }
    }

    private static void insertOrDrop(EntityMaid maid, ItemStack stack) {
        ItemStack remain = stack.copy();
        ItemStackHandler inv = maid.getMaidInv();
        for (int i = 0; i < inv.getSlots() && !remain.isEmpty(); i++) {
            remain = inv.insertItem(i, remain, false);
        }
        if (!remain.isEmpty()) {
            maid.spawnAtLocation(remain);
        }
    }
}
