package com.otherworldinn.world.inn;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.otherworldinn.world.team.TeamData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import com.otherworldinn.entity.GuestEntity;

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
    private final Set<UUID> guestIds = new HashSet<>();
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
        if (!this.guestIds.isEmpty()) {
            return false; // 有客人不能编辑
        }
        this.editMode = true;
        return true;
    }

    public void disableEditMode() {
        this.editMode = false;
    }

    // --- 旅客管理 ---

    public Set<UUID> getGuestIds() {
        return this.guestIds;
    }

    public void addGuest(UUID guestId) {
        this.guestIds.add(guestId);
        // 有客人时自动关闭编辑模式
        this.editMode = false;
    }

    public void removeGuest(UUID uuid) {
        guestIds.remove(uuid);
    }
    
    /**
     * 获取旅客数据
     * <p>
     * 通过 UUID 在服务器等级中查找实体并获取数据。
     * </p>
     * 
     * @param uuid 旅客 UUID
     * @param level 服务器等级
     * @return 旅客数据，如果找不到实体则返回 null
     */
    public GuestData getGuestData(UUID uuid, ServerLevel level) {
        if (!guestIds.contains(uuid)) {
            return null;
        }
        Entity entity = level.getEntity(uuid);
        if (entity instanceof GuestEntity guestEntity) {
            return guestEntity.getGuestData();
        }
        return null;
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
    
    public void tick(ServerLevel level) {
        // 更新所有旅客状态
        // 实际上旅客实体自己会 tick，这里如果需要做全局管理（例如统计）可以在此遍历
        // 如果要遍历，需要从 world 获取实体
        
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
     * @param guestId 旅客 UUID
     * @param roomId  目标房间ID
     * @param level   服务器等级
     * @return 是否成功入住
     */
    public boolean checkIn(UUID guestId, int roomId, ServerLevel level) {
        RoomData room = rooms.get(roomId);
        if (room == null) {
            return false; // 房间不存在
        }

        // 检查房间是否已满
        if (room.getCurrentGuests().size() >= room.getMaxGuests()) {
            return false;
        }

        GuestData guest = getGuestData(guestId, level);
        if (guest == null) {
            return false; // 找不到旅客实体
        }

        // 如果旅客已经在某个房间，先退房
        if (guest.getRoomId() != -1) {
            checkOut(guestId, level);
        }

        // 执行入住逻辑
        if (room.addGuest(guestId)) {
            guest.setRoomId(roomId);
            this.addGuest(guestId);
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
     * @param level   服务器等级
     */
    public void checkOut(UUID guestId, ServerLevel level) {
        GuestData guest = getGuestData(guestId, level);
        if (guest != null) {
            int currentRoomId = guest.getRoomId();
            if (currentRoomId != -1) {
                RoomData room = rooms.get(currentRoomId);
                if (room != null) {
                    room.removeGuest(guestId);
                }
                guest.setRoomId(-1);
            }
        } else {
            // 如果找不到实体（可能未加载），尝试从房间记录中清理
            // 这是一种防御性编程，防止僵尸数据
            for (RoomData room : rooms.values()) {
                if (room.hasGuest(guestId)) {
                    room.removeGuest(guestId);
                    break;
                }
            }
        }
        // 从活跃旅客列表中移除
        // 注意：这里是否移除取决于业务逻辑。如果是退房离开旅社，则移除。
        // 如果只是换房，则不应在此移除，而是在 checkIn 中处理。
        // 但根据方法名 checkOut，通常意味着离开房间。
        // 为了安全起见，仅在完全离开旅社时调用 removeGuest(uuid)。
        // 这里的 checkOut 更像是 "checkOutOfRoom"。
    }

    // --- NBT 序列化 ---

    public CompoundTag save(CompoundTag tag) {
        tag.putString("Name", name);
        tag.putBoolean("Open", open);
        tag.putBoolean("EditMode", editMode);

        ListTag guestsTag = new ListTag();
        for (UUID guestId : guestIds) {
            CompoundTag guestTag = new CompoundTag();
            guestTag.putUUID("UUID", guestId);
            guestsTag.add(guestTag);
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

        guestIds.clear();
        if (tag.contains("Guests")) {
            ListTag guestsTag = tag.getList("Guests", Tag.TAG_COMPOUND);
            for (Tag t : guestsTag) {
                if (t instanceof CompoundTag guestTag) {
                    guestIds.add(guestTag.getUUID("UUID"));
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
