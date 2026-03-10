package com.otherworldinn.world.team;

import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.map.MapPoint;
import com.otherworldinn.world.map.TownDataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 队伍数据
 * <p>
 * 存储队伍的成员、解锁状态（地图点、功能）等信息。
 * </p>
 */
public class TeamData {
    private final UUID teamId;
    private String name;
    private UUID leaderId;
    private final Set<UUID> members = new HashSet<>();
    private final Set<ResourceLocation> unlockedMapPoints = new HashSet<>();
    private boolean teleportUnlocked = false;
    private int coins = 0; // 队伍金币
    private net.minecraft.core.BlockPos innZoneCenter = new net.minecraft.core.BlockPos(0, 70, 0); // 旅社中心
    private int innZoneRadius = 15; // 旅社半径
    private final InnData innData = new InnData(); // 旅社数据管理系统

    public TeamData(UUID teamId) {
        this.teamId = teamId;
        this.name = "Team-" + teamId.toString().substring(0, 8);
    }

    public UUID getTeamId() {
        return teamId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public UUID getLeaderId() {
        return leaderId;
    }
    
    public void setLeaderId(UUID leaderId) {
        // 服务端逻辑检查成员存在性，客户端同步时直接设置
        this.leaderId = leaderId;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID playerId) {
        members.add(playerId);
        if (leaderId == null) {
            leaderId = playerId;
        }
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
        if (playerId.equals(leaderId) && !members.isEmpty()) {
            // 如果移除的是队长，且队伍还有人，则转移给第一顺位
            leaderId = members.iterator().next();
        } else if (members.isEmpty()) {
            leaderId = null;
        }
    }

    public boolean hasMember(UUID playerId) {
        return members.contains(playerId);
    }

    // --- 解锁状态 ---

    public boolean isMapPointUnlocked(ResourceLocation pointId) {
        // 1. 如果在已解锁列表中，则解锁
        if (unlockedMapPoints.contains(pointId)) {
            return true;
        }
        
        // 2. 检查默认解锁条件 (null condition)
        Optional<MapPoint> pointOpt = TownDataProvider.getPoint(pointId);
        if (pointOpt.isPresent()) {
            MapPoint point = pointOpt.get();
            if (point.unlockCondition() == null) {
                return true;
            }
        }
        
        // 3. 硬编码的初始点 (作为后备)
        return pointId.getPath().equals("inn");
    }

    public void unlockMapPoint(ResourceLocation pointId) {
        unlockedMapPoints.add(pointId);
    }

    public boolean isTeleportUnlocked() {
        return teleportUnlocked;
    }

    public void setTeleportUnlocked(boolean unlocked) {
        this.teleportUnlocked = unlocked;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = Math.max(0, coins);
    }

    public void addCoins(int amount) {
        if (amount > 0) {
            this.coins += amount;
        }
    }

    public boolean removeCoins(int amount) {
        if (amount > 0 && this.coins >= amount) {
            this.coins -= amount;
            return true;
        }
        return false;
    }

    public Set<ResourceLocation> getUnlockedMapPoints() {
        return unlockedMapPoints;
    }

    public void setUnlockedMapPoints(Set<ResourceLocation> points) {
        this.unlockedMapPoints.clear();
        this.unlockedMapPoints.addAll(points);
    }
    
    public void setMembers(Set<UUID> newMembers) {
        this.members.clear();
        this.members.addAll(newMembers);
    }

    public net.minecraft.core.BlockPos getInnZoneCenter() {
        return innZoneCenter;
    }

    public void setInnZoneCenter(net.minecraft.core.BlockPos center) {
        this.innZoneCenter = center;
    }

    public int getInnZoneRadius() {
        return innZoneRadius;
    }

    public void setInnZoneRadius(int radius) {
        this.innZoneRadius = radius;
    }

    public InnData getInnData() {
        return innData;
    }

    // --- NBT 序列化 ---

    /**
     * 将队伍数据保存到 NBT
     *
     * @param tag 目标 NBT 标签
     * @return 包含数据的 NBT 标签
     */
    public CompoundTag save(CompoundTag tag) {
        tag.putUUID("TeamId", teamId);
        if (name != null) {
            tag.putString("Name", name);
        }
        if (leaderId != null) {
            tag.putUUID("LeaderId", leaderId);
        }
        
        ListTag membersTag = new ListTag();
        for (UUID member : members) {
            CompoundTag memberTag = new CompoundTag();
            memberTag.putUUID("UUID", member);
            membersTag.add(memberTag);
        }
        tag.put("Members", membersTag);

        ListTag pointsTag = new ListTag();
        for (ResourceLocation point : unlockedMapPoints) {
            pointsTag.add(StringTag.valueOf(point.toString()));
        }
        tag.put("UnlockedPoints", pointsTag);

        tag.putBoolean("TeleportUnlocked", teleportUnlocked);
        tag.putInt("Coins", coins);

        // 旅社区域
        if (innZoneCenter != null) {
            tag.putLong("InnCenter", innZoneCenter.asLong());
        }
        tag.putInt("InnRadius", innZoneRadius);
        
        // 旅社数据
        tag.put("InnData", innData.save(new CompoundTag()));

        return tag;
    }

    /**
     * 从 NBT 加载队伍数据
     *
     * @param tag 源 NBT 标签
     */
    public void load(CompoundTag tag) {
        if (tag.contains("Name")) {
            name = tag.getString("Name");
        }
        if (tag.contains("LeaderId")) {
            leaderId = tag.getUUID("LeaderId");
        }
        
        members.clear();
        ListTag membersTag = tag.getList("Members", Tag.TAG_COMPOUND);
        for (Tag t : membersTag) {
            CompoundTag memberTag = (CompoundTag) t;
            members.add(memberTag.getUUID("UUID"));
        }

        unlockedMapPoints.clear();
        ListTag pointsTag = tag.getList("UnlockedPoints", Tag.TAG_STRING);
        for (Tag t : pointsTag) {
            unlockedMapPoints.add(ResourceLocation.parse(t.getAsString()));
        }

        teleportUnlocked = tag.getBoolean("TeleportUnlocked");
        if (tag.contains("Coins")) {
            coins = tag.getInt("Coins");
        } else {
            coins = 0;
        }
        
        if (tag.contains("InnCenter")) {
            innZoneCenter = net.minecraft.core.BlockPos.of(tag.getLong("InnCenter"));
        } else {
            // 默认值
            innZoneCenter = new net.minecraft.core.BlockPos(0, 70, 0);
        }
        
        if (tag.contains("InnRadius")) {
            innZoneRadius = tag.getInt("InnRadius");
        } else {
            innZoneRadius = 15;
        }
        
        if (tag.contains("InnData")) {
            innData.load(tag.getCompound("InnData"));
        }
    }
}
