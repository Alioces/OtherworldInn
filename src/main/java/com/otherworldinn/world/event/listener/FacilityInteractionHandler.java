package com.otherworldinn.world.event.listener;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.inn.facility.FacilityRegistry;
import com.otherworldinn.world.inn.facility.FacilityUpgradeService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID)
public class FacilityInteractionHandler {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().dimension() != TownDimensions.TOWN_LEVEL) {
            return;
        }
        if (!FacilityRegistry.isHoldingFacilityTool(player)) {
            return;
        }
        if (FacilityUpgradeService.findContext(player) == null) {
            return;
        }
        if (FacilityUpgradeService.tryUpgrade(player, event.getHand())) {
            event.setCanceled(true);
        }
    }
}
