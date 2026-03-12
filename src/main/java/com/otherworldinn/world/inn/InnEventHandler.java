package com.otherworldinn.world.inn;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.item.RoomKeyItem;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashSet;
import java.util.Optional;
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
     * 监听装备变更事件 (服务器端)
     * <p>
     * 当房间登记册进入副手时，播放翻页音效。
     * 这通常发生在玩家将物品从主手切换到副手时。
     * </p>
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            // 避免登录时触发
            if (player.tickCount < 10) return;

            // 仅关注副手变化
            if (event.getSlot() == EquipmentSlot.OFFHAND) {
                ItemStack to = event.getTo();
                ItemStack from = event.getFrom();
                
                // 检查是否切换到了房间登记册，且之前不是房间登记册
                if (to.is(ModItems.ROOM_REGISTER.get()) && !from.is(ModItems.ROOM_REGISTER.get())) {
                    // 使用 null 作为 player 参数，确保包括触发者在内的所有附近玩家都能听到声音
                    player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }
    }

    /**
     * 处理玩家左键点击方块事件 (服务器端)
     * <p>
     * 1. 地契：清除选定范围
     * 2. 房间登记册：删除房间（并阻止方块破坏）
     * </p>
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos relativePos = event.getPos().relative(event.getFace());

        // 1. 处理地契逻辑 (主手)
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHandItem.is(ModItems.LAND_DEED.get())) {
            CustomData customData = mainHandItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();

            if (tag.contains("Pos1")) {
                if (!level.isClientSide) {
                    tag.remove("Pos1");
                    tag.remove("Pos2");
                    mainHandItem.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    player.displayClientMessage(Component.translatable("message.otherworldinn.land_deed.selection_cleared")
                            .withStyle(style -> style.withColor(ModColors.INFO)), true);
                }
                event.setCanceled(true); // 取消方块破坏
                return;
            }
        }

        // 2. 处理房间钥匙逻辑 (主手)
        if (mainHandItem.is(ModItems.ROOM_KEY.get())) {
            Optional<Integer> roomId = RoomKeyItem.getBoundRoomId(mainHandItem);
            if (roomId.isPresent()) {
                if (!level.isClientSide) {
                    RoomKeyItem.unbindRoom(mainHandItem);
                    player.displayClientMessage(Component.translatable("message.otherworldinn.room_key.unbound")
                            .withStyle(style -> style.withColor(ModColors.INFO)), true);
                }
                event.setCanceled(true); // 取消方块破坏
                return;
            }
        }

        // 3. 处理房间登记册逻辑 (副手)
        ItemStack offhandItem = player.getItemInHand(InteractionHand.OFF_HAND);
        if (offhandItem.is(ModItems.ROOM_REGISTER.get())) {
            // 只要副手持有房间登记册，就取消方块破坏，尝试执行删除房间逻辑
            event.setCanceled(true);
            handleLeftClick(player, relativePos, level);
        }
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

        // 再次检查物品（虽然调用前已检查，但作为独立方法保留检查更安全）
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
