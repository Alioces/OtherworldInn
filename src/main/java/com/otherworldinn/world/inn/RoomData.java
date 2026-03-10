package com.otherworldinn.world.inn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.otherworldinn.world.team.TeamData;

/**
 * 房间数据
 * <p>
 * 存储旅社中单个房间的信息，包括空间范围、编号和属性。
 * </p>
 */
public class RoomData {
    private final int id;
    private final BlockPos minPos;
    private final BlockPos maxPos;
    
    // 房间属性 (0-100)
    private int comfort;
    private int light;
    private int humidity;

    // 旅客信息
    private int maxGuests = 1; // 默认最大可居住1人
    private final Set<UUID> currentGuests = new HashSet<>();

    /**
     * 创建一个新的房间数据
     *
     * @param id     房间编号
     * @param minPos 最小坐标
     * @param maxPos 最大坐标
     */
    public RoomData(int id, BlockPos minPos, BlockPos maxPos) {
        this.id = id;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.comfort = 0;
        this.light = 0;
        this.humidity = 0;
    }

    public int getId() {
        return id;
    }

    public BlockPos getMinPos() {
        return minPos;
    }

    public BlockPos getMaxPos() {
        return maxPos;
    }

    public int getComfort() {
        return comfort;
    }

    public void setComfort(int comfort) {
        this.comfort = Math.max(0, Math.min(100, comfort));
    }

    public int getLight() {
        return light;
    }

    public void setLight(int light) {
        this.light = Math.max(0, Math.min(100, light));
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = Math.max(0, Math.min(100, humidity));
    }

    // --- 旅客管理 ---

    public int getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(int maxGuests) {
        this.maxGuests = Math.max(0, maxGuests);
    }

    public Set<UUID> getCurrentGuests() {
        return currentGuests;
    }

    public boolean addGuest(UUID guestId) {
        if (currentGuests.size() < maxGuests) {
            return currentGuests.add(guestId);
        }
        return false;
    }

    public void removeGuest(UUID guestId) {
        currentGuests.remove(guestId);
    }

    public boolean hasGuest(UUID guestId) {
        return currentGuests.contains(guestId);
    }

    // --- 静态验证方法 ---
    /**
     * 判定区域是否能作为房间
     * <p>
     * 检查逻辑：
     * 1. 房间的两个角坐标必须在旅社区域内。
     * 2. 底面（最低点再低一格）所有方块必须有完整的、可站立的上表面。
     * 3. 顶面（最高点再高一格）所有方块不能为无碰撞体积的方块。
     * 4. 侧面（四个侧面）外层方块中，至少 3/4 的方块必须有碰撞体积。
     * 5. 所有侧面的最外层方块中，至少包含一扇门。
     * </p>
     * 
     * @param minPos 房间最小坐标
     * @param maxPos 房间最大坐标
     * @param level 世界实例
     * @param team 所属队伍（用于检查区域范围）
     */
    public static boolean isRoomValid(BlockPos minPos, BlockPos maxPos, Level level, TeamData team) {
        // 0. 检查是否在旅社区域内
        if (team != null) {
            BlockPos center = team.getInnZoneCenter();
            int radius = team.getInnZoneRadius();
            
            // 检查 minPos 和 maxPos 是否都在半径范围内
            // 简单盒式判定: |x - cx| <= r && |z - cz| <= r
            // Y轴通常不做严格限制，或者也可以限制
            if (!isPosInZone(minPos, center, radius) || !isPosInZone(maxPos, center, radius)) {
                return false;
            }
        }

        int minX = minPos.getX();
        int minY = minPos.getY();
        int minZ = minPos.getZ();
        int maxX = maxPos.getX();
        int maxY = maxPos.getY();
        int maxZ = maxPos.getZ();

        // 1. 检查底面 (minY - 1)
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos floorPos = new BlockPos(x, minY - 1, z);
                BlockState state = level.getBlockState(floorPos);
                // 检查是否有完整上表面支持站立 (isFaceSturdy with UP)
                if (!state.isFaceSturdy(level, floorPos, Direction.UP)) {
                    return false;
                }
            }
        }

        // 2. 检查顶面 (maxY + 1)
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos ceilingPos = new BlockPos(x, maxY + 1, z);
                BlockState state = level.getBlockState(ceilingPos);
                // 检查是否有碰撞体积 (getCollisionShape not empty)
                if (state.getCollisionShape(level, ceilingPos).isEmpty()) {
                    return false;
                }
            }
        }

        // 3. 检查侧面
        int totalWallBlocks = 0;
        int solidWallBlocks = 0;
        boolean hasDoor = false;

        // 遍历所有可能的墙壁坐标
        // 墙壁位于 minX-1, maxX+1, minZ-1, maxZ+1 所在的平面 (即房间标记区域的外围一圈)，高度范围 [minY, maxY]
        
        // Z轴固定的墙面 (minZ-1 和 maxZ+1)
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                // 北面 (minZ - 1)
                BlockPos northPos = new BlockPos(x, y, minZ - 1);
                if (checkWallBlock(level, northPos)) solidWallBlocks++;
                if (isDoor(level, northPos)) hasDoor = true;
                totalWallBlocks++;

                // 南面 (maxZ + 1)
                BlockPos southPos = new BlockPos(x, y, maxZ + 1);
                if (checkWallBlock(level, southPos)) solidWallBlocks++;
                if (isDoor(level, southPos)) hasDoor = true;
                totalWallBlocks++;
            }
        }

        // X轴固定的墙面 (minX-1 和 maxX+1)
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                // 西面 (minX - 1)
                BlockPos westPos = new BlockPos(minX - 1, y, z);
                if (checkWallBlock(level, westPos)) solidWallBlocks++;
                if (isDoor(level, westPos)) hasDoor = true;
                totalWallBlocks++;

                // 东面 (maxX + 1)
                BlockPos eastPos = new BlockPos(maxX + 1, y, z);
                if (checkWallBlock(level, eastPos)) solidWallBlocks++;
                if (isDoor(level, eastPos)) hasDoor = true;
                totalWallBlocks++;
            }
        }

        // 3/4 的方块必须有碰撞体积
        if ((double) solidWallBlocks / totalWallBlocks < 0.75) {
            return false;
        }

        // 必须包含至少一扇门
        return hasDoor;
    }

    private static boolean isPosInZone(BlockPos pos, BlockPos center, int radius) {
        return Math.abs(pos.getX() - center.getX()) <= radius &&
               Math.abs(pos.getZ() - center.getZ()) <= radius;
    }

    private static boolean checkWallBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // 有碰撞体积 或 是门 (门通常有碰撞体积，但打开时可能变化，这里视为有效墙体的一部分)
        return !state.getCollisionShape(level, pos).isEmpty() || isDoor(level, pos);
    }

    private static boolean isDoor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof DoorBlock || state.is(BlockTags.DOORS);
    }

    // --- NBT 序列化 ---

    /**
     * 将房间数据保存到 NBT
     *
     * @param tag 目标 NBT 标签
     * @return 包含数据的 NBT 标签
     */
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Id", id);
        tag.putLong("MinPos", minPos.asLong());
        tag.putLong("MaxPos", maxPos.asLong());
        tag.putInt("Comfort", comfort);
        tag.putInt("Light", light);
        tag.putInt("Humidity", humidity);
        
        tag.putInt("MaxGuests", maxGuests);
        ListTag guestsTag = new ListTag();
        for (UUID uuid : currentGuests) {
            CompoundTag guestTag = new CompoundTag();
            guestTag.putUUID("UUID", uuid);
            guestsTag.add(guestTag);
        }
        tag.put("CurrentGuests", guestsTag);
        
        return tag;
    }

    /**
     * 从 NBT 加载房间数据
     *
     * @param tag 源 NBT 标签
     * @return 加载的房间数据
     */
    public static RoomData load(CompoundTag tag) {
        int id = tag.getInt("Id");
        BlockPos minPos = BlockPos.of(tag.getLong("MinPos"));
        BlockPos maxPos = BlockPos.of(tag.getLong("MaxPos"));
        
        RoomData room = new RoomData(id, minPos, maxPos);
        
        if (tag.contains("Comfort")) {
            room.setComfort(tag.getInt("Comfort"));
        }
        if (tag.contains("Light")) {
            room.setLight(tag.getInt("Light"));
        }
        if (tag.contains("Humidity")) {
            room.setHumidity(tag.getInt("Humidity"));
        }

        if (tag.contains("MaxGuests")) {
            room.setMaxGuests(tag.getInt("MaxGuests"));
        }
        
        if (tag.contains("CurrentGuests")) {
            ListTag guestsTag = tag.getList("CurrentGuests", Tag.TAG_COMPOUND);
            for (Tag t : guestsTag) {
                if (t instanceof CompoundTag guestTag) {
                    room.currentGuests.add(guestTag.getUUID("UUID"));
                }
            }
        }
        
        return room;
    }
}
