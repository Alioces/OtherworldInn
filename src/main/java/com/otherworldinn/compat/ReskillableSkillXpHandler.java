package com.otherworldinn.compat;

import com.otherworldinn.OtherworldInn;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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
    private static final double AGILITY_DISTANCE_PER_XP = 6.0D;
    private static final Map<UUID, Integer> LAST_SPRINT_STATS = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> SPRINT_DISTANCE_PROGRESS = new ConcurrentHashMap<>();

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

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!ReskillableCompat.isLoaded()) {
            return;
        }
        float amount = event.getNewDamage();
        if (amount <= 0.0F) {
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            int attackXp = Math.max(1, Mth.floor(amount));
            ReskillableCompat.addSkillExperience(attacker, "attack", attackXp);
        }
        if (event.getEntity() instanceof ServerPlayer defender) {
            int defenseXp = Math.max(1, Mth.floor(amount));
            ReskillableCompat.addSkillExperience(defender, "defense", defenseXp);
        }
    }

    @SubscribeEvent
    public static void onPlayerXpChange(PlayerXpEvent.XpChange event) {
        if (!ReskillableCompat.isLoaded()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int amount = event.getAmount();
        if (amount <= 0) {
            return;
        }
        ReskillableCompat.addSkillExperience(player, "gathering", amount);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!ReskillableCompat.isLoaded()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        UUID playerId = player.getUUID();
        int currentSprintCm = player.getStats().getValue(Stats.CUSTOM, Stats.SPRINT_ONE_CM);
        Integer lastSprintCm = LAST_SPRINT_STATS.put(playerId, currentSprintCm);
        if (lastSprintCm == null) {
            return;
        }
        int deltaCm = currentSprintCm - lastSprintCm;
        if (deltaCm <= 0) {
            return;
        }
        double moved = deltaCm / 100.0D;
        double progressed = SPRINT_DISTANCE_PROGRESS.getOrDefault(playerId, 0.0D) + moved;
        int gained = (int) (progressed / AGILITY_DISTANCE_PER_XP);
        double remain = progressed - gained * AGILITY_DISTANCE_PER_XP;
        if (remain <= 0.000001D) {
            SPRINT_DISTANCE_PROGRESS.remove(playerId);
        } else {
            SPRINT_DISTANCE_PROGRESS.put(playerId, remain);
        }
        if (gained > 0) {
            ReskillableCompat.addSkillExperience(player, "agility", gained);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUUID();
        LAST_SPRINT_STATS.remove(playerId);
        SPRINT_DISTANCE_PROGRESS.remove(playerId);
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
