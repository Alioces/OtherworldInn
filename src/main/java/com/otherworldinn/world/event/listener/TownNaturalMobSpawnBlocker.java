package com.otherworldinn.world.event.listener;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dimension.TownDimensions;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/** 旅社维度自然生成生物拦截器。 */
@EventBusSubscriber(modid = OtherworldInn.MODID)
public class TownNaturalMobSpawnBlocker {
    private TownNaturalMobSpawnBlocker() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpawnPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getEntity().level().dimension() != TownDimensions.TOWN_LEVEL) {
            return;
        }
        if (!isNaturalSpawnType(event.getSpawnType())) {
            return;
        }
        event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity().level().dimension() != TownDimensions.TOWN_LEVEL) {
            return;
        }
        if (!isNaturalSpawnType(event.getSpawnType())) {
            return;
        }
        event.setSpawnCancelled(true);
    }

    private static boolean isNaturalSpawnType(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL
                || spawnType == MobSpawnType.CHUNK_GENERATION
                || spawnType == MobSpawnType.STRUCTURE
                || spawnType == MobSpawnType.PATROL;
    }
}
