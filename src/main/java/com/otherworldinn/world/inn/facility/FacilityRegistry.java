package com.otherworldinn.world.inn.facility;

import com.otherworldinn.OtherworldInn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FacilityRegistry {
    private static final Map<String, FacilityDefinition> FACILITIES = new LinkedHashMap<>();
    private static final List<ResourceLocation> FACILITY_TOOL_ITEM_IDS =
            List.of(
                    ResourceLocation.fromNamespaceAndPath(
                            OtherworldInn.MODID, "facility_upgrade_template"));

    static {
        registerDefaults();
    }

    private FacilityRegistry() {}

    public static FacilityDefinition registerFacility(
            String id,
            String enName,
            String zhName,
            int maxLevel,
            BlockPos centerPos,
            List<LevelUpgradeCost> levelUpgradeCosts) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Facility id cannot be blank.");
        }
        if (maxLevel < 1) {
            throw new IllegalArgumentException("Facility maxLevel must be >= 1.");
        }
        if (levelUpgradeCosts == null || levelUpgradeCosts.size() != maxLevel) {
            throw new IllegalArgumentException(
                    "levelUpgradeCosts size must be equal to maxLevel. expected="
                            + maxLevel
                            + ", actual="
                            + (levelUpgradeCosts == null ? 0 : levelUpgradeCosts.size()));
        }
        if (centerPos == null) {
            throw new IllegalArgumentException("Facility centerPos cannot be null.");
        }

        Map<Integer, FacilityLevelDefinition> levels = new LinkedHashMap<>();
        levels.put(
                0,
                new FacilityLevelDefinition(
                        0,
                        List.of(),
                        0,
                        buildStructureId(id, 0)));

        for (int level = 1; level <= maxLevel; level++) {
            LevelUpgradeCost cost = levelUpgradeCosts.get(level - 1);
            List<ItemStack> copiedItems = copyItemStacks(cost.requiredItems());
            levels.put(
                    level,
                    new FacilityLevelDefinition(
                            level,
                            copiedItems,
                            Math.max(0, cost.requiredCoins()),
                            buildStructureId(id, level)));
        }

        FacilityDefinition definition =
                new FacilityDefinition(
                        id,
                        enName == null || enName.isBlank() ? id : enName,
                        zhName == null || zhName.isBlank() ? enName : zhName,
                        maxLevel,
                        centerPos.immutable(),
                        "facility." + OtherworldInn.MODID + "." + id,
                        Map.copyOf(levels));
        FACILITIES.put(id, definition);
        return definition;
    }

    public static FacilityDefinition get(String id) {
        return FACILITIES.get(id);
    }

    public static Collection<FacilityDefinition> getAll() {
        return FACILITIES.values();
    }

    public static Optional<FacilityDefinition> findNearbyFacility(BlockPos pos, double radius) {
        if (pos == null || radius <= 0) {
            return Optional.empty();
        }
        double radiusSq = radius * radius;
        for (FacilityDefinition facility : FACILITIES.values()) {
            if (facility.centerPos().distSqr(pos) <= radiusSq) {
                return Optional.of(facility);
            }
        }
        return Optional.empty();
    }

    public static boolean isHoldingFacilityTool(Player player) {
        if (player == null) {
            return false;
        }
        return isToolStack(player.getItemInHand(InteractionHand.MAIN_HAND))
                || isToolStack(player.getItemInHand(InteractionHand.OFF_HAND));
    }

    public static boolean isToolStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (ResourceLocation itemId : FACILITY_TOOL_ITEM_IDS) {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item != null && item != net.minecraft.world.item.Items.AIR && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    public static ResourceLocation buildStructureId(String facilityId, int level) {
        return ResourceLocation.fromNamespaceAndPath(
                OtherworldInn.MODID, "facility/" + facilityId + "/level_" + level);
    }

    private static List<ItemStack> copyItemStacks(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return List.of();
        }
        List<ItemStack> copied = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                copied.add(stack.copy());
            }
        }
        return List.copyOf(copied);
    }

    private static void registerDefaults() {
        registerFacility(
                "boiler_room",
                "Boiler Room",
                "锅炉房",
                4,
                new BlockPos(12, 71, -8),
                List.of(
                        new LevelUpgradeCost(
                                List.of(
                                        new ItemStack(Items.COPPER_INGOT, 12),
                                        new ItemStack(Items.FURNACE, 2)),
                                400),
                        new LevelUpgradeCost(
                                List.of(
                                        new ItemStack(Items.IRON_INGOT, 20),
                                        new ItemStack(Items.BLAST_FURNACE, 1)),
                                900),
                        new LevelUpgradeCost(
                                List.of(
                                        new ItemStack(Items.REDSTONE, 24),
                                        new ItemStack(Items.CAULDRON, 2)),
                                1800),
                        new LevelUpgradeCost(
                                List.of(
                                        new ItemStack(Items.GOLD_INGOT, 16),
                                        new ItemStack(Items.BLAZE_ROD, 8)),
                                3200)));
    }

    public record FacilityDefinition(
            String id,
            String enName,
            String zhName,
            int maxLevel,
            BlockPos centerPos,
            String translationKey,
            Map<Integer, FacilityLevelDefinition> levels) {
        public FacilityLevelDefinition getLevel(int level) {
            int clamped = Math.max(0, Math.min(level, maxLevel));
            return levels.get(clamped);
        }
    }

    public record FacilityLevelDefinition(
            int level,
            List<ItemStack> requiredItems,
            int requiredCoins,
            ResourceLocation structureId) {}

    public record LevelUpgradeCost(List<ItemStack> requiredItems, int requiredCoins) {}
}
