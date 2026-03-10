package com.otherworldinn.world.event;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import java.util.List;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.Direction;

/**
 * 城镇保护处理器
 * <p>
 * 在城镇维度中，默认禁止破坏和放置方块。
 * 只有在玩家所属队伍的“旅社”范围内才允许建筑。
 * 同时禁止使用特定的物品。
 */
@EventBusSubscriber(modid = OtherworldInn.MODID)
public class TownProtectionHandler {

    /**
     * 检查是否可以在指定位置建筑（针对玩家）
     */
    private static boolean canBuild(Player player, BlockPos pos, Level level) {
        // 仅在城镇维度生效
        if (level.dimension() != TownDimensions.TOWN_LEVEL) {
            return true;
        }

        // 创造模式豁免
        if (player.isCreative()) {
            return true;
        }

        TeamData team = TeamManager.getInstance().getPlayerTeam(player);
        if (team == null) {
            // 没有队伍，禁止一切建筑
            return false;
        }

        // 必须在旅社区域内，并且开启了编辑模式
        return isInsideInnZone(team, pos) && team.getInnData().isEditMode();
    }
    
    /**
     * 客户端事件处理器
     * 专门用于在客户端预测阶段就拦截交互
     */
    @EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
    public static class ClientHandler {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            if (event.getLevel().dimension() == TownDimensions.TOWN_LEVEL) {
                ItemStack stack = event.getItemStack();
                if (stack.is(OtherworldInn.BANNED_IN_TOWN)) {
                    event.setCanceled(true);
                    event.setUseItem(TriState.FALSE);
                    event.setUseBlock(TriState.FALSE);
                    event.setCancellationResult(InteractionResult.FAIL);
                }
            }
        }
        
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            if (event.getLevel().dimension() == TownDimensions.TOWN_LEVEL) {
                ItemStack stack = event.getItemStack();
                if (stack.is(OtherworldInn.BANNED_IN_TOWN)) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.FAIL);
                }
            }
        }
    }

    /**
     * 检查坐标是否在队伍的旅社区域内
     */
    private static boolean isInsideInnZone(TeamData team, BlockPos pos) {
        BlockPos center = team.getInnZoneCenter();
        int radius = team.getInnZoneRadius();

        // 仅检查水平坐标 (X, Z)，忽略 Y 轴高度限制
        // x: [center.x - radius, center.x + radius]
        // z: [center.z - radius, center.z + radius]
        
        return Math.abs(pos.getX() - center.getX()) <= radius &&
               Math.abs(pos.getZ() - center.getZ()) <= radius;
    }

    private static void sendDenyMessage(Player player) {
        // 使用 Status Bar
        player.displayClientMessage(Component.translatable("message.otherworldinn.protection.deny")
                .withStyle(style -> style.withColor(0xFF6A6A)), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = (Level) event.getLevel();
        
        // 如果是真实玩家
        if (player instanceof ServerPlayer && !(player instanceof FakePlayer)) {
            if (!canBuild(player, event.getPos(), level)) {
                event.setCanceled(true);
                sendDenyMessage(player);
            }
        } 
        // 如果是非玩家实体或 FakePlayer (自动化设备)
        else if (level instanceof ServerLevel serverLevel) {
             if (serverLevel.dimension() == TownDimensions.TOWN_LEVEL) {
                 TeamData team = TeamManager.getInstance().getTeamAt(event.getPos(), serverLevel.getServer());
                 // 只有在开启了编辑模式的旅社区域内才允许破坏
                 if (team == null || !team.getInnData().isEditMode()) {
                     event.setCanceled(true);
                 }
             }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            // 如果是真实玩家
            if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
                if (!canBuild(player, event.getPos(), (Level) event.getLevel())) {
                    event.setCanceled(true);
                    sendDenyMessage(player);
                    serverPlayer.inventoryMenu.sendAllDataToRemote();
                    return;
                }
            } 
            // 如果是 FakePlayer
            else if (event.getEntity() instanceof FakePlayer) {
                Level level = event.getEntity().level();
                if (level instanceof ServerLevel serverLevel && level.dimension() == TownDimensions.TOWN_LEVEL) {
                    TeamData team = TeamManager.getInstance().getTeamAt(event.getPos(), serverLevel.getServer());
                    // 只有在开启了编辑模式的旅社区域内才允许放置
                    if (team == null || !team.getInnData().isEditMode()) {
                        event.setCanceled(true);
                    }
                }
            }
        } else if (event.getEntity() != null) {
            // 如果是非玩家实体 (例如末影人)
            Level level = event.getEntity().level();
            if (level instanceof ServerLevel serverLevel && level.dimension() == TownDimensions.TOWN_LEVEL) {
                TeamData team = TeamManager.getInstance().getTeamAt(event.getPos(), serverLevel.getServer());
                // 只有在开启了编辑模式的旅社区域内才允许放置
                if (team == null || !team.getInnData().isEditMode()) {
                    event.setCanceled(true);
                }
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().dimension() == TownDimensions.TOWN_LEVEL) {
            ItemStack stack = event.getItemStack();
            if (stack.is(OtherworldInn.BANNED_IN_TOWN)) {
                event.setCanceled(true);
                if (event.getEntity() instanceof ServerPlayer player) {
                    player.displayClientMessage(Component.translatable("message.otherworldinn.protection.banned_item"), true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.dimension() == TownDimensions.TOWN_LEVEL) {
            Player player = event.getEntity();
            BlockPos pos = event.getPos();
            
            // 1. 检查禁用物品
            ItemStack stack = event.getItemStack();
            if (stack.is(OtherworldInn.BANNED_IN_TOWN)) {
                event.setCanceled(true);
                event.setUseItem(TriState.FALSE);
                event.setUseBlock(TriState.FALSE);
                event.setCancellationResult(InteractionResult.FAIL);
                
                if (player instanceof ServerPlayer serverPlayer) {
                    player.displayClientMessage(Component.translatable("message.otherworldinn.protection.banned_item"), true);
                    serverPlayer.inventoryMenu.sendAllDataToRemote();
                }
                return;
            }

            // 2. 检查建筑权限 (针对放置方块的行为)
            // 只有当玩家在生存/冒险模式下，且手持方块物品时才需要提前拦截
            if (!player.isCreative() && !stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                // 计算拟放置位置
                BlockPos placePos = pos.relative(event.getFace());
                
                // 检查是否允许在该位置建筑
                if (!canBuild(player, placePos, level)) {
                    event.setCanceled(true);
                    event.setUseItem(TriState.FALSE);
                    event.setUseBlock(TriState.FALSE);
                    event.setCancellationResult(InteractionResult.FAIL);
                    
                    if (player instanceof ServerPlayer serverPlayer) {
                        sendDenyMessage(player);
                        // 关键：同步背包数据，防止客户端显示物品被消耗
                        serverPlayer.inventoryMenu.sendAllDataToRemote();
                    }
                }
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.dimension() == TownDimensions.TOWN_LEVEL) {
            // 清除受影响的方块列表，防止破坏地形
            event.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().dimension() == TownDimensions.TOWN_LEVEL) {
            Entity entity = event.getEntity();
            
            // 检查实体是否在禁用列表中 (通过 ResourceLocation 检查)
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            
            // 针对 Super Glue 的特殊检查
            /*
            if (entityId.toString().equals("create:super_glue")) {
                event.setCanceled(true);
                return;
            }
             */

        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPistonPre(PistonEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == TownDimensions.TOWN_LEVEL) {
            BlockPos pos = event.getPos();
            
            // 始终手动创建并解析结构，以确保获取最准确的受影响方块列表
            PistonStructureResolver resolver = new PistonStructureResolver(level, pos, event.getDirection(), event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND);
            if (!resolver.resolve()) {
                return; // 推不动，无需干预
            }

            // 收集所有需要检查的位置
            java.util.Set<BlockPos> pointsToCheck = new java.util.HashSet<>();
            
            // 1. 活塞本身的位置
            pointsToCheck.add(pos);
            
            // 2. 活塞臂伸出的位置 (如果是伸出)
            if (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND) {
                pointsToCheck.add(pos.relative(event.getDirection()));
            }
            
            // 3. 将被破坏的方块
            pointsToCheck.addAll(resolver.getToDestroy());
            
            // 4. 将被移动的方块及其目标位置
            Direction moveDir = (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND) ? event.getDirection() : event.getDirection().getOpposite();
            for (BlockPos p : resolver.getToPush()) {
                pointsToCheck.add(p); // 源位置
                pointsToCheck.add(p.relative(moveDir)); // 目标位置
            }
            
            // 验证一致性：所有点必须属于同一个队伍（或者都不属于任何队伍）
            TeamData firstTeam = null;
            boolean hasFirst = false;
            
            for (BlockPos p : pointsToCheck) {
                TeamData currentTeam = TeamManager.getInstance().getTeamAt(p, level.getServer());
                
                if (!hasFirst) {
                    firstTeam = currentTeam;
                    hasFirst = true;
                } else {
                    // 检查是否与第一个点的归属一致
                    // 1. 如果一个有队伍，一个没队伍 -> 不一致
                    // 2. 如果都有队伍，但队伍不同 -> 不一致
                    if (firstTeam != currentTeam) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }
}
