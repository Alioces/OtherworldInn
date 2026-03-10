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
        return tag;
    }

    public static GuestData load(CompoundTag tag) {
        UUID uuid = tag.getUUID("UUID");
        long remainingTime = tag.getLong("RemainingTime");
        GuestData guest = new GuestData(uuid, remainingTime);
        if (tag.contains("RoomId")) {
            guest.setRoomId(tag.getInt("RoomId"));
        }
        return guest;
    }
}
