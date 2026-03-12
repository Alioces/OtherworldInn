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

/**
 * 旅客实体
 * <p>
 * 抽象父类实体，存储旅客数据。
 * </p>
 */
public abstract class GuestEntity extends PathfinderMob {

    /**
     * 旅客数据
     */
    @Getter
    private GuestData guestData;

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

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // 旅客不可被伤害，除非是创造模式玩家或虚空伤害
        return source != this.damageSources().fellOutOfWorld() && !source.isCreativePlayer();
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
            this.guestData.tick();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        // 将 GuestData 保存到 NBT 中
        CompoundTag guestTag = new CompoundTag();
        this.guestData.save(guestTag);
        compound.put("GuestData", guestTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        // 从 NBT 加载 GuestData
        if (compound.contains("GuestData")) {
            CompoundTag guestTag = compound.getCompound("GuestData");
            this.guestData = GuestData.load(guestTag);
        }
    }
}
