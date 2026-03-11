package com.otherworldinn.world.team;

import lombok.Data;
import lombok.Setter;
import lombok.AccessLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.map.MapPoint;
import com.otherworldinn.world.map.TownDataProvider;

/**
 * 队伍数据
 * <p>
 * 存储队伍的成员、解锁状态（地图点、功能）等信息。
 * </p>
 */
@Data
public class TeamData {
    
    public record InnRegion(int minX, int minZ, int maxX, int maxZ) {
        public boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        public boolean contains(InnRegion other) {
            return this.minX <= other.minX && this.maxX >= other.maxX &&
                   this.minZ <= other.minZ && this.maxZ >= other.maxZ;
        }

        public boolean intersects(InnRegion other) {
            return this.minX <= other.maxX && this.maxX >= other.minX &&
                   this.minZ <= other.maxZ && this.maxZ >= other.minZ;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("MinX", minX);
            tag.putInt("MinZ", minZ);
            tag.putInt("MaxX", maxX);
            tag.putInt("MaxZ", maxZ);
            return tag;
        }

        public static InnRegion load(CompoundTag tag) {
            return new InnRegion(
                tag.getInt("MinX"),
                tag.getInt("MinZ"),
                tag.getInt("MaxX"),
                tag.getInt("MaxZ")
            );
        }
    }

    private final UUID teamId;
    private String name;
    private UUID leaderId;
    private final Set<UUID> members = new HashSet<>();
    
    @Setter(AccessLevel.NONE)
    private final Set<ResourceLocation> unlockedMapPoints = new HashSet<>();
    
    private boolean teleportUnlocked = false;
    
    @Setter(AccessLevel.NONE)
    private int coins = 0; // 队伍金币
    
    // 旅社区域列表 (替代原有的中心点+半径)
    private final List<InnRegion> innRegions = new ArrayList<>();
    
    // 过时字段，仅用于数据迁移或特定逻辑
    @Deprecated
    @Setter(AccessLevel.NONE)
    private BlockPos innZoneCenter = new BlockPos(0, 70, 0); 
    @Deprecated
    @Setter(AccessLevel.NONE)
    private int innZoneRadius = 15; 
    
    private final InnData innData = new InnData(); // 旅社数据管理系统

    public TeamData(UUID teamId) {
        this.teamId = teamId;
        this.name = "Team-" + teamId.toString().substring(0, 8);
        // 初始化默认区域 (以原点为中心，半径15)
        // -15 ~ 15 -> 31x31
        addRegion(new InnRegion(-15, -15, 15, 15));
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

    public void lockMapPoint(ResourceLocation pointId) {
        unlockedMapPoints.remove(pointId);
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

    public void setUnlockedMapPoints(Set<ResourceLocation> points) {
        this.unlockedMapPoints.clear();
        this.unlockedMapPoints.addAll(points);
    }
    
    public void setMembers(Set<UUID> newMembers) {
        this.members.clear();
        this.members.addAll(newMembers);
    }

    // 兼容性方法：返回第一个区域的中心，或者默认中心
    // 主要用于某些仍需要单一中心点的逻辑（如地图渲染定位）
    public BlockPos getEffectiveCenter() {
        if (!innRegions.isEmpty()) {
            InnRegion r = innRegions.get(0);
            return new BlockPos((r.minX + r.maxX) / 2, 70, (r.minZ + r.maxZ) / 2);
        }
        return innZoneCenter;
    }
    
    // 兼容性方法：尽可能返回一个能覆盖所有区域的半径
    public int getEffectiveRadius() {
        if (innRegions.isEmpty()) return innZoneRadius;
        
        BlockPos center = getEffectiveCenter();
        int maxDist = 0;
        
        for (InnRegion region : innRegions) {
            int d1 = Math.max(Math.abs(region.minX - center.getX()), Math.abs(region.minZ - center.getZ()));
            int d2 = Math.max(Math.abs(region.maxX - center.getX()), Math.abs(region.maxZ - center.getZ()));
            maxDist = Math.max(maxDist, Math.max(d1, d2));
        }
        
        return maxDist;
    }

    public void addRegion(InnRegion region) {
        // 确保 min <= max
        int minX = Math.min(region.minX, region.maxX);
        int maxX = Math.max(region.minX, region.maxX);
        int minZ = Math.min(region.minZ, region.maxZ);
        int maxZ = Math.max(region.minZ, region.maxZ);
        
        InnRegion normalized = new InnRegion(minX, minZ, maxX, maxZ);
        
        List<InnRegion> toAdd = new ArrayList<>();
        toAdd.add(normalized);
        
        // 用现有的所有区域去切割新区域，确保存储的区域互不重叠
        // 这样做的好处是：
        // 1. 避免重叠区域在渲染时出现颜色叠加加深的问题
        // 2. 保持数据结构的整洁性
        for (InnRegion existing : innRegions) {
            List<InnRegion> nextPass = new ArrayList<>();
            for (InnRegion candidate : toAdd) {
                nextPass.addAll(subtract(candidate, existing));
            }
            toAdd = nextPass;
            if (toAdd.isEmpty()) break;
        }
        
        if (!toAdd.isEmpty()) {
            innRegions.addAll(toAdd);
            optimizeRegions();
        }
    }
    
    /**
     * 计算区域差集 (A - B)
     * 返回一组互不重叠的矩形，其并集等于 (A - B)
     */
    private List<InnRegion> subtract(InnRegion a, InnRegion b) {
        List<InnRegion> result = new ArrayList<>();
        
        // 如果不相交，直接返回 A
        if (!a.intersects(b)) {
            result.add(a);
            return result;
        }
        
        // 如果 A 被 B 完全包含，返回空
        if (b.contains(a)) {
            return result;
        }
        
        // 如果有重叠，我们需要将 A 切割
        // 切割策略：上下左右四个方向
        
        int ax1 = a.minX, ax2 = a.maxX, az1 = a.minZ, az2 = a.maxZ;
        int bx1 = b.minX, bx2 = b.maxX, bz1 = b.minZ, bz2 = b.maxZ;
        
        // 1. Top (Z < bz1)
        if (az1 < bz1) {
            result.add(new InnRegion(ax1, az1, ax2, bz1 - 1));
            // 剩下的部分继续处理 (更新 az1)
            az1 = bz1;
        }
        
        // 2. Bottom (Z > bz2)
        if (az2 > bz2) {
            result.add(new InnRegion(ax1, bz2 + 1, ax2, az2));
            // 剩下的部分继续处理 (更新 az2)
            az2 = bz2;
        }
        
        // 现在 Z 范围已经被限制在 B 的 Z 范围内 (或 A 原本的 Z 范围内)
        // 处理 X 方向
        
        // 3. Left (X < bx1)
        if (ax1 < bx1) {
            result.add(new InnRegion(ax1, az1, bx1 - 1, az2));
        }
        
        // 4. Right (X > bx2)
        if (ax2 > bx2) {
            result.add(new InnRegion(bx2 + 1, az1, ax2, az2));
        }
        
        return result;
    }
    
    public void removeRegion(InnRegion region) {
        // 简单实现：移除完全匹配的，或者重叠部分剔除（复杂）
        // 目前需求主要是添加和合并。如果需要移除，通常是“移除某个区域内的权限”。
        // 这里暂时仅支持移除完全一致的区域
        innRegions.remove(region);
    }

    /**
     * 优化区域列表，合并可合并的矩形
     */
    private void optimizeRegions() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < innRegions.size(); i++) {
                for (int j = i + 1; j < innRegions.size(); j++) {
                    InnRegion r1 = innRegions.get(i);
                    InnRegion r2 = innRegions.get(j);
                    InnRegion merged = tryMerge(r1, r2);
                    if (merged != null) {
                        innRegions.remove(j); // 先移除后面的索引
                        innRegions.remove(i);
                        innRegions.add(merged);
                        changed = true;
                        break; 
                    }
                }
                if (changed) break;
            }
        }
    }

    private InnRegion tryMerge(InnRegion r1, InnRegion r2) {
        // 包含关系
        if (r1.contains(r2)) return r1;
        if (r2.contains(r1)) return r2;
        
        // 水平拼接 (Z 范围相同，X 相邻或重叠)
        if (r1.minZ == r2.minZ && r1.maxZ == r2.maxZ) {
            // 检查 X 是否连续或重叠
            // 连续条件: r1.minX <= r2.maxX + 1 && r2.minX <= r1.maxX + 1
            if (r1.minX <= r2.maxX + 1 && r2.minX <= r1.maxX + 1) {
                return new InnRegion(Math.min(r1.minX, r2.minX), r1.minZ, Math.max(r1.maxX, r2.maxX), r1.maxZ);
            }
        }
        
        // 垂直拼接 (X 范围相同，Z 相邻或重叠)
        if (r1.minX == r2.minX && r1.maxX == r2.maxX) {
            // 检查 Z 是否连续或重叠
            if (r1.minZ <= r2.maxZ + 1 && r2.minZ <= r1.maxZ + 1) {
                return new InnRegion(r1.minX, Math.min(r1.minZ, r2.minZ), r1.maxX, Math.max(r1.maxZ, r2.maxZ));
            }
        }
        
        return null;
    }

    /**
     * 检查坐标是否在旅社区域内
     */
    public boolean isInInnZone(BlockPos pos) {
        for (InnRegion region : innRegions) {
            if (region.contains(pos.getX(), pos.getZ())) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public void setInnZoneCenter(BlockPos center) {
        this.innZoneCenter = center;
        // 迁移逻辑：如果设置中心点，则重置区域为以该点为中心的矩形
        innRegions.clear();
        addRegion(new InnRegion(center.getX() - innZoneRadius, center.getZ() - innZoneRadius, 
                                center.getX() + innZoneRadius, center.getZ() + innZoneRadius));
    }

    @Deprecated
    public void setInnZoneRadius(int radius) {
        this.innZoneRadius = radius;
        // 迁移逻辑
        if (innZoneCenter != null) {
            innRegions.clear();
            addRegion(new InnRegion(innZoneCenter.getX() - radius, innZoneCenter.getZ() - radius, 
                                    innZoneCenter.getX() + radius, innZoneCenter.getZ() + radius));
        }
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
        tag.putString("Name", name != null ? name : "");
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
        
        // 旅社数据 (包含 EditMode)
        tag.put("InnData", innData.save(new CompoundTag()));

        // 旅社区域
        ListTag regionsTag = new ListTag();
        for (InnRegion region : innRegions) {
            CompoundTag regionTag = region.save();
            regionsTag.add(regionTag);
        }
        tag.put("InnRegions", regionsTag);

        return tag;
    }

    /**
     * 从 NBT 加载队伍数据
     *
     * @param tag 源 NBT 标签
     */
    public void load(CompoundTag tag) {
        if (tag.contains("TeamId")) {
            // teamId is final and passed in constructor, usually not overwritten here unless we are loading into a dummy
        }
        if (tag.contains("Name")) {
            name = tag.getString("Name");
        }
        if (tag.contains("LeaderId")) {
            leaderId = tag.getUUID("LeaderId");
        }
        
        members.clear();
        if (tag.contains("Members")) {
            ListTag membersTag = tag.getList("Members", Tag.TAG_COMPOUND);
            for (Tag t : membersTag) {
                if (t instanceof CompoundTag memberTag) {
                    members.add(memberTag.getUUID("UUID"));
                }
            }
        }

        unlockedMapPoints.clear();
        if (tag.contains("UnlockedPoints")) {
            ListTag pointsTag = tag.getList("UnlockedPoints", Tag.TAG_STRING);
            for (Tag t : pointsTag) {
                unlockedMapPoints.add(ResourceLocation.parse(t.getAsString()));
            }
        }

        teleportUnlocked = tag.getBoolean("TeleportUnlocked");
        if (tag.contains("Coins")) {
            coins = tag.getInt("Coins");
        } else {
            coins = 0;
        }
        
        // 优先加载 InnData，因为后续可能需要用到它
        if (tag.contains("InnData")) {
            innData.load(tag.getCompound("InnData"));
        }

        // 加载区域
        innRegions.clear();
        if (tag.contains("InnRegions")) {
            ListTag regionsTag = tag.getList("InnRegions", Tag.TAG_COMPOUND);
            for (Tag t : regionsTag) {
                if (t instanceof CompoundTag regionTag) {
                    innRegions.add(InnRegion.load(regionTag));
                }
            }
        } else {
            // 尝试从旧数据迁移
            BlockPos center = innZoneCenter;
            int radius = innZoneRadius;
            if (tag.contains("InnCenter")) {
                center = BlockPos.of(tag.getLong("InnCenter"));
            }
            if (tag.contains("InnRadius")) {
                radius = tag.getInt("InnRadius");
            }
            // 生成默认区域
            addRegion(new InnRegion(center.getX() - radius, center.getZ() - radius, 
                                    center.getX() + radius, center.getZ() + radius));
        }
    }
}
