package com.otherworldinn.world.inn.facility;

import com.otherworldinn.world.event.TownStructurePlacer;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class FacilityUpgradeService {
    public static final double INTERACTION_RADIUS = 7.0D;

    private FacilityUpgradeService() {}

    public static FacilityContext findContext(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        TeamData team = TeamManager.getInstance().getPlayerTeam(player);
        if (team == null) {
            return null;
        }
        FacilityRegistry.FacilityDefinition facility =
                FacilityRegistry.findNearbyFacility(player.blockPosition(), INTERACTION_RADIUS).orElse(null);
        if (facility == null) {
            return null;
        }
        int currentLevel = team.getInnData().getFacilityLevel(facility.id());
        int nextLevel = currentLevel + 1;
        FacilityRegistry.FacilityLevelDefinition nextLevelDefinition =
                nextLevel <= facility.maxLevel() ? facility.getLevel(nextLevel) : null;
        return new FacilityContext(team, facility, currentLevel, nextLevelDefinition);
    }

    public static boolean tryUpgrade(ServerPlayer player, InteractionHand hand) {
        if (player == null) {
            return false;
        }
        if (!FacilityRegistry.isToolStack(player.getItemInHand(hand))) {
            return false;
        }
        FacilityContext context = findContext(player);
        if (context == null) {
            return false;
        }
        if (context.nextLevelDefinition() == null) {
            player.displayClientMessage(
                    Component.translatable("facility.otherworldinn.upgrade_max_level"),
                    true);
            return true;
        }

        TeamData team = context.team();
        FacilityRegistry.FacilityLevelDefinition next = context.nextLevelDefinition();

        if (!player.getAbilities().instabuild) {
            if (team.getCoins() < next.requiredCoins()) {
                player.displayClientMessage(
                        Component.translatable(
                                "facility.otherworldinn.upgrade_fail_coins",
                                next.requiredCoins(),
                                team.getCoins()),
                        true);
                return true;
            }
            if (!hasRequiredItems(player.getInventory(), next.requiredItems())) {
                player.displayClientMessage(
                        Component.translatable("facility.otherworldinn.upgrade_fail_items"),
                        true);
                return true;
            }
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return true;
        }
        boolean placed =
                TownStructurePlacer.placeStructureTemplate(
                        serverLevel, next.structureId(), context.facility().centerPos());
        if (!placed) {
            player.displayClientMessage(
                    Component.translatable("facility.otherworldinn.upgrade_fail_structure"),
                    true);
            return true;
        }

        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
            removeRequiredItems(player.getInventory(), next.requiredItems());
            team.removeCoins(next.requiredCoins(), player.getServer());
        }

        team.getInnData().setFacilityLevel(context.facility().id(), next.level());
        TeamManager.getInstance().syncTeam(team, player.getServer());
        String resultKey =
                context.currentLevel() == 0
                        ? "facility.otherworldinn.repair_success"
                        : "facility.otherworldinn.upgrade_success";
        player.displayClientMessage(
                Component.translatable(
                        resultKey,
                        Component.translatable(context.facility().translationKey()),
                        next.level()),
                true);
        return true;
    }

    private static boolean hasRequiredItems(Inventory inventory, List<ItemStack> requiredItems) {
        for (ItemStack required : requiredItems) {
            if (required.isEmpty()) {
                continue;
            }
            if (countMatchingItems(inventory, required) < required.getCount()) {
                return false;
            }
        }
        return true;
    }

    private static int countMatchingItems(Inventory inventory, ItemStack required) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (ItemStack.isSameItemSameComponents(slot, required)) {
                total += slot.getCount();
            }
        }
        return total;
    }

    private static void removeRequiredItems(Inventory inventory, List<ItemStack> requiredItems) {
        for (ItemStack required : requiredItems) {
            int remaining = required.getCount();
            if (remaining <= 0) {
                continue;
            }
            for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
                ItemStack slot = inventory.getItem(i);
                if (!ItemStack.isSameItemSameComponents(slot, required)) {
                    continue;
                }
                int removed = Math.min(remaining, slot.getCount());
                slot.shrink(removed);
                remaining -= removed;
                if (slot.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        inventory.setChanged();
    }

    public record FacilityContext(
            TeamData team,
            FacilityRegistry.FacilityDefinition facility,
            int currentLevel,
            FacilityRegistry.FacilityLevelDefinition nextLevelDefinition) {}
}
