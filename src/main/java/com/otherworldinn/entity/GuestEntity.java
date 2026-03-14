package com.otherworldinn.entity;

import com.otherworldinn.world.economy.ItemSellPriceManager;
import com.otherworldinn.world.inn.GuestData;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import com.simibubi.create.content.redstone.deskBell.DeskBellBlockEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import com.otherworldinn.util.GuestNameManager;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

/**
 * 旅客实体
 * <p>
 * 抽象父类实体，存储旅客数据。
 * </p>
 */
public abstract class GuestEntity extends PathfinderMob {

    private static final EntityDataAccessor<Integer> SKIN_VARIANT = SynchedEntityData.defineId(GuestEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GUEST_STATE = SynchedEntityData.defineId(GuestEntity.class, EntityDataSerializers.INT);
    // 搜索“餐台”的范围：以旅客为中心 32 格
    private static final int DINING_SEARCH_RADIUS = 32;
    // 平均一天触发 3 次：24000 / 3 = 8000 tick
    private static final int DINING_AVERAGE_INTERVAL = 8000;
    private static final int DINING_BUDGET_MIN = 6;
    private static final int DINING_BUDGET_MAX = 42;
    private static final int MIN_DAILY_DINING_ATTEMPTS = 3;
    private static final int NAVIGATION_STUCK_TIMEOUT_TICKS = 200;
    private static final double NAVIGATION_PROGRESS_THRESHOLD_SQR = 0.0625D;
    private static final ResourceLocation CREATE_DEPOT_ID = ResourceLocation.fromNamespaceAndPath("create", "depot");
    private static final ResourceLocation CREATE_WEIGHTED_EJECTOR_ID = ResourceLocation.fromNamespaceAndPath("create", "weighted_ejector");

    /**
     * 旅客数据
     */
    @Getter
    private GuestData guestData;
    
    /**
     * 导航目标
     */
    private BlockPos navigationTarget;

    /**
     * 生成延迟计数器
     */
    private int spawnDelay = 0;
    @Getter
    private int budget;
    private int navigationStuckTicks = 0;
    private double lastNavigationDistanceSqr = Double.MAX_VALUE;
    private long diningPlanDay = Long.MIN_VALUE;
    private int dailySpendTarget = 0;
    private int dailySpentCoins = 0;
    private int dailyPurchaseTarget = 0;
    private int dailyPurchaseCount = 0;
    private int dailyPurchaseAttemptCount = 0;
    // 下一次尝试“用餐购买”的时间戳（游戏刻）
    private long nextDiningAttemptTime = 0L;
    @Nullable
    // 当前已经锁定、正在前往的餐台坐标
    private BlockPos pendingDiningTarget;

    protected GuestEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // 初始化旅客数据
        long currentTime = level.getGameTime();
        this.guestData = new GuestData(this.getUUID(), currentTime + getStayDuration());
        // 初始化默认偏好
        this.initGuestPreferences();
        // 初始化奖励物品
        this.initRewardItems();
        this.budget = this.generateInitialBudget();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        spawnData = super.finalizeSpawn(level, difficulty, reason, spawnData);
        
        // 如果没有自定义名称，则设置一个随机名称
        if (!this.hasCustomName()) {
            this.setCustomName(GuestNameManager.getRandomName(this.getRandom()));
        }
        
        return spawnData;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation navigation = new GroundPathNavigation(this, level);
        navigation.setCanOpenDoors(true);
        return navigation;
    }

    private class MoveToTargetGoal extends Goal {
        private int recalculateDelay;

        public MoveToTargetGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return GuestEntity.this.navigationTarget != null;
        }

        @Override
        public void start() {
            this.recalculateDelay = 0;
            moveToTarget();
        }

        @Override
        public void tick() {
            if (GuestEntity.this.navigationTarget != null) {
                double distSqr = GuestEntity.this.distanceToSqr(
                    GuestEntity.this.navigationTarget.getX() + 0.5, 
                    GuestEntity.this.navigationTarget.getY(), 
                    GuestEntity.this.navigationTarget.getZ() + 0.5
                );
                
                // 如果距离小于 2 格 (平方 < 4)，认为到达
                if (distSqr < 4.0D) {
                    GuestEntity.this.clearNavigationTarget();
                    return;
                }
                if (GuestEntity.this.lastNavigationDistanceSqr - distSqr > NAVIGATION_PROGRESS_THRESHOLD_SQR) {
                    GuestEntity.this.navigationStuckTicks = 0;
                } else {
                    GuestEntity.this.navigationStuckTicks++;
                    if (GuestEntity.this.getNavigation().isDone()) {
                        GuestEntity.this.navigationStuckTicks += 2;
                    }
                }
                GuestEntity.this.lastNavigationDistanceSqr = distSqr;
                if (GuestEntity.this.navigationStuckTicks >= NAVIGATION_STUCK_TIMEOUT_TICKS) {
                    GuestEntity.this.clearNavigationTarget();
                    return;
                }

                // 定期重新计算路径 (每 20 tick / 1秒)
                if (--this.recalculateDelay <= 0) {
                    this.recalculateDelay = 20;
                    moveToTarget();
                }
            }
        }
        
        private void moveToTarget() {
            if (GuestEntity.this.navigationTarget != null) {
                GuestEntity.this.getNavigation().moveTo(
                    GuestEntity.this.navigationTarget.getX() + 0.5, 
                    GuestEntity.this.navigationTarget.getY(), 
                    GuestEntity.this.navigationTarget.getZ() + 0.5, 
                    1.0D
                );
            }
        }
    }

    /**
     * 获取旅客皮肤纹理
     * <p>
     * 子类必须实现此方法以提供特定的纹理。
     * </p>
     *
     * @return 纹理资源位置
     */
    public abstract ResourceLocation getSkinTexture();

    /**
     * 获取模型类型
     * <p>
     * 返回 "default" (Steve) 或 "slim" (Alex)。
     * 默认为 "default"。
     * </p>
     *
     * @return 模型类型字符串
     */
    public String getModelType() {
        return "default";
    }

    /**
     * 创建旅客属性
     *
     * @return 属性构建器
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 64.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    /**
     * 获取旅客停留时长（ticks）
     * <p>
     * 默认为 1 Minecraft 天 (24000 ticks)。
     * 子类可覆盖此方法以设定特定的停留时间。
     * </p>
     *
     * @return 停留时长 (ticks)
     */
    protected long getStayDuration() {
        return 24000L;
    }

    /**
     * 初始化旅客偏好
     * <p>
     * 子类可覆盖此方法以设定特定的房间偏好。
     * 默认所有属性偏好均为 0-100 (无限制)。
     * </p>
     */
    protected void initGuestPreferences() {
        this.guestData.setComfortPreference(0, 100);
        this.guestData.setLightPreference(0, 100);
        this.guestData.setHumidityPreference(0, 100);
    }

    /**
     * 初始化奖励物品
     * <p>
     * 子类可覆盖此方法以添加特定的奖励物品。
     * 默认无奖励。
     * 示例：this.guestData.addRewardItem(Items.EMERALD, 1, 3);
     * </p>
     */
    protected void initRewardItems() {
        // 默认无奖励，由子类实现
    }

    public void setNavigationTarget(BlockPos pos) {
        this.navigationTarget = pos;
        this.navigationStuckTicks = 0;
        this.lastNavigationDistanceSqr = Double.MAX_VALUE;
    }

    private void clearNavigationTarget() {
        this.navigationTarget = null;
        this.navigationStuckTicks = 0;
        this.lastNavigationDistanceSqr = Double.MAX_VALUE;
        this.getNavigation().stop();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 优先级 1: 移动到目标 (退房离开)
        this.goalSelector.addGoal(1, new MoveToTargetGoal());
        // 优先级 1: 开门
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        // 优先级 2: 寻找旅社 (生成后)
        this.goalSelector.addGoal(2, new FindInnGoal());
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6D));
    }

    private class FindInnGoal extends Goal {
        private BlockPos targetInnPos;
        private int recalculatePathDelay;

        public FindInnGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // 延迟执行
            if (GuestEntity.this.spawnDelay < 60) return false;

            // 只有处于空闲状态且没有导航目标时才寻找旅社
            if (GuestEntity.this.guestData.getState() != GuestData.GuestState.IDLE) return false;
            if (GuestEntity.this.navigationTarget != null) return false;
            
            // 如果已经在旅社范围内，则不需要寻找
            if (isInInnRange()) return false;

            // 查找最近的队伍旅社
            if (targetInnPos == null && GuestEntity.this.level() instanceof ServerLevel serverLevel) {
                findNearestInn(serverLevel);
            }
            return targetInnPos != null;
        }

        private void findNearestInn(ServerLevel serverLevel) {
            TeamData team = TeamManager.getInstance().getNearestInn(GuestEntity.this.blockPosition(), serverLevel.getServer());
            if (team != null && !team.getInnRegions().isEmpty()) {
                // 取第一个区域的中心作为目标
                TeamData.InnRegion region = team.getInnRegions().get(0);
                targetInnPos = new BlockPos((region.minX() + region.maxX()) / 2, 64, (region.minZ() + region.maxZ()) / 2);
            }
        }

        @Override
        public void start() {
            if (targetInnPos != null) {
                GuestEntity.this.getNavigation().moveTo(targetInnPos.getX(), targetInnPos.getY(), targetInnPos.getZ(), 1.0D);
                this.recalculatePathDelay = 0;
            }
        }

        @Override
        public boolean canContinueToUse() {
            // 如果已经在范围内，或者状态不再是 IDLE，停止
            if (isInInnRange() || GuestEntity.this.guestData.getState() != GuestData.GuestState.IDLE) {
                return false;
            }
            return targetInnPos != null;
        }

        @Override
        public void tick() {
            // 每5tick检查一次范围
            if (GuestEntity.this.tickCount % 5 == 0) {
                // 检查是否进入了旅社范围
                if (isInInnRange()) {
                    // 停止移动
                    GuestEntity.this.getNavigation().stop();
                    targetInnPos = null;
                    return;
                }
            }

            // 重新计算路径逻辑 (每 40 tick / 2秒)
            if (--this.recalculatePathDelay <= 0) {
                this.recalculatePathDelay = 40;
                if (targetInnPos != null) {
                    if (GuestEntity.this.getNavigation().isDone()) {
                        // 如果导航完成了但还没到，尝试重新寻找目标并移动
                        if (GuestEntity.this.level() instanceof ServerLevel serverLevel) {
                            findNearestInn(serverLevel);
                        }
                    }
                    GuestEntity.this.getNavigation().moveTo(targetInnPos.getX(), targetInnPos.getY(), targetInnPos.getZ(), 1.0D);
                }
            }
        }
        
        private boolean isInInnRange() {
            if (GuestEntity.this.level() instanceof ServerLevel serverLevel) {
                TeamData team = TeamManager.getInstance().getTeamAt(GuestEntity.this.blockPosition(), serverLevel.getServer());
                return team != null;
            }
            return false;
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // 旅客不可被伤害，除非是创造模式玩家、虚空伤害或指令kill
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isCreativePlayer();
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            // 获取当前位置的队伍/旅社
            TeamData team = TeamManager.getInstance().getTeamAt(this.blockPosition(), serverLevel.getServer());
            if (team != null) {
                InnData innData = team.getInnData();
                // 强制退房，标记为非正常退房（不支付房费）
                innData.checkOut(this.getUUID(), serverLevel, false);
            }
        }
    }

    private void scheduleNextDiningAttempt(ServerLevel level) {
        int intervalBase = this.getBudgetBasedDiningInterval();
        int next = intervalBase / 2 + this.getRandom().nextInt(intervalBase + 1);
        this.nextDiningAttemptTime = level.getGameTime() + next;
    }

    private boolean isDiningDisplay(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return CREATE_DEPOT_ID.equals(id) || CREATE_WEIGHTED_EJECTOR_ID.equals(id);
    }

    @Nullable
    private IItemHandler getDisplayItemHandler(ServerLevel level, BlockPos pos, BlockState state) {
        // 统一通过方块物品能力读取 Create 置物台/弹射置物台上的展示物
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, null);
    }

    private boolean hasSellableItem(ServerLevel level, BlockPos pos, BlockState state) {
        IItemHandler itemHandler = getDisplayItemHandler(level, pos, state);
        if (itemHandler == null) {
            return false;
        }
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty() && ItemSellPriceManager.getPrice(stack) > 0) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private BlockPos findNearbyDiningDisplay(ServerLevel level) {
        // 仅在“触发购买时刻”执行全量扫描，平时不做，避免频繁性能开销
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos origin = this.blockPosition();
        for (int x = -DINING_SEARCH_RADIUS; x <= DINING_SEARCH_RADIUS; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -DINING_SEARCH_RADIUS; z <= DINING_SEARCH_RADIUS; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (origin.distSqr(pos) > DINING_SEARCH_RADIUS * DINING_SEARCH_RADIUS) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (!isDiningDisplay(state)) {
                        continue;
                    }
                    if (hasSellableItem(level, pos, state)) {
                        candidates.add(pos.immutable());
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(this.getRandom().nextInt(candidates.size()));
    }

    private boolean tryPurchaseFromDisplay(ServerLevel level, BlockPos pos) {
        // 以餐台位置归属队伍，避免旅客站位边界导致入账队伍错误
        TeamData team = TeamManager.getInstance().getTeamAt(pos, level.getServer());
        if (team == null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        IItemHandler itemHandler = getDisplayItemHandler(level, pos, state);
        if (itemHandler == null) {
            return false;
        }

        List<Integer> sellableSlots = new ArrayList<>();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int unitPrice = ItemSellPriceManager.getPrice(stack);
            if (unitPrice > 0 && unitPrice <= this.budget) {
                sellableSlots.add(slot);
            }
        }
        if (sellableSlots.isEmpty()) {
            return false;
        }

        int slot = sellableSlots.get(this.getRandom().nextInt(sellableSlots.size()));
        ItemStack slotStack = itemHandler.getStackInSlot(slot);
        int unitPrice = ItemSellPriceManager.getPrice(slotStack);
        if (unitPrice <= 0 || unitPrice > this.budget) {
            return false;
        }
        int remainingToday = Math.max(1, this.dailySpendTarget - this.dailySpentCoins);
        int desiredSpend = Math.min(this.getBudgetBasedDesiredSpend(), remainingToday + unitPrice);
        int maxCountByBudget = this.budget / unitPrice;
        int maxCountByDailyTarget = Math.max(1, (remainingToday + unitPrice - 1) / unitPrice);
        int desiredCount = Math.max(1, desiredSpend / unitPrice);
        int buyCount = Math.min(slotStack.getCount(), Math.min(Math.min(maxCountByBudget, maxCountByDailyTarget), desiredCount));
        if (buyCount <= 0) {
            return false;
        }
        ItemStack simulated = itemHandler.extractItem(slot, buyCount, true);
        if (simulated.isEmpty()) {
            return false;
        }

        ItemStack purchased = itemHandler.extractItem(slot, simulated.getCount(), false);
        if (purchased.isEmpty()) {
            return false;
        }

        int paidUnitPrice = ItemSellPriceManager.getPrice(purchased);
        if (paidUnitPrice <= 0) {
            return false;
        }

        int totalCost = paidUnitPrice * purchased.getCount();
        team.addCoins(totalCost, level.getServer());
        TeamManager.getInstance().syncTeam(team, level.getServer());
        this.dailySpentCoins += totalCost;
        this.dailyPurchaseCount += 1;
        return true;
    }

    private void handleDiningPurchase(ServerLevel level) {
        if (this.guestData.getState() != GuestData.GuestState.CHECKED_IN) {
            this.pendingDiningTarget = null;
            return;
        }
        this.ensureDailyDiningPlan(level);
        if (this.hasReachedDailyDiningTargets()) {
            this.pendingDiningTarget = null;
            this.navigationTarget = null;
            this.getNavigation().stop();
            long nextDayStart = (this.diningPlanDay + 1L) * 24000L;
            this.nextDiningAttemptTime = Math.max(this.nextDiningAttemptTime, nextDayStart + this.getRandom().nextInt(200));
            return;
        }

        if (this.pendingDiningTarget != null) {
            double distSqr = this.distanceToSqr(this.pendingDiningTarget.getX() + 0.5, this.pendingDiningTarget.getY(), this.pendingDiningTarget.getZ() + 0.5);
            if (distSqr <= 4.0D) {
                // 到达餐台后立即尝试购买一次（成功或失败都进入下一轮冷却）
                this.dailyPurchaseAttemptCount += 1;
                tryPurchaseFromDisplay(level, this.pendingDiningTarget);
                this.pendingDiningTarget = null;
                this.clearNavigationTarget();
                scheduleNextDiningAttempt(level);
            } else if (this.navigationTarget == null) {
                this.pendingDiningTarget = null;
                this.nextDiningAttemptTime = level.getGameTime() + this.getBudgetBasedRetryInterval();
            }
            return;
        }

        if (this.nextDiningAttemptTime == 0L) {
            this.nextDiningAttemptTime = level.getGameTime() + this.getInitialDiningAttemptDelay();
            return;
        }
        // 避免在已有导航任务时插入餐饮任务，减少行为冲突
        if (level.getGameTime() < this.nextDiningAttemptTime || this.navigationTarget != null) {
            return;
        }
        if (!this.shouldAttemptDiningPurchaseNow()) {
            this.nextDiningAttemptTime = level.getGameTime() + this.getBudgetBasedRetryInterval();
            return;
        }

        BlockPos target = findNearbyDiningDisplay(level);
        if (target == null) {
            this.nextDiningAttemptTime = level.getGameTime() + this.getBudgetBasedRetryInterval();
            return;
        }

        this.pendingDiningTarget = target;
        this.setNavigationTarget(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            spawnDelay++;
            this.guestData.tick();
            
            // 检测是否进入旅社范围并触发登记
            if (this.tickCount % 20 == 0 && this.guestData.getState() == GuestData.GuestState.IDLE) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    TeamData team = TeamManager.getInstance().getTeamAt(this.blockPosition(), serverLevel.getServer());
                    if (team != null) {
                         // 旅客在旅社范围内，触发进入旅社逻辑
                         team.getInnData().addGuest(this, team, serverLevel);
                         BlockPos bellPos = this.findNearestDeskBellInInn(serverLevel, team);
                         if (bellPos != null) {
                             this.setNavigationTarget(bellPos);
                         } else {
                             this.getNavigation().stop();
                         }
                    }
                }
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                handleDiningPurchase(serverLevel);
            }

            // 同步状态到 SynchedEntityData
            int currentStateOrdinal = this.guestData.getState().ordinal();
            if (this.entityData.get(GUEST_STATE) != currentStateOrdinal) {
                this.entityData.set(GUEST_STATE, currentStateOrdinal);
            }
            
            // 发光逻辑：等待入住时发光
            if (this.guestData.getState() == GuestData.GuestState.WAITING) {
                if (!this.hasGlowingTag()) {
                    this.setGlowingTag(true);
                }
            } else {
                if (this.hasGlowingTag()) {
                    this.setGlowingTag(false);
                }
            }
        } else {
            // 客户端：从 SynchedEntityData 更新 GuestData 状态
            int syncedStateOrdinal = this.entityData.get(GUEST_STATE);
            if (syncedStateOrdinal >= 0 && syncedStateOrdinal < GuestData.GuestState.values().length) {
                GuestData.GuestState syncedState = GuestData.GuestState.values()[syncedStateOrdinal];
                if (this.guestData.getState() != syncedState) {
                    this.guestData.setState(syncedState);
                }
            }
            
            // 客户端发光逻辑 (虽然 glowing tag 会自动同步，但这里双重保险或用于其他客户端效果)
            // 注意：setGlowingTag 主要由服务端控制，客户端设置可能只在本地生效
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN_VARIANT, 0);
        builder.define(GUEST_STATE, GuestData.GuestState.IDLE.ordinal());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() && player.isShiftKeyDown() && !this.level().isClientSide && player.isCreative()) {
            // 调试信息
            player.sendSystemMessage(Component.literal("--- Guest Debug Info ---"));
            player.sendSystemMessage(Component.literal("UUID: " + this.getUUID()));
            player.sendSystemMessage(Component.literal("State: " + this.guestData.getState()));
            player.sendSystemMessage(Component.literal("Synced State: " + this.entityData.get(GUEST_STATE)));
            player.sendSystemMessage(Component.literal("Room ID: " + this.guestData.getRoomId()));
            
            TeamData team = TeamManager.getInstance().getTeamAt(this.blockPosition(), this.level().getServer());
            player.sendSystemMessage(Component.literal("In Inn Range: " + (team != null)));
            if (team != null) {
                player.sendSystemMessage(Component.literal("Team ID: " + team.getTeamId()));
            }
            
            player.sendSystemMessage(Component.literal("Navigation Target: " + (this.navigationTarget != null ? this.navigationTarget.toShortString() : "null")));
            player.sendSystemMessage(Component.literal("Spawn Delay: " + this.spawnDelay));
            
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    public int getSkinVariant() {
        return this.entityData.get(SKIN_VARIANT);
    }

    public void setSkinVariant(int variant) {
        this.entityData.set(SKIN_VARIANT, variant);
    }

    protected void setBudget(int budget) {
        this.budget = Mth.clamp(budget, DINING_BUDGET_MIN, DINING_BUDGET_MAX);
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("SkinVariant", this.getSkinVariant());
        compound.putInt("Budget", this.budget);
        compound.putLong("DiningPlanDay", this.diningPlanDay);
        compound.putInt("DailySpendTarget", this.dailySpendTarget);
        compound.putInt("DailySpentCoins", this.dailySpentCoins);
        compound.putInt("DailyPurchaseTarget", this.dailyPurchaseTarget);
        compound.putInt("DailyPurchaseCount", this.dailyPurchaseCount);
        compound.putInt("DailyPurchaseAttemptCount", this.dailyPurchaseAttemptCount);
        // 将 GuestData 保存到 NBT 中
        CompoundTag guestTag = new CompoundTag();
        this.guestData.save(guestTag);
        compound.put("GuestData", guestTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("SkinVariant")) {
            this.setSkinVariant(compound.getInt("SkinVariant"));
        }
        if (compound.contains("Budget")) {
            this.budget = Mth.clamp(compound.getInt("Budget"), DINING_BUDGET_MIN, DINING_BUDGET_MAX);
        } else {
            this.budget = this.generateInitialBudget();
        }
        this.diningPlanDay = compound.contains("DiningPlanDay") ? compound.getLong("DiningPlanDay") : Long.MIN_VALUE;
        this.dailySpendTarget = Math.max(0, compound.getInt("DailySpendTarget"));
        this.dailySpentCoins = Math.max(0, compound.getInt("DailySpentCoins"));
        this.dailyPurchaseTarget = Math.max(0, compound.getInt("DailyPurchaseTarget"));
        this.dailyPurchaseCount = Math.max(0, compound.getInt("DailyPurchaseCount"));
        this.dailyPurchaseAttemptCount = Math.max(0, compound.getInt("DailyPurchaseAttemptCount"));
        // 从 NBT 加载 GuestData
        if (compound.contains("GuestData")) {
            CompoundTag guestTag = compound.getCompound("GuestData");
            this.guestData = GuestData.load(guestTag);
            // 初始同步状态
            this.entityData.set(GUEST_STATE, this.guestData.getState().ordinal());
        }
    }

    private int generateInitialBudget() {
        return DINING_BUDGET_MIN + this.getRandom().nextInt(DINING_BUDGET_MAX - DINING_BUDGET_MIN + 1);
    }

    private int getBudgetBasedDiningInterval() {
        float factor = Mth.clamp(18.0f / (float) this.budget, 0.45f, 1.9f);
        return Math.max(1200, (int) (DINING_AVERAGE_INTERVAL * factor));
    }

    private int getBudgetBasedRetryInterval() {
        int base = Math.max(300, this.getBudgetBasedDiningInterval() / 4);
        return base + this.getRandom().nextInt(base + 1);
    }

    private int getBudgetBasedDesiredSpend() {
        int minSpend = Math.max(1, this.budget / 3);
        int variable = Math.max(1, this.budget - minSpend + 1);
        return minSpend + this.getRandom().nextInt(variable);
    }

    private boolean shouldAttemptDiningPurchaseNow() {
        if (this.dailyPurchaseAttemptCount < MIN_DAILY_DINING_ATTEMPTS && !this.hasReachedDailyDiningTargets()) {
            return true;
        }
        float normalized = (this.budget - DINING_BUDGET_MIN) / (float) (DINING_BUDGET_MAX - DINING_BUDGET_MIN);
        float spendProgress = this.dailySpendTarget <= 0 ? 0.0f : Mth.clamp(this.dailySpentCoins / (float) this.dailySpendTarget, 0.0f, 1.0f);
        float remainingIntent = 1.0f - spendProgress;
        float desire = (0.3f + normalized * 0.7f) * (0.35f + remainingIntent * 0.65f);
        return this.getRandom().nextFloat() < desire;
    }

    private void ensureDailyDiningPlan(ServerLevel level) {
        long currentDay = level.getGameTime() / 24000L;
        if (this.diningPlanDay == currentDay) {
            return;
        }
        this.diningPlanDay = currentDay;
        this.dailySpentCoins = 0;
        this.dailyPurchaseCount = 0;
        this.dailyPurchaseAttemptCount = 0;

        float spendRatio = (float) (0.7 + this.getRandom().nextGaussian() * 0.15);
        spendRatio = Mth.clamp(spendRatio, 0.25f, 1.0f);
        this.dailySpendTarget = Math.max(1, Math.round(this.budget * spendRatio));

        float normalized = (this.budget - DINING_BUDGET_MIN) / (float) (DINING_BUDGET_MAX - DINING_BUDGET_MIN);
        float meanCount = 1.2f + normalized * 3.2f;
        int sampledCount = Math.max(1, Math.round((float) (meanCount + this.getRandom().nextGaussian() * 0.9f)));
        this.dailyPurchaseTarget = Math.max(1, Math.min(this.dailySpendTarget, sampledCount));
    }

    private boolean hasReachedDailyDiningTargets() {
        if (this.dailyPurchaseAttemptCount < MIN_DAILY_DINING_ATTEMPTS) {
            return false;
        }
        return this.dailySpentCoins >= this.dailySpendTarget || this.dailyPurchaseCount >= this.dailyPurchaseTarget;
    }

    private int getInitialDiningAttemptDelay() {
        float normalized = (this.budget - DINING_BUDGET_MIN) / (float) (DINING_BUDGET_MAX - DINING_BUDGET_MIN);
        int base = Mth.floor(Mth.lerp(1.0f - normalized, 200.0f, 900.0f));
        return base + this.getRandom().nextInt(161);
    }

    @Nullable
    private BlockPos findNearestDeskBellInInn(ServerLevel level, TeamData team) {
        BlockPos origin = this.blockPosition();
        int nearMinY = Math.max(level.getMinBuildHeight(), origin.getY() - 16);
        int nearMaxY = Math.min(level.getMaxBuildHeight() - 1, origin.getY() + 16);
        BlockPos near = this.findNearestDeskBellInInn(level, team, nearMinY, nearMaxY);
        if (near != null) {
            return near;
        }
        return this.findNearestDeskBellInInn(level, team, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
    }

    @Nullable
    private BlockPos findNearestDeskBellInInn(ServerLevel level, TeamData team, int minY, int maxY) {
        BlockPos origin = this.blockPosition();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockPos bestPos = null;
        double bestDist = Double.MAX_VALUE;
        for (TeamData.InnRegion region : team.getInnRegions()) {
            for (int x = region.minX(); x <= region.maxX(); x++) {
                for (int z = region.minZ(); z <= region.maxZ(); z++) {
                    for (int y = minY; y <= maxY; y++) {
                        mutable.set(x, y, z);
                        BlockEntity blockEntity = level.getBlockEntity(mutable);
                        if (!(blockEntity instanceof DeskBellBlockEntity)) {
                            continue;
                        }
                        double dist = origin.distSqr(mutable);
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestPos = mutable.immutable();
                        }
                    }
                }
            }
        }
        return bestPos;
    }
}
