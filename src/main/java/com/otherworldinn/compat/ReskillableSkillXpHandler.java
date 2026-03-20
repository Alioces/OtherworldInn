package com.otherworldinn.compat;

import com.otherworldinn.OtherworldInn;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ReskillableSkillXpHandler {
    private static final int BUILDING_XP_NUMERATOR = 1;
    private static final int BUILDING_XP_DENOMINATOR = 50;
    private static final int MINING_XP_NUMERATOR = 1;
    private static final int MINING_XP_DENOMINATOR = 50;
    private static final int FARMING_XP_PER_CROP_ACTION = 2;
    private static final int FISHING_XP_PER_CATCH = 3;
    private static final double FISHING_TRIPLE_DROP_MAX_CHANCE = 0.5D;
    private static final double FISHING_DOUBLE_DROP_MAX_CHANCE = 0.9D;

    private ReskillableSkillXpHandler() {}

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!ReskillableCompat.isLoaded()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockState placed = event.getState();
        if (isCrop(placed)) {
            ReskillableCompat.addSkillExperience(player, "farming", FARMING_XP_PER_CROP_ACTION);
            return;
        }
        awardScaledExperience(
                player, "building", BUILDING_XP_NUMERATOR, BUILDING_XP_DENOMINATOR);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!ReskillableCompat.isLoaded() || event.isCanceled()) {
            return;
        }
        Player rawPlayer = event.getPlayer();
        if (!(rawPlayer instanceof ServerPlayer player)) {
            return;
        }
        BlockState broken = event.getState();
        if (isCrop(broken)) {
            ReskillableCompat.addSkillExperience(player, "farming", FARMING_XP_PER_CROP_ACTION);
            return;
        }
        awardScaledExperience(player, "mining", MINING_XP_NUMERATOR, MINING_XP_DENOMINATOR);
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (!ReskillableCompat.isLoaded() || event.isCanceled() || event.getDrops().isEmpty()) {
            return;
        }
        Player rawPlayer = event.getEntity();
        if (!(rawPlayer instanceof ServerPlayer player)) {
            return;
        }
        tryApplyFishingDropMultiplier(player, event);
        ReskillableCompat.addSkillExperience(player, "magic", FISHING_XP_PER_CATCH);
    }

    private static boolean isCrop(BlockState state) {
        return state.is(BlockTags.CROPS) || state.getBlock() instanceof CropBlock;
    }

    private static void awardScaledExperience(
            ServerPlayer player, String skillId, int numerator, int denominator) {
        if (numerator <= 0 || denominator <= 0) {
            return;
        }
        int guaranteed = numerator / denominator;
        int remainder = numerator % denominator;
        int total = guaranteed;
        if (remainder > 0 && player.getRandom().nextInt(denominator) < remainder) {
            total += 1;
        }
        if (total > 0) {
            ReskillableCompat.addSkillExperience(player, skillId, total);
        }
    }

    private static void tryApplyFishingDropMultiplier(ServerPlayer player, ItemFishedEvent event) {
        int maxLevel = Math.max(1, ReskillableCompat.getMaxLevel());
        int skillLevel = Math.max(0, ReskillableCompat.getSkillLevel(player, "magic"));
        double tripleChance =
                Math.min(
                        FISHING_TRIPLE_DROP_MAX_CHANCE,
                        (skillLevel / (double) maxLevel) * FISHING_TRIPLE_DROP_MAX_CHANCE);
        if (tripleChance > 0.0D && player.getRandom().nextDouble() < tripleChance) {
            event.getDrops().forEach(drop -> drop.setCount(drop.getCount() * 3));
            return;
        }
        double doubleChance =
                Math.min(
                        FISHING_DOUBLE_DROP_MAX_CHANCE,
                        (skillLevel / (double) maxLevel) * FISHING_DOUBLE_DROP_MAX_CHANCE);
        if (doubleChance > 0.0D && player.getRandom().nextDouble() < doubleChance) {
            event.getDrops().forEach(drop -> drop.setCount(drop.getCount() * 2));
        }
    }
}
