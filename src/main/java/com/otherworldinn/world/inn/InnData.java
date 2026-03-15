package com.otherworldinn.world.inn;

import com.otherworldinn.entity.base.GuestEntity;
import com.otherworldinn.foundation.ModBlockProperties;
import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.init.ModEntities;
import com.otherworldinn.util.EntityUtils;
import com.otherworldinn.world.inn.service.ClipboardManager;
import com.otherworldinn.world.inn.service.FurnitureManager;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import com.otherworldinn.world.team.TeamSavedData;
import java.util.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

/**
 * 旅社数据
 *
 * <p>存储旅社的运营状态、旅客列表等信息。
 */
@Data
public class InnData {
    private String name = "My Inn";

    @Setter(AccessLevel.NONE)
    private int rating = 0; // 旅社评级 (0-5)

    @Setter(AccessLevel.NONE)
    private int reputation = 0; // 旅社声望

    @Setter(AccessLevel.NONE)
    private InnState state = InnState.CLOSED; // 默认为歇业

    public enum InnState {
        CLOSED, // 歇业中
        OPEN, // 营业中
        EDIT_MODE // 装修中
    }

    private final Set<UUID> guestIds = new HashSet<>();
    private final Map<Integer, RoomData> rooms = new HashMap<>();

    // 待办事项缓存列表
    private final List<String> todoList = new ArrayList<>();

    // 下一次生成旅客的时间 (GameTime)
    private long nextGuestSpawnTime = 0;
    private static final int BASE_GUEST_WAITING_TIMEOUT = 6000;
    private static final int WAITING_PATIENCE_BONUS_PER_STAR = 1200;
    private static final int MAX_GUEST_WAITING_TIMEOUT = 12000;
    private static final double SPAWN_DELAY_REDUCTION_PER_STAR = 0.08D;
    private static final double MIN_SPAWN_DELAY_MULTIPLIER = 0.60D;

    public InnData() {}

    public void setRating(int rating) {
        this.rating = Math.max(0, Math.min(5, rating));
    }

    public void setReputation(int reputation) {
        this.reputation = Math.max(0, reputation);
    }

    /**
     * 获取当前等级升级所需的最大声望值
     *
     * <p>类似于 Minecraft 的经验值系统。 公式：100 * (rating + 1)
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
     *
     * <p>逻辑暂时留空。
     */
    public void checkLevelUp() {
        // TODO: 实现升级逻辑
    }

    /**
     * 尝试开启编辑模式
     *
     * @return 如果成功开启返回 true，否则返回 false (例如正在营业或有客人)
     */
    public boolean setState(InnState newState) {
        // 如果状态没有改变，直接返回成功
        if (this.state == newState) {
            return true;
        }

        switch (newState) {
            case OPEN:
                // 可以从 CLOSED 切换到 OPEN
                // 不可以直接从 EDIT_MODE 切换到 OPEN (需要先 CLOSED)
                if (this.state == InnState.CLOSED) {
                    this.state = InnState.OPEN;
                    return true;
                }
                break;

            case EDIT_MODE:
                // 只能从 CLOSED 切换到 EDIT_MODE
                // 且必须没有客人
                if (this.state == InnState.CLOSED && this.guestIds.isEmpty()) {
                    this.state = InnState.EDIT_MODE;
                    return true;
                }
                break;

            case CLOSED:
                // 可以从任何状态切换到 CLOSED
                this.state = InnState.CLOSED;
                return true;
        }

        return false;
    }

    public void addGuest(UUID guestId) {
        this.guestIds.add(guestId);
        // 有客人时自动关闭编辑模式，如果处于 Open 状态则保持 Open，否则切换到 Closed (异常情况)
        if (this.state == InnState.EDIT_MODE) {
            this.state = InnState.CLOSED;
        }
    }

    /**
     * 添加旅客（进入旅社范围）
     *
     * @param guest 旅客实体
     * @param team 队伍数据
     * @param level 世界
     */
    public void addGuest(GuestEntity guest, TeamData team, ServerLevel level) {
        if (!guestIds.contains(guest.getUUID())) {
            addGuest(guest.getUUID());

            // 设置状态为等待
            GuestData data = guest.getGuestData();
            data.setWaiting(true, level.getGameTime());

            // 添加待办事项
            String guestName =
                    guest.getCustomName() != null ? guest.getCustomName().getString() : "Guest";
            String todoText =
                    Component.translatable("todo.otherworldinn.guest_waiting", guestName)
                            .getString();
            addTodo(level, team, todoText);
        }
    }

    public void removeGuest(UUID uuid) {
        guestIds.remove(uuid);
    }

    /**
     * 获取旅客数据
     *
     * <p>通过 UUID 在服务器等级中查找实体并获取数据。
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
            team.getMembers()
                    .forEach(
                            uuid -> {
                                Player player = level.getPlayerByUUID(uuid);
                                if (player != null) {
                                    if (reason != null) {
                                        player.displayClientMessage(
                                                Component.translatable(
                                                                "message.otherworldinn.room_register.remove_success_with_reason",
                                                                roomId,
                                                                reason)
                                                        .withStyle(
                                                                style ->
                                                                        style.withColor(
                                                                                ModColors.ERROR)),
                                                false);
                                    } else {
                                        player.displayClientMessage(
                                                Component.translatable(
                                                                "message.otherworldinn.room_register.remove_success",
                                                                roomId)
                                                        .withStyle(
                                                                style ->
                                                                        style.withColor(
                                                                                ModColors.ERROR)),
                                                false);
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
            if (pos.getX() >= min.getX()
                    && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY()
                    && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ()
                    && pos.getZ() <= max.getZ()) {
                return room;
            }
        }
        return null;
    }

    /**
     * 每 tick 更新逻辑
     *
     * <p>处理旅社的全局逻辑，例如统计或批量更新。 旅客个体的逻辑由 GuestEntity 自身处理。
     *
     * @param level 服务器等级
     */
    public void tick(ServerLevel level) {
        // 全局旅客管理逻辑（如自动退房检查）可在此处实现
    }

    /**
     * 计算并更新房间属性
     *
     * <p>遍历房间内的所有方块，查找已注册的家具，累加其属性值。
     *
     * @param roomId 房间ID
     * @param level 服务器等级 (用于获取方块状态)
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
            FurnitureManager.getStats(state.getBlock())
                    .ifPresent(
                            s -> {
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
                int[] bedStats =
                        RoomData.calculateBedStats(room.getMinPos(), room.getMaxPos(), level);
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
     *
     * <p>遍历所有房间，如果不符合合法性规则，则将其删除。
     *
     * @param level 服务器等级
     * @param team 所属队伍 (用于范围检查)
     * @return 被删除的房间ID列表
     */
    public List<Integer> checkAllRoomsValidity(Level level, TeamData team) {
        List<Integer> removedRooms = new ArrayList<>();
        // 收集需要删除的房间ID和原因，避免在遍历时修改集合
        Map<Integer, RoomData.ValidationResult> failureReasons = new HashMap<>();

        for (RoomData room : rooms.values()) {
            RoomData.ValidationResult result =
                    RoomData.validate(
                            room.getMinPos(), room.getMaxPos(), level, team, room.getId());
            if (!result.isSuccess()) {
                removedRooms.add(room.getId());
                failureReasons.put(room.getId(), result);
            } else {
                // 如果验证通过，更新床的数量和整洁度
                int[] bedStats =
                        RoomData.calculateBedStats(room.getMinPos(), room.getMaxPos(), level);
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
     *
     * <p>将旅客分配到指定房间。如果房间已满或不存在，返回 false。 如果旅客已在其他房间，会自动先执行退房。
     *
     * @param guestId 旅客 UUID
     * @param roomId 目标房间ID
     * @param level 服务器等级
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

            // 让实体寻路到房间中心
            Entity entity = level.getEntity(guestId);
            if (entity instanceof GuestEntity guestEntity) {
                BlockPos targetPos = findBestRoomNavigationTarget(level, guestEntity, room);
                guestEntity.setNavigationTarget(targetPos);

                // 移除剪贴板 TODO
                String guestName =
                        entity.getCustomName() != null
                                ? entity.getCustomName().getString()
                                : "Guest";
                // 使用与生成时相同的 Key
                String todoText =
                        Component.translatable("todo.otherworldinn.guest_waiting", guestName)
                                .getString();

                // 获取当前队伍并移除 TODO
                TeamData team =
                        TeamManager.getInstance().getTeamAt(room.getMinPos(), level.getServer());
                if (team != null) {
                    removeTodo(level, team, todoText);
                    // 触发客户端同步，更新 Tooltip
                    TeamManager.getInstance().syncTeam(team, level.getServer());
                }
            }

            return true;
        }

        return false;
    }

    private BlockPos findBestRoomNavigationTarget(
            ServerLevel level, GuestEntity guestEntity, RoomData room) {
        BlockPos min = room.getMinPos();
        BlockPos max = room.getMaxPos();
        int centerX = (min.getX() + max.getX()) / 2;
        int centerY = min.getY() + 1;
        int centerZ = (min.getZ() + max.getZ()) / 2;
        BlockPos fallback = new BlockPos(centerX, centerY, centerZ);

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int y = min.getY(); y <= Math.min(max.getY(), min.getY() + 2); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!isWalkableRoomTarget(level, pos)) {
                        continue;
                    }
                    Path path = guestEntity.getNavigation().createPath(pos, 0);
                    if (path == null || !path.canReach()) {
                        continue;
                    }
                    double dist =
                            guestEntity.distanceToSqr(
                                    pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = pos;
                    }
                }
            }
        }
        if (best != null) {
            return best;
        }
        if (!isWalkableRoomTarget(level, fallback)) {
            return fallback.above();
        }
        return fallback;
    }

    private boolean isWalkableRoomTarget(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());
        return !feet.isSolid() && !head.isSolid() && ground.isSolid();
    }

    // --- 旅客生成 ---

    /**
     * 尝试生成新旅客
     *
     * @param level 服务器等级
     */
    private void trySpawnGuest(ServerLevel level) {
        long currentTime = level.getGameTime();

        // 1. 检查是否到达生成时间
        if (currentTime < nextGuestSpawnTime) {
            return;
        }

        // 2. 检查旅社是否开业
        if (this.state != InnState.OPEN) {
            return;
        }

        // 3. 检查是否有可用床位
        if (!hasAvailableBed()) {
            return;
        }

        // 4. 检查当前世界中等待入住的旅客数量
        if (getWaitingGuestCount(level) >= 3) {
            // 如果等待人数过多，推迟生成
            scheduleNextSpawn(level.getRandom(), currentTime);
            return;
        }

        // 5. 生成旅客
        spawnGuest(level);

        // 6. 安排下一次生成
        scheduleNextSpawn(level.getRandom(), currentTime);
    }

    /** 检查是否有可用床位 */
    private boolean hasAvailableBed() {
        for (RoomData room : rooms.values()) {
            if (room.getCurrentGuests().size() < room.getMaxGuests()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前世界中正在等待入住的旅客数量
     *
     * <p>统计所有处于 IDLE 或 WAITING 状态的旅客实体。
     */
    private int getWaitingGuestCount(ServerLevel level) {
        int count = 0;
        // 遍历所有加载的实体，筛选出 GuestEntity
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof GuestEntity guest) {
                GuestData.GuestState state = guest.getGuestData().getState();
                if (state == GuestData.GuestState.IDLE || state == GuestData.GuestState.WAITING) {
                    count++;
                }
            }
        }
        return count;
    }

    /** 生成旅客实体 */
    private void spawnGuest(ServerLevel level) {
        // 随机坐标范围：(20, 71, 2) ~ (5, 71, -2)
        // X: 5 ~ 20
        // Z: -2 ~ 2
        // Y: 71
        double x = 5 + level.random.nextDouble() * (20 - 5);
        double z = -2 + level.random.nextDouble() * (2 - (-2));
        double y = 71;

        // 检查该位置所在的区块是否加载
        if (!level.isLoaded(BlockPos.containing(x, y, z))) {
            return;
        }

        GuestEntity guest = ModEntities.ORDINARY_GUEST.get().create(level);
        if (guest != null) {
            guest.moveTo(x, y, z, level.random.nextFloat() * 360F, 0.0F);
            guest.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(guest.blockPosition()),
                    MobSpawnType.EVENT,
                    null);
            guest.setNoAi(false);
            guest.setPersistenceRequired();
            level.addFreshEntity(guest);
        }
    }

    // 修改 scheduleNextSpawn 为返回 delay
    private int calculateNextSpawnDelay(RandomSource random) {
        double gaussian = random.nextGaussian();
        int delay = (int) (1900 + gaussian * 566);
        delay = Math.max(200, Math.min(3600, delay));
        return (int) Math.round(delay * getSpawnDelayMultiplier());
    }

    private double getSpawnDelayMultiplier() {
        int clampedRating = Math.max(0, Math.min(5, this.rating));
        double multiplier = 1.0D - clampedRating * SPAWN_DELAY_REDUCTION_PER_STAR;
        return Math.max(MIN_SPAWN_DELAY_MULTIPLIER, multiplier);
    }

    private int getGuestWaitingTimeoutTicks() {
        int clampedRating = Math.max(0, Math.min(5, this.rating));
        int timeout = BASE_GUEST_WAITING_TIMEOUT + clampedRating * WAITING_PATIENCE_BONUS_PER_STAR;
        return Math.min(MAX_GUEST_WAITING_TIMEOUT, timeout);
    }

    /**
     * 辅助方法：安排下一次生成
     *
     * @param random 随机源
     * @param currentTime 当前游戏时间
     */
    private void scheduleNextSpawn(RandomSource random, long currentTime) {
        this.nextGuestSpawnTime = currentTime + calculateNextSpawnDelay(random);
    }

    // --- 辅助方法 ---

    /**
     * 将房间内的一张干净的床设置为脏乱状态
     *
     * @param roomId 房间ID
     * @param level 服务器等级
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
                // 找到第一张可处理的床并标记为脏乱
                if (state.hasProperty(ModBlockProperties.MESSY)) {
                    // 检查是否已经是脏乱的，我们只弄乱干净的床
                    if (!state.getValue(ModBlockProperties.MESSY)) {
                        level.setBlock(pos, state.setValue(ModBlockProperties.MESSY, true), 3);

                        // 如果是床头，还需要处理床脚，反之亦然。
                        BedPart part = state.getValue(BedBlock.PART);
                        // 根据 Facing 和 Part 来推断另一半
                        Direction facing = state.getValue(BedBlock.FACING);
                        BlockPos otherPos =
                                pos.relative(part == BedPart.HEAD ? facing.getOpposite() : facing);

                        BlockState otherState = level.getBlockState(otherPos);
                        // 检查另一半是否也是床且也是正确的部分
                        if (otherState.getBlock() instanceof BedBlock
                                && otherState.hasProperty(ModBlockProperties.MESSY)) {
                            // 简单的双重检查，确保是同一张床
                            if (otherState.getValue(BedBlock.PART) != part) {
                                level.setBlock(
                                        otherPos,
                                        otherState.setValue(ModBlockProperties.MESSY, true),
                                        3);
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
     * 每 tick 更新
     *
     * <p>检查等待超时的旅客。
     *
     * @param level 世界
     * @param team 队伍数据
     */
    public void tick(ServerLevel level, TeamData team) {
        long currentTime = level.getGameTime();

        // 尝试生成旅客 (每 20 tick 检查一次，减少开销)
        if (currentTime % 20 == 0) {
            trySpawnGuest(level);
        }

        // 每 5 tick 检查一次
        if (currentTime % 5 != 0) return;

        // 遍历旅客检查状态

        List<UUID> guestsToDepart = new ArrayList<>();
        List<UUID> guestsToCheckOut = new ArrayList<>();
        int waitingTimeoutTicks = getGuestWaitingTimeoutTicks();

        for (UUID guestId : guestIds) {
            Entity entity = level.getEntity(guestId);
            if (entity instanceof GuestEntity guestEntity) {
                GuestData guestData = guestEntity.getGuestData();
                if (guestData.getState() == GuestData.GuestState.WAITING) {
                    if (currentTime - guestData.getWaitingSince() > waitingTimeoutTicks
                            || this.state != InnState.OPEN) {
                        guestsToDepart.add(guestId);
                    }
                } else if (guestData.getState() == GuestData.GuestState.CHECKED_IN) {
                    // 检查是否到达退房时间
                    if (currentTime >= guestData.getCheckoutTime()) {
                        guestsToCheckOut.add(guestId);
                    }
                }
            }
        }

        // 处理离开
        for (UUID guestId : guestsToDepart) {
            handleGuestDeparture(guestId, true, level, team);
            // handleGuestDeparture 内部不调用 removeGuest，所以这里手动移除
            removeGuest(guestId);
        }

        // 处理退房
        for (UUID guestId : guestsToCheckOut) {
            checkOut(guestId, level, true);
            // checkOut 内部会调用 removeGuest
        }
    }

    /**
     * 处理旅客离开
     *
     * @param guestId 旅客 ID
     * @param isAngry 是否生气离开
     * @param level 世界
     * @param team 队伍数据
     */
    public void handleGuestDeparture(
            UUID guestId, boolean isAngry, ServerLevel level, TeamData team) {
        Entity entity = level.getEntity(guestId);

        // 1. 获取并移除待办事项 (如果是等待中离开)
        if (entity instanceof GuestEntity guestEntity) {
            GuestData guestData = guestEntity.getGuestData();
            if (guestData.getState() == GuestData.GuestState.WAITING) {
                String guestName =
                        entity.getCustomName() != null
                                ? entity.getCustomName().getString()
                                : "Guest";
                String todoText =
                        Component.translatable("todo.otherworldinn.guest_waiting", guestName)
                                .getString();
                removeTodo(level, team, todoText);
            }
        }

        if (isAngry) {
            // 生气离开逻辑
            // 1. 扣除声望 (2-6点)
            int reputationLoss = 2 + level.random.nextInt(5);
            this.addReputation(-reputationLoss);

            // 2. 播放特效
            if (entity != null) {
                // 生气粒子
                level.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                        entity.getX(),
                        entity.getY() + entity.getEyeHeight() + 0.5,
                        entity.getZ(),
                        5,
                        0.5,
                        0.5,
                        0.5,
                        0.02);

                // 生气音效
                level.playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                        SoundSource.NEUTRAL,
                        1.0f,
                        1.0f);
            }
        } else {
            // 正常退房由 checkOut 处理；此处处理异常离开
        }

        // 通用离开逻辑：移除占用并触发离场

        if (entity instanceof GuestEntity guestEntity) {
            guestEntity.setNavigationTarget(new BlockPos(10, 71, 0));
            EntityUtils.scheduleDisappear(guestEntity);

            // 更新状态
            guestEntity.getGuestData().setCheckedOut(true);
        }

        // 从列表移除：tick 内遍历场景由调用方处理
        if (!isAngry) { // 仅非 tick 调用的情况
            removeGuest(guestId);
        }
    }

    /**
     * 旅客退房
     *
     * <p>将旅客从当前房间移除，并从旅社旅客名单中删除。 如果旅客实体存在，会触发奖励物品掉落。 此外，会将房间内的一张床标记为脏乱。
     *
     * @param guestId 旅客UUID
     * @param level 服务器等级
     * @param isNormalCheckout 是否为正常退房（如果为 false，则不计算房费）
     */
    public void checkOut(UUID guestId, ServerLevel level, boolean isNormalCheckout) {
        Entity entity = level.getEntity(guestId);
        GuestData guest = null;
        if (entity instanceof GuestEntity guestEntity) {
            guest = guestEntity.getGuestData();
            if (guest != null) {
                guest.dropRewards(level, entity.blockPosition());
            }
        }
        RoomData targetRoom = null;
        if (guest != null && guest.getRoomId() != -1) {
            targetRoom = rooms.get(guest.getRoomId());
        }
        if (targetRoom == null) {
            for (RoomData room : rooms.values()) {
                if (room.hasGuest(guestId)) {
                    targetRoom = room;
                    break;
                }
            }
        }
        TeamData team = null;
        if (targetRoom != null) {
            boolean bedMessy = setRoomBedMessy(targetRoom.getId(), level);
            if (bedMessy) {
                targetRoom.setMaxGuests(Math.max(0, targetRoom.getMaxGuests() - 1));
            }
            targetRoom.removeGuest(guestId);
            team = TeamManager.getInstance().getTeamAt(targetRoom.getMinPos(), level.getServer());
            if (team == null) {
                TeamSavedData data = TeamManager.getInstance().getData(level.getServer());
                if (data != null) {
                    for (TeamData candidate : data.getTeams().values()) {
                        RoomData candidateRoom = candidate.getInnData().getRoom(targetRoom.getId());
                        if (candidateRoom != null
                                && candidateRoom.getUuid().equals(targetRoom.getUuid())) {
                            team = candidate;
                            break;
                        }
                    }
                }
            }
            if (isNormalCheckout && team != null) {
                int price = targetRoom.getBedPrice(this.rating);
                team.addCoins(price, level.getServer());
                if (bedMessy) {
                    String todoText =
                            Component.translatable(
                                            "todo.otherworldinn.room_cleaning", targetRoom.getId())
                                    .getString();
                    this.addTodo(level, team, todoText);
                }
            }
            if (isNormalCheckout && guest != null) {
                guest.updatePreferenceScore(targetRoom);
                int score = guest.getPreferenceScore();
                int reputationGain = Math.max(2, Math.min(10, score));
                this.addReputation(reputationGain);
            }
        }
        if (guest != null) {
            guest.setRoomId(-1);
            guest.setCheckedOut(true);
        }
        removeGuest(guestId);
        if (team != null) {
            TeamManager.getInstance().syncTeam(team, level.getServer());
        }
        if (entity instanceof GuestEntity guestEntity) {
            guestEntity.setNavigationTarget(new BlockPos(10, 71, 0));
        }
        if (entity != null) {
            EntityUtils.scheduleDisappear(entity);
        }
    }

    /**
     * 添加待办事项
     *
     * <p>同时添加到缓存列表和实际剪贴板中。
     *
     * @param level 世界
     * @param team 队伍数据
     * @param todoText 待办事项文本
     * @return 是否成功添加到剪贴板（如果只添加到缓存也算处理成功，但返回 false 表示没有物理剪贴板更新）
     */
    public boolean addTodo(Level level, TeamData team, String todoText) {
        // 1. 添加到缓存
        if (!todoList.contains(todoText)) {
            todoList.add(todoText);
        }

        // 2. 尝试同步到剪贴板
        boolean addedToClipboard = false;
        for (TeamData.InnRegion region : team.getInnRegions()) {
            AABB area =
                    new AABB(region.minX(), -64, region.minZ(), region.maxX(), 320, region.maxZ());
            if (ClipboardManager.addTodo(level, area, todoText)) {
                addedToClipboard = true;
            }
        }

        // 3. 广播通知
        if (addedToClipboard && level instanceof ServerLevel serverLevel) {
            team.getMembers()
                    .forEach(
                            uuid -> {
                                ServerPlayer player =
                                        serverLevel.getServer().getPlayerList().getPlayer(uuid);
                                if (player != null) {
                                    player.displayClientMessage(
                                            Component.translatable(
                                                            "message.otherworldinn.todo.new_task",
                                                            todoText)
                                                    .withStyle(
                                                            style ->
                                                                    style.withColor(
                                                                            ModColors.INFO)),
                                            false);
                                    player.playNotifySound(
                                            SoundEvents.NOTE_BLOCK_BELL.value(),
                                            SoundSource.PLAYERS,
                                            1.0f,
                                            1.0f);
                                }
                            });
        }

        return addedToClipboard;
    }

    /**
     * 移除待办事项
     *
     * @param level 世界
     * @param team 队伍数据
     * @param todoText 待办事项文本
     */
    public void removeTodo(Level level, TeamData team, String todoText) {
        // 1. 从缓存移除
        todoList.remove(todoText);

        // 2. 从剪贴板移除
        for (TeamData.InnRegion region : team.getInnRegions()) {
            AABB area =
                    new AABB(region.minX(), -64, region.minZ(), region.maxX(), 320, region.maxZ());
            ClipboardManager.removeTodo(level, area, todoText);
        }
    }

    /**
     * 同步缓存的待办事项到指定区域的剪贴板
     *
     * <p>通常在放置新的剪贴板时调用。
     */
    public void syncTodosToClipboard(Level level, AABB area) {
        if (todoList.isEmpty()) return;

        for (String todo : todoList) {
            ClipboardManager.addTodo(level, area, todo);
        }
    }

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
        tag.putString("State", state.name());

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

        // 保存待办事项
        ListTag todosTag = new ListTag();
        for (String todo : todoList) {
            todosTag.add(StringTag.valueOf(todo));
        }
        tag.put("TodoList", todosTag);

        tag.putLong("NextGuestSpawnTime", nextGuestSpawnTime);

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

        if (tag.contains("State")) {
            try {
                state = InnState.valueOf(tag.getString("State"));
            } catch (IllegalArgumentException e) {
                state = InnState.CLOSED;
            }
        } else {
            // 兼容旧数据
            boolean isOpen = tag.contains("Open") && tag.getBoolean("Open");
            boolean isEditMode = tag.contains("EditMode") && tag.getBoolean("EditMode");

            if (isOpen) {
                state = InnState.OPEN;
            } else if (isEditMode) {
                state = InnState.EDIT_MODE;
            } else {
                state = InnState.CLOSED;
            }
        }

        if (tag.contains("NextGuestSpawnTime")) {
            nextGuestSpawnTime = tag.getLong("NextGuestSpawnTime");
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

        todoList.clear();
        if (tag.contains("TodoList")) {
            ListTag todosTag = tag.getList("TodoList", Tag.TAG_STRING);
            for (Tag t : todosTag) {
                todoList.add(t.getAsString());
            }
        }
    }
}
