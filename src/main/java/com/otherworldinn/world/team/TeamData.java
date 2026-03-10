package com.otherworldinn.world.team;

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
    }
}
