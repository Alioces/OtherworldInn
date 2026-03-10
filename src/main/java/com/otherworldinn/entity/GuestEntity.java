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

    private GuestData guestData;

    protected GuestEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // 初始化旅客数据，默认居住时间为 1 Minecraft 天 (24000 ticks)
        this.guestData = new GuestData(this.getUUID(), 24000L);
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
        // 使用子标签避免键冲突
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

    public GuestData getGuestData() {
        return guestData;
    }
}
