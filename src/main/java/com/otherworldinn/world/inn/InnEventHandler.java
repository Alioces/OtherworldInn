package com.otherworldinn.world.inn;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.otherworldinn.init.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.List;
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
                
                // 更新房间属性 (家具统计)
                team.getInnData().updateAllRoomsStats(level);
                // 同步数据给客户端
                teamManager.syncTeam(team, level.getServer());
            }
        }
    }

    /**
     * 处理玩家左键点击方块事件 (服务器端)
     * <p>
     * 用于“房间登记册”在副手手持时的房间删除功能。
     * </p>
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        handleLeftClick(event.getEntity(), event.getPos(), event.getLevel());
    }

    /**
     * 处理左键点击方块的公共逻辑
     * <p>
     * 检查玩家副手是否持有房间登记册，且处于编辑模式下。
     * 如果条件满足，则删除点击位置所在的房间。
     * </p>
     *
     * @param player 玩家实体
     * @param pos    点击的方块坐标
     * @param level  世界实例
     */
    private static void handleLeftClick(Player player, BlockPos pos, Level level) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack offhandItem = player.getItemInHand(InteractionHand.OFF_HAND);
        if (!offhandItem.is(ModItems.ROOM_REGISTER.get())) {
            return;
        }

        // 检查是否在城镇维度
        if (level.dimension() != TownDimensions.TOWN_LEVEL) return;

        TeamData team = TeamManager.getInstance().getPlayerTeam(serverPlayer);
        if (team != null && team.getInnData().isEditMode()) {
            InnData innData = team.getInnData();

            RoomData room = innData.getRoomAt(pos);
            
            if (room != null) {
                // 执行删除
                innData.removeRoom(room.getId(), level, team, Component.translatable("message.otherworldinn.room_register.manual_removal"));
                
                // 同步数据给客户端
                TeamManager.getInstance().syncTeam(team, serverPlayer.getServer());
            }
        }
    }
}
