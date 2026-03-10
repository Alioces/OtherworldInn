package com.otherworldinn.world.inn;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 旅社事件处理器
 * <p>
 * 处理与旅社运营相关的事件，例如方块更新触发的房间检查。
 * </p>
 */
@EventBusSubscriber(modid = OtherworldInn.MODID)
public class InnEventHandler {

    // 记录本 tick 需要检查的队伍 ID
    private static final Set<UUID> pendingChecks = new HashSet<>();

    /**
     * 监听方块更新事件 (NeighborNotifyEvent)
     * <p>
     * 当方块发生更新（放置、破坏、状态改变）时触发。
     * 如果更新发生在开启了编辑模式的旅社区域内，则标记该队伍在 tick 结束时进行房间检查。
     * </p>
     */
    @SubscribeEvent
    public static void onBlockUpdate(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != TownDimensions.TOWN_LEVEL) return;

        BlockPos pos = event.getPos();
        TeamData team = TeamManager.getInstance().getTeamAt(pos, level.getServer());

        if (team != null && team.getInnData().isEditMode()) {
            synchronized (pendingChecks) {
                pendingChecks.add(team.getTeamId());
            }
        }
    }
    
    /**
     * 监听方块放置事件
     * <p>
     * NeighborNotifyEvent 可能不覆盖所有情况（如直接放置），补充监听 PlaceEvent。
     * </p>
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != TownDimensions.TOWN_LEVEL) return;

        BlockPos pos = event.getPos();
        TeamData team = TeamManager.getInstance().getTeamAt(pos, level.getServer());

        if (team != null && team.getInnData().isEditMode()) {
            synchronized (pendingChecks) {
                pendingChecks.add(team.getTeamId());
            }
        }
    }
    
    /**
     * 监听方块破坏事件
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != TownDimensions.TOWN_LEVEL) return;

        BlockPos pos = event.getPos();
        TeamData team = TeamManager.getInstance().getTeamAt(pos, level.getServer());

        if (team != null && team.getInnData().isEditMode()) {
            synchronized (pendingChecks) {
                pendingChecks.add(team.getTeamId());
            }
        }
    }

    /**
     * 在 Level Tick 结束时处理待定检查
     * <p>
     * 确保每个 tick 每个队伍最多只检查一次房间合法性，避免性能浪费。
     * </p>
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != TownDimensions.TOWN_LEVEL) return;

        Set<UUID> teamsToCheck;
        synchronized (pendingChecks) {
            if (pendingChecks.isEmpty()) return;
            teamsToCheck = new HashSet<>(pendingChecks);
            pendingChecks.clear();
        }

        TeamManager teamManager = TeamManager.getInstance();
        for (UUID teamId : teamsToCheck) {
            TeamData team = teamManager.getTeam(teamId, level.getServer());
            if (team != null) {
                // 执行房间合法性检查
                team.getInnData().checkAllRoomsValidity(level, team);
            }
        }
    }
}
