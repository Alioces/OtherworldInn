package com.otherworldinn.world.inn;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.GuestEntity;
import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.mixin.BedBlockExtension;
import com.otherworldinn.util.EntityUtils;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.*;

/**
 * 旅社数据
 * <p>
 * 存储旅社的运营状态、旅客列表等信息。
 * </p>
 */
@Data
public class InnData {
    private String name = "My Inn";
    
    @Setter(AccessLevel.NONE)
    private int rating = 0; // 旅社评级 (0-5)
    
    @Setter(AccessLevel.NONE)
    private int reputation = 0; // 旅社声望

    @Setter(AccessLevel.NONE)
    private boolean open = false; // 默认为歇业
    
    @Setter(AccessLevel.NONE)
    private boolean editMode = false; // 默认为非编辑模式
    
    private final Set<UUID> guestIds = new HashSet<>();
    private final Map<Integer, RoomData> rooms = new HashMap<>();

    public InnData() {
    }

    public void setRating(int rating) {
        this.rating = Math.max(0, Math.min(5, rating));
    }

    public void setReputation(int reputation) {
        this.reputation = Math.max(0, reputation);
    }
    
    /**
     * 获取当前等级升级所需的最大声望值
     * <p>
     * 类似于 Minecraft 的经验值系统。
     * 公式：100 * (rating + 1)
     * </p>
     *
     * @param rating 当前星级
     * @return 升级所需声望
     */
    public int getMaxReputation(int rating) {
        return 100 * (rating + 1);
    }
    
    /**
     * 增加声望
     *
     * @param amount 增加的数值
     */
    public void addReputation(int amount) {
        this.reputation += amount;
        if (this.reputation < 0) {
            this.reputation = 0;
        }
    }
    
    /**
     * 检查是否可以升级旅社星级
     * <p>
     * 逻辑暂时留空。
     * </p>
     */
    public void checkLevelUp() {
        // TODO: 实现升级逻辑
    }

    public void setOpen(boolean open) {
        this.open = open;
        // 如果开启营业，自动关闭编辑模式
        if (open) {
            this.editMode = false;
        }
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

    public void addRoom(RoomData room) {
        this.rooms.put(room.getId(), room);
    }

    public void removeRoom(int roomId) {
        removeRoom(roomId, null, null, null);
    }

    public void removeRoom(int roomId, Level level, TeamData team) {
        removeRoom(roomId, level, team, null);
    }

    public void removeRoom(int roomId, Level level, TeamData team, Component reason) {
        this.rooms.remove(roomId);
        
        // 通知队伍所有成员
        if (level != null && team != null) {
            team.getMembers().forEach(uuid -> {
                Player player = level.getPlayerByUUID(uuid);
                if (player != null) {
                    if (reason != null) {
                        player.displayClientMessage(Component.translatable("message.otherworldinn.room_register.remove_success_with_reason", roomId, reason)
                                .withStyle(style -> style.withColor(ModColors.ERROR)), false);
                    } else {
                        player.displayClientMessage(Component.translatable("message.otherworldinn.room_register.remove_success", roomId)
                                .withStyle(style -> style.withColor(ModColors.ERROR)), false);
                    }
                }
            });
        }
    }

    public RoomData getRoom(int roomId) {
        return this.rooms.get(roomId);
    }

    public RoomData getRoomAt(BlockPos pos) {
        for (RoomData room : rooms.values()) {
            BlockPos min = room.getMinPos();
            BlockPos max = room.getMaxPos();
            if (pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
                pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
                pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ()) {
                return room;
            }
        }
        return null;
    }
    
    /**
     * 每 tick 更新逻辑
     * <p>
     * 处理旅社的全局逻辑，例如统计或批量更新。
     * 旅客个体的逻辑由 GuestEntity 自身处理。
     * </p>
     *
     * @param level 服务器等级
     */
    public void tick(ServerLevel level) {
        // 全局旅客管理逻辑（如自动退房检查）可在此处实现
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
    public void calculateRoomStats(int roomId, Level level) {
        RoomData room = rooms.get(roomId);
        if (room == null) {
            return;
        }

        // 使用局部变量累加，避免 lambda 表达式中无法修改局部变量的问题
        final int[] stats = new int[3]; // [0]: comfort, [1]: light, [2]: humidity

        BlockPos min = room.getMinPos();
        BlockPos max = room.getMaxPos();

        // 遍历房间区域
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            // 获取方块状态
            BlockState state = level.getBlockState(pos);
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
    public void updateAllRoomsStats(Level level) {
        for (Integer roomId : rooms.keySet()) {
            calculateRoomStats(roomId, level);
            // 更新房间内所有旅客的偏好分数
            RoomData room = rooms.get(roomId);
            if (room != null && level instanceof ServerLevel serverLevel) {
                // 更新房间整洁度（虽然不用于平均计算，但可能用于其他逻辑）
                int[] bedStats = RoomData.calculateBedStats(room.getMinPos(), room.getMaxPos(), level);
                room.setMaxGuests(bedStats[0]); // 有效床位
                room.setCleanliness(bedStats[1]); // 整洁度
                
                for (UUID guestId : room.getCurrentGuests()) {
                    GuestData guest = getGuestData(guestId, serverLevel);
                    if (guest != null) {
                        guest.updatePreferenceScore(room);
                    }
                }
            }
        }
    }
    
    /**
     * 获取总房间数量
     *
     * @return 房间总数
     */
    public int getRoomCount() {
        return rooms.size();
    }
    
    /**
     * 获取旅社平均舒适度
     *
     * @return 平均舒适度 (0-100)，如果没有房间则返回 0
     */
    public int getAverageComfort() {
        if (rooms.isEmpty()) {
            return 0;
        }
        
        int totalComfort = 0;
        for (RoomData room : rooms.values()) {
            totalComfort += room.getComfort();
        }
        
        return totalComfort / rooms.size();
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
        // 收集需要删除的房间ID和原因，避免在遍历时修改集合
        Map<Integer, RoomData.ValidationResult> failureReasons = new HashMap<>();

        for (RoomData room : rooms.values()) {
            RoomData.ValidationResult result = RoomData.validate(room.getMinPos(), room.getMaxPos(), level, team, room.getId());
            if (!result.isSuccess()) {
                removedRooms.add(room.getId());
                failureReasons.put(room.getId(), result);
            } else {
                // 如果验证通过，更新床的数量和整洁度
                int[] bedStats = RoomData.calculateBedStats(room.getMinPos(), room.getMaxPos(), level);
                room.setMaxGuests(bedStats[0]);
                room.setCleanliness(bedStats[1]);
            }
        }
        
        // 删除无效房间
        for (Integer roomId : removedRooms) {
            RoomData.ValidationResult reason = failureReasons.get(roomId);
            removeRoom(roomId, level, team, Component.translatable(reason.getTranslationKey()));
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

        // 检查旅客是否已在其他房间
        if (guest.getRoomId() != -1) {
            return false;
        }

        // 执行入住逻辑
        if (room.addGuest(guestId)) {
            guest.setRoomId(roomId);
            // 更新偏好分数
            guest.updatePreferenceScore(room);
            this.addGuest(guestId);
            
            // 让实体寻路到房间
            Entity entity = level.getEntity(guestId);
            if (entity instanceof GuestEntity guestEntity) {
                guestEntity.setNavigationTarget(room.getMinPos());
            }
            
            return true;
        }
        
        return false;
    }

    // --- 辅助方法 ---
    
    /**
     * 将房间内的一张干净的床设置为脏乱状态
     *
     * @param roomId 房间ID
     * @param level  服务器等级
     * @return 是否成功弄乱了一张床
     */
    private boolean setRoomBedMessy(int roomId, ServerLevel level) {
        RoomData room = rooms.get(roomId);
        if (room == null) return false;
        
        BlockPos min = room.getMinPos();
        BlockPos max = room.getMaxPos();
        
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BedBlock) {
                // 确保我们设置的是床头，或者两部分都设置
                // 实际上只需要设置一部分，因为床通常是联动的，但为了保险起见，或者只设置床头
                // 这里我们简单地找到第一张床并设置其为脏乱
                if (state.hasProperty(BedBlockExtension.MESSY)) {
                    // 检查是否已经是脏乱的，我们只弄乱干净的床
                    if (!state.getValue(BedBlockExtension.MESSY)) {
                        level.setBlock(pos, state.setValue(BedBlockExtension.MESSY, true), 3);
                        
                        // 如果是床头，还需要处理床脚，反之亦然。
                        BedPart part = state.getValue(BedBlock.PART);
                        // 根据 Facing 和 Part 来推断另一半
                        Direction facing = state.getValue(BedBlock.FACING);
                        BlockPos otherPos = pos.relative(part == BedPart.HEAD ? facing.getOpposite() : facing);
                        
                        BlockState otherState = level.getBlockState(otherPos);
                        // 检查另一半是否也是床且也是正确的部分
                        if (otherState.getBlock() instanceof BedBlock && 
                            otherState.hasProperty(BedBlockExtension.MESSY)) {
                            // 简单的双重检查，确保是同一张床
                            if (otherState.getValue(BedBlock.PART) != part) {
                                level.setBlock(otherPos, otherState.setValue(BedBlockExtension.MESSY, true), 3);
                            }
                        }
                        
                        // 只弄乱一张床即可，并返回成功
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * 旅客退房
     * <p>
     * 将旅客从当前房间移除，并从旅社旅客名单中删除。
     * 如果旅客实体存在，会触发奖励物品掉落。
     * 此外，会将房间内的一张床标记为脏乱。
     * </p>
     *
     * @param guestId          旅客UUID
     * @param level            服务器等级
     * @param isNormalCheckout 是否为正常退房（如果为 false，则不计算房费）
     */
    public void checkOut(UUID guestId, ServerLevel level, boolean isNormalCheckout) {
        // 尝试获取实体（如果已加载）
        Entity entity = level.getEntity(guestId);
        GuestData guest = null;
        
        if (entity instanceof GuestEntity guestEntity) {
            guest = guestEntity.getGuestData();
            
            // 触发奖励掉落
            if (guest != null) {
                guest.dropRewards(level, entity.blockPosition());
            }
        }
        
        // 1. 清理房间记录
        // 如果能获取到 GuestData，直接定位房间清理
        if (guest != null) {
            int currentRoomId = guest.getRoomId();
            if (currentRoomId != -1) {
                // 将一张干净的床弄乱
                if (setRoomBedMessy(currentRoomId, level)) {
                    // 如果成功弄乱了床，说明可用床位减少了一个
                    // 直接减少最大可入住人数，避免全量重新计算
                    RoomData room = rooms.get(currentRoomId);
                    if (room != null) {
                        room.setMaxGuests(Math.max(0, room.getMaxGuests() - 1));
                        
                        // 计算房费并添加到队伍金币 (仅在正常退房时执行)
                        if (isNormalCheckout) {
                            TeamData team = TeamManager.getInstance().getTeamAt(room.getMinPos(), level.getServer());
                            if (team != null) {
                                int price = room.getBedPrice(this.rating);
                                team.addCoins(price, level.getServer());
                                TeamManager.getInstance().syncTeam(team, level.getServer());
                            }
                            
                            // 计算并增加声望
                            guest.updatePreferenceScore(room);
                            int score = guest.getPreferenceScore();
                            // 最低提升 2，最高提升 10 (score 本身是 0-10)
                            int reputationGain = Math.max(2, Math.min(10, score));
                            this.addReputation(reputationGain);
                        } else {
                            // 即使非正常退房，如果修改了 maxGuests，仍需同步队伍数据
                            TeamData team = TeamManager.getInstance().getTeamAt(room.getMinPos(), level.getServer());
                            if (team != null) {
                                TeamManager.getInstance().syncTeam(team, level.getServer());
                            }
                        }
                    }
                }
                
                RoomData room = rooms.get(currentRoomId);
                if (room != null) {
                    room.removeGuest(guestId);
                }
                guest.setRoomId(-1);
                // 标记为已退房
                guest.setCheckedOut(true);
            }
        } else {
            // 如果无法获取 GuestData，也尝试清理
            for (RoomData room : rooms.values()) {
                if (room.hasGuest(guestId)) {
                    // 将一张干净的床弄乱
                    if (setRoomBedMessy(room.getId(), level)) {
                        room.setMaxGuests(Math.max(0, room.getMaxGuests() - 1));
                        
                        // 计算房费并添加到队伍金币 (仅在正常退房时执行)
                        if (isNormalCheckout) {
                            TeamData team = TeamManager.getInstance().getTeamAt(room.getMinPos(), level.getServer());
                            if (team != null) {
                                int price = room.getBedPrice(this.rating);
                                team.addCoins(price, level.getServer());
                                TeamManager.getInstance().syncTeam(team, level.getServer());
                            }
                        } else {
                            // 即使非正常退房，如果修改了 maxGuests，仍需同步队伍数据
                            TeamData team = TeamManager.getInstance().getTeamAt(room.getMinPos(), level.getServer());
                            if (team != null) {
                                TeamManager.getInstance().syncTeam(team, level.getServer());
                            }
                        }
                    }
                    
                    room.removeGuest(guestId);
                    // 找到并移除后即可停止遍历，因为一个旅客只能住一个房间
                    break;
                }
            }
        }

        // 2. 从旅社旅客名单中彻底移除
        removeGuest(guestId);
        
        // 3. 安排实体寻路到指定位置并自行消失
        if (entity instanceof GuestEntity guestEntity) {
            // 设置目标位置 (10, 71, 0)
            guestEntity.setNavigationTarget(new BlockPos(10, 71, 0));
        }
        
        // 4. 安排实体消失 (如果实体存在)
        if (entity != null) {
            EntityUtils.scheduleDisappear(entity);
        }
    }

    // --- NBT 序列化 ---

    /**
     * 保存数据到 NBT
     *
     * @param tag 目标标签
     * @return 写入数据的标签
     */
    public CompoundTag save(CompoundTag tag) {
        tag.putString("Name", name);
        tag.putInt("Rating", rating);
        tag.putInt("Reputation", reputation);
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

    /**
     * 从 NBT 加载数据
     *
     * @param tag 源标签
     */
    public void load(CompoundTag tag) {
        if (tag.contains("Name")) {
            name = tag.getString("Name");
        }
        if (tag.contains("Rating")) {
            rating = tag.getInt("Rating");
        }
        if (tag.contains("Reputation")) {
            reputation = tag.getInt("Reputation");
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
