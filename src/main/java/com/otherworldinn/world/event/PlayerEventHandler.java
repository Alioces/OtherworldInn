package com.otherworldinn.world.event;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.minecraft.world.item.ItemStack;
import com.otherworldinn.init.ModItems;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * 玩家事件处理器
 */
@EventBusSubscriber(modid = OtherworldInn.MODID)
public class PlayerEventHandler {

    /**
     * 处理玩家维度切换事件
     * <p>
     * 当玩家从城镇维度离开时，给予回程卷轴。
     */
    @SubscribeEvent
    public static void onDimensionChange(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 检查出发维度是否是城镇维度
            if (player.level().dimension() == TownDimensions.TOWN_LEVEL) {
                // 检查目标维度是否不是城镇维度
                if (event.getDimension() != TownDimensions.TOWN_LEVEL) {
                    // 给予回程卷轴
                    ItemStack scroll = new ItemStack(ModItems.RECALL_SCROLL.get());
                    // 检查背包是否已有
                    if (!player.getInventory().contains(scroll)) {
                        if (!player.getInventory().add(scroll)) {
                            player.drop(scroll, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理玩家登录事件
     * <p>
     * 玩家首次加入时，将其传送到旅社并设置重生点。
     * 同时也负责初始化玩家的队伍信息。
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.getServer();
            
            // 确保玩家加入队伍
            if (server != null) {
                TeamManager.getInstance().onPlayerJoin(player, server);
            }
            
            // 首次加入逻辑
            if (!player.getTags().contains("otherworldinn.joined")) {
                ServerLevel townLevel = player.getServer().getLevel(TownDimensions.TOWN_LEVEL);
                if (townLevel != null) {
                    BlockPos spawnPos = new BlockPos(10, 71, 0);
                    player.teleportTo(townLevel, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                    player.setRespawnPosition(TownDimensions.TOWN_LEVEL, spawnPos, 0, true, false);
                    player.addTag("otherworldinn.joined");
                }
            }
        }
    }

    /**
     * 处理玩家重生事件
     * <p>
     * 如果玩家没有重生点，则尝试将其传送到旅社。
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
             if (player.getRespawnPosition() == null) {
                 ServerLevel townLevel = player.getServer().getLevel(TownDimensions.TOWN_LEVEL);
                 if (townLevel != null) {
                     BlockPos spawnPos = new BlockPos(10, 71, 0);
                     player.teleportTo(townLevel, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                 }
             }
        }
    }
}
