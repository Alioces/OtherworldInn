package com.otherworldinn.entity;

import com.otherworldinn.world.inn.GuestData;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import java.util.EnumSet;

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

    protected GuestEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // 初始化旅客数据
        long currentTime = level.getGameTime();
        this.guestData = new GuestData(this.getUUID(), currentTime + getStayDuration());
        // 初始化默认偏好
        this.initGuestPreferences();
        // 初始化奖励物品
        this.initRewardItems();
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
                    GuestEntity.this.navigationTarget = null;
                    GuestEntity.this.getNavigation().stop();
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
                         // 停止导航，准备等待
                         this.getNavigation().stop();
                    }
                }
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
    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("SkinVariant", this.getSkinVariant());
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
        // 从 NBT 加载 GuestData
        if (compound.contains("GuestData")) {
            CompoundTag guestTag = compound.getCompound("GuestData");
            this.guestData = GuestData.load(guestTag);
            // 初始同步状态
            this.entityData.set(GUEST_STATE, this.guestData.getState().ordinal());
        }
    }
}
