package com.otherworldinn.world.event;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dimension.TownDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源主世界生物生成处理器
 * <p>
 * 负责在资源主世界中手动执行原版主世界的特殊生物生成机制，
 * 包括：巡逻队、幻翼、猫、僵尸围城、流浪商人。
 * </p>
 */
@EventBusSubscriber(modid = OtherworldInn.MODID)
public class ResourceOverworldSpawnerHandler {

    // 缓存每个 Level 对应的 Spawner 列表
    // 虽然通常只有一个 Resource Overworld 实例，但为了安全起见使用 Map
    private static final Map<ServerLevel, List<CustomSpawner>> SPAWNERS = new HashMap<>();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        
        // 仅处理资源主世界
        if (level.dimension() == TownDimensions.RESOURCE_OVERWORLD_LEVEL) {
            List<CustomSpawner> spawners = SPAWNERS.computeIfAbsent(level, l -> {
                List<CustomSpawner> list = new ArrayList<>();
                list.add(new PhantomSpawner());
                list.add(new PatrolSpawner());
                list.add(new CatSpawner());
                list.add(new VillageSiege());
                
                // WanderingTraderSpawner 需要 ServerLevelData
                // l.getLevelData() 返回 LevelData，需要强制转换为 ServerLevelData
                // 通常 ServerLevel 的 getLevelData() 返回的是 ServerLevelData 实例
                if (l.getLevelData() instanceof net.minecraft.world.level.storage.ServerLevelData serverLevelData) {
                    list.add(new WanderingTraderSpawner(serverLevelData));
                } else {
                    OtherworldInn.LOGGER.warn("Failed to add WanderingTraderSpawner: LevelData is not ServerLevelData");
                }
                
                OtherworldInn.LOGGER.info("Initialized custom spawners for Resource Overworld");
                return list;
            });

            // 手动 tick 这些 spawner
            boolean spawnEnemies = level.getLevelData().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING);
            boolean spawnFriendlies = level.getLevelData().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING); // 简化处理，通常这也受游戏规则控制

            for (CustomSpawner spawner : spawners) {
                spawner.tick(level, spawnEnemies, spawnFriendlies);
            }
        }
    }
}
