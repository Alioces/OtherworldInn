package com.otherworldinn.world.inn;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * 旅客数据
 * <p>
 * 存储旅社中单个旅客的信息。
 * </p>
 */
public class GuestData {
    private final UUID uuid;
    private long checkoutTime; // 预计退房时间 (GameTime)
    private int roomId = -1; // 居住的房间ID (-1 表示无房间)

    // 房间属性偏好 (区间)
    private IntRange comfortPreference = new IntRange(0, 100);
    private IntRange lightPreference = new IntRange(0, 100);
    private IntRange humidityPreference = new IntRange(0, 100);

    /**
     * 构造一个新的旅客数据
     *
     * @param uuid         旅客UUID
     * @param checkoutTime 预计退房时间 (GameTime)
     */
    public GuestData(UUID uuid, long checkoutTime) {
        this.uuid = uuid;
        this.checkoutTime = checkoutTime;
    }

    public UUID getUuid() {
        return uuid;
    }

    /**
     * 获取预计退房时间
     *
     * @return 退房时间 (GameTime)
     */
    public long getCheckoutTime() {
        return checkoutTime;
    }

    /**
     * 设置预计退房时间
     *
     * @param checkoutTime 退房时间 (GameTime)
     */
    public void setCheckoutTime(long checkoutTime) {
        this.checkoutTime = checkoutTime;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    // --- 偏好设置 ---

    public IntRange getComfortPreference() {
        return comfortPreference;
    }

    public void setComfortPreference(int min, int max) {
        this.comfortPreference = new IntRange(min, max);
    }

    public IntRange getLightPreference() {
        return lightPreference;
    }

    public void setLightPreference(int min, int max) {
        this.lightPreference = new IntRange(min, max);
    }

    public IntRange getHumidityPreference() {
        return humidityPreference;
    }

    public void setHumidityPreference(int min, int max) {
        this.humidityPreference = new IntRange(min, max);
    }

    /**
     * 每 tick 更新逻辑
     * <p>
     * 可以在此检查是否到达退房时间。
     * 目前留空，暂不实现自动退房逻辑。
     * </p>
     */
    public void tick() {
        // 退房逻辑暂不实现
    }

    // --- NBT 序列化 ---

    /**
     * 保存数据到 NBT
     *
     * @param tag 目标标签
     * @return 写入数据的标签
     */
    public CompoundTag save(CompoundTag tag) {
        tag.putUUID("UUID", uuid);
        tag.putLong("CheckoutTime", checkoutTime);
        tag.putInt("RoomId", roomId);
        
        tag.put("ComfortPref", comfortPreference.save());
        tag.put("LightPref", lightPreference.save());
        tag.put("HumidityPref", humidityPreference.save());
        
        return tag;
    }

    /**
     * 从 NBT 加载数据
     *
     * @param tag 源标签
     * @return 加载的旅客数据
     */
    public static GuestData load(CompoundTag tag) {
        UUID uuid = tag.getUUID("UUID");
        long checkoutTime = tag.getLong("CheckoutTime");
        
        GuestData guest = new GuestData(uuid, checkoutTime);
        if (tag.contains("RoomId")) {
            guest.setRoomId(tag.getInt("RoomId"));
        }
        
        if (tag.contains("ComfortPref")) {
            guest.comfortPreference = IntRange.load(tag.getCompound("ComfortPref"));
        }
        if (tag.contains("LightPref")) {
            guest.lightPreference = IntRange.load(tag.getCompound("LightPref"));
        }
        if (tag.contains("HumidityPref")) {
            guest.humidityPreference = IntRange.load(tag.getCompound("HumidityPref"));
        }
        
        return guest;
    }
    
    /**
     * 整数区间记录类
     * <p>
     * 用于存储属性偏好范围 (min, max)。
     * </p>
     */
    public record IntRange(int min, int max) {
        /**
         * 检查值是否在区间内
         *
         * @param value 待检查值
         * @return 是否包含
         */
        public boolean contains(int value) {
            return value >= min && value <= max;
        }
        
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Min", min);
            tag.putInt("Max", max);
            return tag;
        }
        
        public static IntRange load(CompoundTag tag) {
            return new IntRange(tag.getInt("Min"), tag.getInt("Max"));
        }
    }
}
