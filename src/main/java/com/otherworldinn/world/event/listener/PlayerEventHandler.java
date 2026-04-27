package com.otherworldinn.world.event.listener;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.team.service.TeamManager;
import com.otherworldinn.world.teleport.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** 玩家事件处理器 */
@EventBusSubscriber(modid = OtherworldInn.MODID)
public class PlayerEventHandler {
    private static final double TOWN_BOUNDARY_CENTER_X = -19.0D;
    private static final double TOWN_BOUNDARY_CENTER_Z = 0.0D;
    private static final double TOWN_BOUNDARY_WARNING_DISTANCE = 150.0D;
    private static final double TOWN_BOUNDARY_WARNING_DISTANCE_SQR =
            TOWN_BOUNDARY_WARNING_DISTANCE * TOWN_BOUNDARY_WARNING_DISTANCE;
    private static final double TOWN_BOUNDARY_TELEPORT_DISTANCE = 170.0D;
    private static final double TOWN_BOUNDARY_TELEPORT_DISTANCE_SQR =
            TOWN_BOUNDARY_TELEPORT_DISTANCE * TOWN_BOUNDARY_TELEPORT_DISTANCE;
    private static final double TOWN_RELOCATE_X = 7.0D;
    private static final double TOWN_RELOCATE_Y = 71.0D;
    private static final double TOWN_RELOCATE_Z = 0.0D;
    private static final String TOWN_BOUNDARY_WARNING_KEY =
            "message.otherworldinn.town.boundary_warning";
    private static final Component TOWN_BOUNDARY_WARNING_TEXT =
            Component.translatable(TOWN_BOUNDARY_WARNING_KEY).withStyle(ChatFormatting.RED);


    /**
     * 处理玩家维度切换事件
     *
     * <p>当玩家从城镇维度离开时，给予回程卷轴。
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
     *
     * <p>玩家首次加入时，将其传送到旅社并设置重生点。 同时也负责初始化玩家的队伍信息。
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
                    player.teleportTo(
                            townLevel,
                            spawnPos.getX() + 0.5,
                            spawnPos.getY() + 1,
                            spawnPos.getZ() + 0.5,
                            player.getYRot(),
                            player.getXRot());
                    player.setRespawnPosition(TownDimensions.TOWN_LEVEL, spawnPos, 0, true, false);
                    player.addTag("otherworldinn.joined");
                }
            }
        }
    }

    /**
     * 处理玩家重生事件
     *
     * <p>如果玩家没有重生点，则尝试将其传送到旅社。 如果重生点是在原版主世界（已被隐藏），则重定向到资源主世界。
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {

            // 检查重生维度是否是原版主世界
            if (player.getRespawnDimension() == Level.OVERWORLD) {
                // 如果玩家没有设置具体的重生点（即使用的是世界出生点），或者强制重定向
                // 注意：如果玩家在原版主世界睡过觉（这在正常游玩中不应该发生，因为进不去），
                // 这里也会被重定向。
                TeleportUtils.teleportToOverworldSpawn(player);
            }

            if (player.getRespawnPosition() == null) {
                ServerLevel townLevel = player.getServer().getLevel(TownDimensions.TOWN_LEVEL);
                if (townLevel != null) {
                    BlockPos spawnPos = new BlockPos(10, 71, 0);
                    player.teleportTo(
                            townLevel,
                            spawnPos.getX() + 0.5,
                            spawnPos.getY() + 1,
                            spawnPos.getZ() + 0.5,
                            player.getYRot(),
                            player.getXRot());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        if (player.level().dimension() != TownDimensions.TOWN_LEVEL) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        double dx = player.getX() - TOWN_BOUNDARY_CENTER_X;
        double dz = player.getZ() - TOWN_BOUNDARY_CENTER_Z;
        double distanceSqr = dx * dx + dz * dz;
        if (distanceSqr <= TOWN_BOUNDARY_WARNING_DISTANCE_SQR) {
            return;
        }

        player.displayClientMessage(TOWN_BOUNDARY_WARNING_TEXT, true);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1, false, true));
        if (distanceSqr <= TOWN_BOUNDARY_TELEPORT_DISTANCE_SQR) {
            return;
        }

        player.teleportTo(
                player.serverLevel(),
                TOWN_RELOCATE_X,
                TOWN_RELOCATE_Y,
                TOWN_RELOCATE_Z,
                player.getYRot(),
                player.getXRot());
        player.displayClientMessage(TOWN_BOUNDARY_WARNING_TEXT, true);
    }
}
