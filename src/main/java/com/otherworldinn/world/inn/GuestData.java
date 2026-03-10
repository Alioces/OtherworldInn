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
    private long remainingTime; // 剩余居住时间 (ticks)
    private int roomId = -1; // 居住的房间ID (-1 表示无房间)

    // 房间属性偏好 (区间)
    private IntRange comfortPreference = new IntRange(0, 100);
    private IntRange lightPreference = new IntRange(0, 100);
    private IntRange humidityPreference = new IntRange(0, 100);

    public GuestData(UUID uuid, long remainingTime) {
        this.uuid = uuid;
        this.remainingTime = remainingTime;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(long remainingTime) {
        this.remainingTime = remainingTime;
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

    public void tick() {
        if (this.remainingTime > 0) {
            this.remainingTime--;
        }
    }

    // --- NBT 序列化 ---

    public CompoundTag save(CompoundTag tag) {
        tag.putUUID("UUID", uuid);
        tag.putLong("RemainingTime", remainingTime);
        tag.putInt("RoomId", roomId);
        
        tag.put("ComfortPref", comfortPreference.save());
        tag.put("LightPref", lightPreference.save());
        tag.put("HumidityPref", humidityPreference.save());
        
        return tag;
    }

    public static GuestData load(CompoundTag tag) {
        UUID uuid = tag.getUUID("UUID");
        long remainingTime = tag.getLong("RemainingTime");
        GuestData guest = new GuestData(uuid, remainingTime);
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
     */
    public record IntRange(int min, int max) {
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
