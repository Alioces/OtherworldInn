package com.otherworldinn.world.inn;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.otherworldinn.world.team.TeamData;

/**
 * 旅社数据
 * <p>
 * 存储旅社的运营状态、旅客列表等信息。
 * </p>
 */
public class InnData {
    private String name = "My Inn";
    private boolean open = false; // 默认为歇业
    private boolean editMode = false; // 默认为非编辑模式
    private final Map<UUID, GuestData> guests = new HashMap<>();
    private final Map<Integer, RoomData> rooms = new HashMap<>();

    public InnData() {
    }

    // --- 属性访问 ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
        // 如果开启营业，自动关闭编辑模式
        if (open) {
            this.editMode = false;
        }
    }

    public boolean isEditMode() {
        return this.editMode;
    }

    /**
     * 尝试开启编辑模式
     * @return 如果成功开启返回 true，否则返回 false (例如正在营业或有客人)
     */
    public boolean tryEnableEditMode() {
        if (this.open) {
            return false; // 营业中不能编辑
        }
        if (!this.guests.isEmpty()) {
            return false; // 有客人不能编辑
        }
        this.editMode = true;
        return true;
    }

    public void disableEditMode() {
        this.editMode = false;
    }

    // --- 旅客管理 ---

    public Map<UUID, GuestData> getGuests() {
        return this.guests;
    }

    public void addGuest(GuestData guest) {
        this.guests.put(guest.getUuid(), guest);
        // 有客人时自动关闭编辑模式
        this.editMode = false;
    }

    public void removeGuest(UUID uuid) {
        guests.remove(uuid);
    }
    
    public GuestData getGuest(UUID uuid) {
        return guests.get(uuid);
    }

    // --- 房间管理 ---

    public Map<Integer, RoomData> getRooms() {
        return this.rooms;
    }

    public void addRoom(RoomData room) {
        this.rooms.put(room.getId(), room);
    }

    public void removeRoom(int roomId) {
        this.rooms.remove(roomId);
    }

    public RoomData getRoom(int roomId) {
        return this.rooms.get(roomId);
    }

    public RoomData getRoomAt(net.minecraft.core.BlockPos pos) {
        for (RoomData room : rooms.values()) {
            net.minecraft.core.BlockPos min = room.getMinPos();
            net.minecraft.core.BlockPos max = room.getMaxPos();
            if (pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
                pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
                pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ()) {
                return room;
            }
        }
        return null;
    }
    
    public void tick() {
        // 更新所有旅客状态
        guests.values().forEach(GuestData::tick);
        
        // 移除时间耗尽的旅客 (可选，或者仅标记)
        // guests.values().removeIf(g -> g.getRemainingTime() <= 0);
    }

    /**
     * 计算并更新房间属性
     * <p>
     * 遍历房间内的所有方块，查找已注册的家具，累加其属性值。
     * </p>
     *
     * @param roomId 房间ID
     * @param level  服务器等级 (用于获取方块状态)
     */
    public void calculateRoomStats(int roomId, net.minecraft.world.level.Level level) {
        RoomData room = rooms.get(roomId);
        if (room == null) {
            return;
        }

        // 使用局部变量累加，避免 lambda 表达式中无法修改局部变量的问题
        final int[] stats = new int[3]; // [0]: comfort, [1]: light, [2]: humidity

        net.minecraft.core.BlockPos min = room.getMinPos();
        net.minecraft.core.BlockPos max = room.getMaxPos();

        // 遍历房间区域
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(min, max)) {
            // 获取方块状态
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            // 获取家具属性
            FurnitureManager.getStats(state.getBlock()).ifPresent(s -> {
                stats[0] += s.comfort();
                stats[1] += s.light();
                stats[2] += s.humidity();
            });
        }

        room.setComfort(stats[0]);
        room.setLight(stats[1]);
        room.setHumidity(stats[2]);
    }

    /**
     * 更新所有房间的属性数据
     *
     * @param level 服务器等级
     */
    public void updateAllRoomsStats(net.minecraft.world.level.Level level) {
        for (Integer roomId : rooms.keySet()) {
            calculateRoomStats(roomId, level);
        }
    }

    /**
     * 检查所有房间的合法性
     * <p>
     * 遍历所有房间，如果不符合合法性规则，则将其删除。
     * </p>
     *
     * @param level 服务器等级
     * @param team  所属队伍 (用于范围检查)
     * @return 被删除的房间ID列表
     */
    public List<Integer> checkAllRoomsValidity(Level level, TeamData team) {
        List<Integer> removedRooms = new ArrayList<>();
        // 收集需要删除的房间ID，避免在遍历时修改集合
        for (RoomData room : rooms.values()) {
            if (!RoomData.isRoomValid(room.getMinPos(), room.getMaxPos(), level, team)) {
                removedRooms.add(room.getId());
            }
        }
        
        // 删除无效房间
        for (Integer roomId : removedRooms) {
            removeRoom(roomId);
        }
        
        return removedRooms;
    }

    // --- 入住/退房 ---

    /**
     * 旅客入住
     * <p>
     * 将旅客分配到指定房间。如果房间已满或不存在，返回 false。
     * 如果旅客已在其他房间，会自动先执行退房。
     * </p>
     *
     * @param guest  旅客数据
     * @param roomId 目标房间ID
     * @return 是否成功入住
     */
    public boolean checkIn(GuestData guest, int roomId) {
        RoomData room = rooms.get(roomId);
        if (room == null) {
            return false; // 房间不存在
        }

        // 检查房间是否已满
        if (room.getCurrentGuests().size() >= room.getMaxGuests()) {
            return false;
        }

        // 如果旅客已经在某个房间，先退房
        if (guest.getRoomId() != -1) {
            checkOut(guest.getUuid());
        }

        // 执行入住逻辑
        if (room.addGuest(guest.getUuid())) {
            guest.setRoomId(roomId);
            // 更新旅客列表，确保使用最新的 GuestData 实例
            this.guests.put(guest.getUuid(), guest);
            return true;
        }
        
        return false;
    }

    /**
     * 旅客退房
     * <p>
     * 将旅客从当前房间移除。
     * </p>
     *
     * @param guestId 旅客UUID
     */
    public void checkOut(UUID guestId) {
        GuestData guest = guests.get(guestId);
        if (guest != null) {
            int currentRoomId = guest.getRoomId();
            if (currentRoomId != -1) {
                RoomData room = rooms.get(currentRoomId);
                if (room != null) {
                    room.removeGuest(guestId);
                }
                guest.setRoomId(-1);
            }
        }
    }

    // --- NBT 序列化 ---

    public CompoundTag save(CompoundTag tag) {
        tag.putString("Name", name);
        tag.putBoolean("Open", open);
        tag.putBoolean("EditMode", editMode);

        ListTag guestsTag = new ListTag();
        for (GuestData guest : guests.values()) {
            guestsTag.add(guest.save(new CompoundTag()));
        }
        tag.put("Guests", guestsTag);

        ListTag roomsTag = new ListTag();
        for (RoomData room : rooms.values()) {
            roomsTag.add(room.save(new CompoundTag()));
        }
        tag.put("Rooms", roomsTag);
        
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("Name")) {
            name = tag.getString("Name");
        }
        if (tag.contains("Open")) {
            open = tag.getBoolean("Open");
        }
        if (tag.contains("EditMode")) {
            editMode = tag.getBoolean("EditMode");
        }

        guests.clear();
        if (tag.contains("Guests")) {
            ListTag guestsTag = tag.getList("Guests", Tag.TAG_COMPOUND);
            for (Tag t : guestsTag) {
                if (t instanceof CompoundTag guestTag) {
                    GuestData guest = GuestData.load(guestTag);
                    guests.put(guest.getUuid(), guest);
                }
            }
        }

        rooms.clear();
        if (tag.contains("Rooms")) {
            ListTag roomsTag = tag.getList("Rooms", Tag.TAG_COMPOUND);
            for (Tag t : roomsTag) {
                if (t instanceof CompoundTag roomTag) {
                    RoomData room = RoomData.load(roomTag);
                    rooms.put(room.getId(), room);
                }
            }
        }
    }
}
