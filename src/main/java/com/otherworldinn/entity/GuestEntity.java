package com.otherworldinn.entity;

import com.otherworldinn.world.inn.GuestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
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
    private GuestData guestData;

    protected GuestEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // 初始化旅客数据，默认退房时间为当前时间 + 1 Minecraft 天 (24000 ticks)
        long currentTime = level.getGameTime();
        this.guestData = new GuestData(this.getUUID(), currentTime + 24000L);
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

    /**
     * 获取旅客数据
     *
     * @return 旅客数据对象
     */
    public GuestData getGuestData() {
        return guestData;
    }
}
