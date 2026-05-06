package com.otherworldinn.world.event.listener;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.foundation.ModBlockProperties;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.inn.facility.FacilityRegistry;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

/**
 * 城镇保护处理器
 *
 * <p>在城镇维度中，默认禁止破坏和放置方块。 只有在玩家所属队伍的“旅社”范围内才允许建筑。 同时禁止使用特定的物品。
 */
@EventBusSubscriber(modid = OtherworldInn.MODID)
public class TownProtectionHandler {
    private static final double TOWN_CROP_GROWTH_MULTIPLIER = 0.3D;
    private static final double GREENHOUSE_CROP_GROWTH_MULTIPLIER = 1.5D;
    private static final String GREENHOUSE_FACILITY_ID = "greenhouse";

    /**
     * 检查是否可以在指定位置建筑（针对玩家），如果不可以则返回拒绝原因
     *
     * @return 拒绝原因的组件，如果允许则返回 null
     */
    private static Component getBuildDenyReason(Player player, BlockPos pos, Level level) {
        // 仅在城镇维度生效
        if (level.dimension() != TownDimensions.TOWN_LEVEL) {
            return null;
        }

        // 创造模式豁免
        if (player.isCreative()) {
            return null;
        }

        TeamData team = TeamManager.getInstance().getPlayerTeam(player);
        if (isInsideGreenhouseExtraBuildAllowRange(team, pos)) {
            return null;
        }

        // 情况 1: 没有队伍 或 不在旅社区域内 -> 通用保护提示
        if (team == null || !isInsideInnZone(team, pos)) {
            return Component.translatable("message.otherworldinn.protection.deny");
        }

        // 情况 2: 在旅社区域内，但未开启编辑模式 -> 装修提示
        if (team.getInnData().getState() != InnData.InnState.EDIT_MODE) {
            return Component.translatable("message.otherworldinn.protection.deny_renovation");
        }

        return null; // 允许
    }

    /** 检查方块是否属于农作物白名单 */
    private static boolean isFarmingBlock(BlockState state) {
        if (state == null) return false;
        return state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof StemBlock
                || state.getBlock() instanceof AttachedStemBlock
                || state.getBlock() instanceof NetherWartBlock
                || state.getBlock() instanceof CocoaBlock
                || state.getBlock() instanceof SweetBerryBushBlock
                || state.getBlock() instanceof FarmBlock
                || state.is(net.minecraft.tags.BlockTags.CROPS)
                || state.is(net.minecraft.tags.BlockTags.MAINTAINS_FARMLAND);
    }

    private static boolean isBedSheetCleaningUpdate(Player player, BlockState state) {
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.BedBlock)
                || !state.hasProperty(ModBlockProperties.MESSY)) {
            return false;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(ModItems.BED_SHEET.get())
                || off.is(ModItems.BED_SHEET.get())
                || main.is(ModItems.MESSY_BED_SHEET.get())
                || off.is(ModItems.MESSY_BED_SHEET.get());
    }

    private static boolean isTownDimension(Level level) {
        return level.dimension() == TownDimensions.TOWN_LEVEL;
    }

    private static boolean isIgnitionFireBlock(BlockState state) {
        return state != null && state.is(BlockTags.FIRE);
    }

    private static boolean isInnZonePos(ServerLevel level, BlockPos pos) {
        return TeamManager.getInstance().getTeamAt(pos, level.getServer()) != null;
    }

    private static boolean isInsideGreenhouseExtraBuildAllowRange(TeamData team, BlockPos pos) {
        if (team == null || pos == null) {
            return false;
        }
        int greenhouseLevel = Math.max(0, team.getInnData().getFacilityLevel(GREENHOUSE_FACILITY_ID));
        if (greenhouseLevel <= 0) {
            return false;
        }
        FacilityRegistry.FacilityDefinition greenhouse = FacilityRegistry.get(GREENHOUSE_FACILITY_ID);
        if (greenhouse == null) {
            return false;
        }
        for (FacilityRegistry.FacilityRange range : greenhouse.getExtraBuildAllowRanges(greenhouseLevel)) {
            if (range.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    private static int getGreenhouseLevelAtPos(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || pos == null || !isTownDimension(level)) {
            return 0;
        }
        FacilityRegistry.FacilityDefinition greenhouse = FacilityRegistry.get(GREENHOUSE_FACILITY_ID);
        if (greenhouse == null) {
            return 0;
        }
        TeamManager manager = TeamManager.getInstance();
        TeamData teamAtPos = manager.getTeamAt(pos, serverLevel.getServer());
        int levelAtPos = getGreenhouseLevelForTeamAtPos(teamAtPos, greenhouse, pos);
        if (levelAtPos > 0) {
            return levelAtPos;
        }
        TeamData nearestTeam = manager.getNearestInn(pos, serverLevel.getServer());
        if (nearestTeam == null || nearestTeam == teamAtPos) {
            return 0;
        }
        return getGreenhouseLevelForTeamAtPos(nearestTeam, greenhouse, pos);
    }

    private static int getGreenhouseLevelForTeamAtPos(
            TeamData team, FacilityRegistry.FacilityDefinition greenhouse, BlockPos pos) {
        if (team == null || greenhouse == null || pos == null) {
            return 0;
        }
        int greenhouseLevel = Math.max(0, team.getInnData().getFacilityLevel(GREENHOUSE_FACILITY_ID));
        if (greenhouseLevel <= 0) {
            return 0;
        }
        if (greenhouse.facilityRange().contains(pos)) {
            return greenhouseLevel;
        }
        for (FacilityRegistry.FacilityRange range : greenhouse.getExtraBuildAllowRanges(greenhouseLevel)) {
            if (range.contains(pos)) {
                return greenhouseLevel;
            }
        }
        return 0;
    }

    private static double getCropGrowthMultiplier(Level level, BlockPos pos, BlockState state) {
        if (!isTownDimension(level)) {
            return 1.0D;
        }
        if (state != null
                && (state.getBlock() instanceof SaplingBlock
                        || state.is(net.minecraft.tags.BlockTags.SAPLINGS))) {
            return 0.0D;
        }
        int greenhouseLevel = getGreenhouseLevelAtPos(level, pos);
        if (greenhouseLevel > 0) {
            return GREENHOUSE_CROP_GROWTH_MULTIPLIER + (1.0D * (greenhouseLevel - 1));
        }
        return TOWN_CROP_GROWTH_MULTIPLIER;
    }

    private static boolean shouldRestrictNaturalGrowthBlock(BlockState state) {
        if (state == null) {
            return false;
        }
        if (isFarmingBlock(state)) {
            return true;
        }
        return state.is(net.minecraft.world.level.block.Blocks.PUMPKIN)
                || state.is(net.minecraft.world.level.block.Blocks.MELON)
                || state.is(net.minecraft.world.level.block.Blocks.SUGAR_CANE)
                || state.is(net.minecraft.world.level.block.Blocks.BAMBOO)
                || state.is(net.minecraft.world.level.block.Blocks.CACTUS)
                || state.is(net.minecraft.world.level.block.Blocks.COCOA)
                || state.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH);
    }

    private static boolean isNormalTownDecorArea(ServerLevel level, BlockPos pos) {
        return isTownDimension(level) && !isInnZonePos(level, pos) && getGreenhouseLevelAtPos(level, pos) <= 0;
    }

    private static boolean willSelfUpdateDestroyBlock(
            ServerLevel level, BlockPos pos, BlockState currentState, Direction notifiedSide) {
        BlockPos neighborPos = pos.relative(notifiedSide);
        BlockState neighborState = level.getBlockState(neighborPos);
        BlockState updatedState =
                currentState.updateShape(notifiedSide, neighborState, level, pos, neighborPos);
        return updatedState.isAir();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPreventDestructiveNeighborUpdate(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isTownDimension(level)) {
            return;
        }

        BlockPos pos = event.getPos();
        if (!isNormalTownDecorArea(level, pos)) {
            return;
        }
        BlockState currentState = level.getBlockState(pos);
        if (currentState.isAir()) {
            return;
        }

        // 场景 1：当前方块已失去生存条件，更新将导致其被破坏。
        if (!currentState.canSurvive(level, pos)) {
            event.setCanceled(true);
            return;
        }

        // 场景 2：物理更新结算会把当前方块更新为空气（被破坏）。
        for (Direction side : event.getNotifiedSides()) {
            if (willSelfUpdateDestroyBlock(level, pos, currentState, side)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockGrowFeature(BlockGrowFeatureEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isTownDimension(level)) {
            return;
        }
        BlockState stateAtPos = level.getBlockState(event.getPos());
        if (stateAtPos.getBlock() instanceof SaplingBlock || stateAtPos.is(net.minecraft.tags.BlockTags.SAPLINGS)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCropGrowPre(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !isTownDimension(serverLevel)) {
            return;
        }
        double multiplier = getCropGrowthMultiplier(serverLevel, event.getPos(), event.getState());
        if (multiplier <= 0.0D) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            return;
        }
        if (multiplier < 1.0D && serverLevel.random.nextDouble() >= multiplier) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCropGrowPost(CropGrowEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !isTownDimension(serverLevel)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = serverLevel.getBlockState(pos);
        if (state.getBlock() instanceof SaplingBlock || state.is(net.minecraft.tags.BlockTags.SAPLINGS)) {
            return;
        }
        double multiplier = getCropGrowthMultiplier(serverLevel, pos, state);
        if (multiplier <= 1.0D) {
            return;
        }
        double extraGrowth = multiplier - 1.0D;
        int guaranteedExtraSteps = (int) Math.floor(extraGrowth);
        double fractionalChance = extraGrowth - guaranteedExtraSteps;
        int extraSteps = guaranteedExtraSteps;
        if (fractionalChance > 0.0D && serverLevel.random.nextDouble() < fractionalChance) {
            extraSteps++;
        }
        for (int i = 0; i < extraSteps; i++) {
            BlockState current = serverLevel.getBlockState(pos);
            if (!tryApplyExtraGrowth(serverLevel, pos, current)) {
                break;
            }
        }
    }

    private static boolean tryApplyExtraGrowth(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            if (!crop.isMaxAge(state)) {
                level.setBlockAndUpdate(pos, crop.getStateForAge(crop.getAge(state) + 1));
                return true;
            }
            return false;
        }
        if (state.getBlock() instanceof StemBlock && state.hasProperty(StemBlock.AGE)) {
            int age = state.getValue(StemBlock.AGE);
            if (age < 7) {
                level.setBlockAndUpdate(pos, state.setValue(StemBlock.AGE, age + 1));
                return true;
            }
            return false;
        }
        if (state.getBlock() instanceof NetherWartBlock && state.hasProperty(NetherWartBlock.AGE)) {
            int age = state.getValue(NetherWartBlock.AGE);
            if (age < 3) {
                level.setBlockAndUpdate(pos, state.setValue(NetherWartBlock.AGE, age + 1));
                return true;
            }
            return false;
        }
        if (state.getBlock() instanceof CocoaBlock && state.hasProperty(CocoaBlock.AGE)) {
            int age = state.getValue(CocoaBlock.AGE);
            if (age < 2) {
                level.setBlockAndUpdate(pos, state.setValue(CocoaBlock.AGE, age + 1));
                return true;
            }
            return false;
        }
        if (state.getBlock() instanceof SweetBerryBushBlock
                && state.hasProperty(SweetBerryBushBlock.AGE)) {
            int age = state.getValue(SweetBerryBushBlock.AGE);
            if (age < 3) {
                level.setBlockAndUpdate(pos, state.setValue(SweetBerryBushBlock.AGE, age + 1));
                return true;
            }
        }
        return false;
    }

    /** 客户端事件处理器 专门用于在客户端预测阶段就拦截交互 */
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

    /** 检查坐标是否在队伍的旅社区域内 */
    private static boolean isInsideInnZone(TeamData team, BlockPos pos) {
        return team.isInInnZone(pos);
    }

    private static void sendDenyMessage(Player player, Component message) {
        // 使用 Status Bar
        player.displayClientMessage(
                message.copy().withStyle(style -> style.withColor(ModColors.RED)), true);
    }

    private static void syncInventoryIfServerPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.sendAllDataToRemote();
        }
    }

    private static void denyBuildForPlayer(Player player, Component message) {
        sendDenyMessage(player, message);
        syncInventoryIfServerPlayer(player);
    }

    private static void denyRightClickBlock(PlayerInteractEvent.RightClickBlock event, Player player) {
        event.setCanceled(true);
        event.setUseItem(TriState.FALSE);
        event.setUseBlock(TriState.FALSE);
        event.setCancellationResult(InteractionResult.FAIL);
        syncInventoryIfServerPlayer(player);
    }

    private static void denyRightClickBlock(
            PlayerInteractEvent.RightClickBlock event, Player player, Component message) {
        denyRightClickBlock(event, player);
        sendDenyMessage(player, message);
    }

    private static boolean isEditModeAllowedAt(ServerLevel level, BlockPos pos) {
        TeamData team = TeamManager.getInstance().getTeamAt(pos, level.getServer());
        return team != null && team.getInnData().getState() == InnData.InnState.EDIT_MODE;
    }

    private static void spawnFallingBlockDrop(
            ServerLevel level, FallingBlockEntity fallingBlock, BlockState state, BlockPos pos) {
        Item item = state.getBlock().asItem();
        if (item == net.minecraft.world.item.Items.AIR) {
            return;
        }
        ItemStack drop = new ItemStack(item);
        ItemEntity itemEntity =
                new ItemEntity(
                        level, fallingBlock.getX(), fallingBlock.getY(), fallingBlock.getZ(), drop);
        itemEntity.setDeltaMovement(fallingBlock.getDeltaMovement());
        level.addFreshEntity(itemEntity);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = (Level) event.getLevel();

        // 允许破坏农作物
        if (isFarmingBlock(event.getState())) {
            if (!isTownDimension(level)) {
                return;
            }
            if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
                Component denyReason = getBuildDenyReason(player, event.getPos(), level);
                if (denyReason != null) {
                    event.setCanceled(true);
                    denyBuildForPlayer(player, denyReason);
                }
                return;
            }
            // 覆盖 FakePlayer 路径
            if (level instanceof ServerLevel serverLevel && player instanceof FakePlayer) {
                if (!isEditModeAllowedAt(serverLevel, event.getPos())) {
                    event.setCanceled(true);
                }
                return;
            }
            return;
        }

        // 如果是真实玩家
        if (player instanceof ServerPlayer && !(player instanceof FakePlayer)) {
            Component denyReason = getBuildDenyReason(player, event.getPos(), level);
            if (denyReason != null) {
                event.setCanceled(true);
                sendDenyMessage(player, denyReason);
            }
        }
        // 如果是非玩家实体或 FakePlayer (自动化设备)
        else if (level instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension() == TownDimensions.TOWN_LEVEL) {
                if (!isEditModeAllowedAt(serverLevel, event.getPos())) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel
                && isTownDimension(serverLevel)
                && isIgnitionFireBlock(event.getState())) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() == null
                && event.getLevel() instanceof ServerLevel serverLevel
                && isTownDimension(serverLevel)
                && shouldRestrictNaturalGrowthBlock(event.getState())) {
            double multiplier = getCropGrowthMultiplier(serverLevel, event.getPos(), event.getState());
            if (multiplier <= 0.0D) {
                event.setCanceled(true);
                return;
            }
            if (!isInnZonePos(serverLevel, event.getPos())) {
                event.setCanceled(true);
                return;
            }
            if (multiplier < 1.0D && serverLevel.random.nextDouble() >= multiplier) {
                event.setCanceled(true);
                return;
            }
        }

        if (event.getLevel() instanceof ServerLevel serverLevel
                && serverLevel.dimension() == TownDimensions.TOWN_LEVEL
                && event.getEntity() instanceof FallingBlockEntity fallingBlock
                && TeamManager.getInstance().getTeamAt(event.getPos(), serverLevel.getServer())
                        == null) {
            event.setCanceled(true);
            spawnFallingBlockDrop(serverLevel, fallingBlock, event.getState(), event.getPos());
            fallingBlock.discard();
            return;
        }

        // 允许种植农作物
        if (isFarmingBlock(event.getState())) {
            if (!(event.getEntity() instanceof Player player)
                    || !(event.getLevel() instanceof Level level)
                    || !isTownDimension(level)) {
                return;
            }
            Component denyReason = getBuildDenyReason(player, event.getPos(), level);
            if (denyReason != null) {
                event.setCanceled(true);
                denyBuildForPlayer(player, denyReason);
            }
            return;
        }

        if (event.getEntity() instanceof Player player) {
            if (isBedSheetCleaningUpdate(player, event.getState())) {
                return;
            }
            // 如果是真实玩家
            if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
                Component denyReason =
                        getBuildDenyReason(player, event.getPos(), (Level) event.getLevel());
                if (denyReason != null) {
                    event.setCanceled(true);
                    denyBuildForPlayer(player, denyReason);
                    return;
                }
            }
            // 如果是 FakePlayer
            else if (event.getEntity() instanceof FakePlayer) {
                Level level = event.getEntity().level();
                if (level instanceof ServerLevel serverLevel
                        && level.dimension() == TownDimensions.TOWN_LEVEL) {
                    if (!isEditModeAllowedAt(serverLevel, event.getPos())) {
                        event.setCanceled(true);
                    }
                }
            }
        } else if (event.getEntity() != null) {
            // 如果是非玩家实体 (例如末影人)
            Level level = event.getEntity().level();
            if (level instanceof ServerLevel serverLevel
                    && level.dimension() == TownDimensions.TOWN_LEVEL) {
                if (!isEditModeAllowedAt(serverLevel, event.getPos())) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        ItemStack stack = event.getItemStack();

        // 1. 检查禁用物品 (仅在城镇维度)
        if (level.dimension() == TownDimensions.TOWN_LEVEL) {
            if (stack.is(OtherworldInn.BANNED_IN_TOWN)) {
                event.setCanceled(true);
                if (event.getEntity() instanceof ServerPlayer player) {
                    player.displayClientMessage(
                            Component.translatable("message.otherworldinn.protection.banned_item"),
                            true);
                }
            }
        } else {
            // 2. 检查仅限城镇维度使用的物品
            if (stack.is(OtherworldInn.ONLY_IN_TOWN)) {
                event.setCanceled(true);
                if (event.getEntity() instanceof ServerPlayer player) {
                    player.displayClientMessage(
                            Component.translatable("message.otherworldinn.protection.only_in_town"),
                            true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        ItemStack stack = event.getItemStack();

        // 1. 检查仅限城镇维度使用的物品 (如果不在城镇维度)
        if (level.dimension() != TownDimensions.TOWN_LEVEL) {
            if (stack.is(OtherworldInn.ONLY_IN_TOWN)) {
                denyRightClickBlock(event, player);
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                            Component.translatable("message.otherworldinn.protection.only_in_town"),
                            true);
                }
                return;
            }
            return; // 不在城镇维度，后续检查跳过
        }

        // --- 以下为城镇维度内的检查 ---

        // 2. 检查禁用物品
        if (stack.is(OtherworldInn.BANNED_IN_TOWN)) {
            denyRightClickBlock(event, player);
            if (player instanceof ServerPlayer serverPlayer) {
                player.displayClientMessage(
                        Component.translatable("message.otherworldinn.protection.banned_item"),
                        true);
            }
            return;
        }

        // 2.5 拦截农作物右键交互（覆盖 FTB Ultimine 右键收获路径）
        BlockState clickedState = level.getBlockState(pos);
        if (isFarmingBlock(clickedState)) {
            Component denyReason = getBuildDenyReason(player, pos, level);
            if (denyReason != null) {
                denyRightClickBlock(event, player, denyReason);
            }
            return;
        }

        // 3. 检查建筑权限 (针对放置方块的行为)
        // 只有当玩家在生存/冒险模式下，且手持方块物品时才需要提前拦截
        if (!player.isCreative()
                && !stack.isEmpty()
                && stack.getItem() instanceof BlockItem blockItem) {
            // 允许种植农作物 (例如种子)
            if (isFarmingBlock(blockItem.getBlock().defaultBlockState())) {
                Component denyReason = getBuildDenyReason(player, pos, level);
                if (denyReason != null) {
                    denyRightClickBlock(event, player, denyReason);
                }
                return;
            }

            // 计算拟放置位置
            BlockPos placePos = pos.relative(event.getFace());

            // 检查是否允许在该位置建筑
            Component denyReason = getBuildDenyReason(player, placePos, level);
            if (denyReason != null) {
                denyRightClickBlock(event, player, denyReason);
            }
        }

        if (!player.isCreative() && stack.getItem() instanceof HoeItem) {
            Component denyReason = getBuildDenyReason(player, pos, level);
            if (denyReason != null) {
                denyRightClickBlock(event, player, denyReason);
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
            if (entity instanceof LightningBolt lightningBolt) {
                lightningBolt.setVisualOnly(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPistonPre(PistonEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension() == TownDimensions.TOWN_LEVEL) {
            BlockPos pos = event.getPos();

            // 始终手动创建并解析结构，以确保获取最准确的受影响方块列表
            PistonStructureResolver resolver =
                    new PistonStructureResolver(
                            level,
                            pos,
                            event.getDirection(),
                            event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND);
            if (!resolver.resolve()) {
                return; // 推不动，无需干预
            }

            // 收集所有需要检查的位置
            Set<BlockPos> pointsToCheck = new HashSet<>();

            // 1. 活塞本身的位置
            pointsToCheck.add(pos);

            // 2. 活塞臂伸出的位置 (如果是伸出)
            if (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND) {
                pointsToCheck.add(pos.relative(event.getDirection()));
            }

            // 3. 将被破坏的方块
            pointsToCheck.addAll(resolver.getToDestroy());

            // 4. 将被移动的方块及其目标位置
            Direction moveDir =
                    (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND)
                            ? event.getDirection()
                            : event.getDirection().getOpposite();
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
