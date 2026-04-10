package com.otherworldinn.world.inn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 旅客数据
 *
 * <p>存储旅社中单个旅客的信息。
 */
@Data
public class GuestData {
    private final UUID uuid;
    private long checkoutTime; // 预计退房时间 (GameTime)

    // 旅客状态
    public enum GuestState {
        IDLE, // 空闲
        WAITING, // 等待入住
        CHECKED_IN, // 已入住
        CHECKED_OUT // 已退房
    }

    private GuestState state = GuestState.IDLE;
    private long waitingSince = 0; // 开始等待的时间 (GameTime)

    // 房间属性偏好 (区间)
    private IntRange comfortPreference = new IntRange(0, 100);
    private IntRange lightPreference = new IntRange(0, 100);
    private IntRange humidityPreference = new IntRange(0, 100);

    // 房间 ID
    private int roomId = -1;
    @Nullable private BlockPos assignedBedPos = null;

    public void setRoomId(int roomId) {
        this.roomId = roomId;
        if (roomId != -1) {
            this.state = GuestState.CHECKED_IN;
            this.waitingSince = 0;
        } else if (this.state == GuestState.CHECKED_IN) {
            this.state = GuestState.IDLE;
            this.assignedBedPos = null;
        }
    }

    public void setCheckedOut(boolean checkedOut) {
        if (checkedOut) {
            this.state = GuestState.CHECKED_OUT;
            this.roomId = -1;
            this.assignedBedPos = null;
        } else if (this.state == GuestState.CHECKED_OUT) {
            this.state = GuestState.IDLE;
        }
    }

    public boolean isCheckedOut() {
        return this.state == GuestState.CHECKED_OUT;
    }

    public void setWaiting(boolean waiting, long currentTime) {
        if (waiting) {
            if (this.state != GuestState.WAITING) {
                this.state = GuestState.WAITING;
                this.waitingSince = currentTime;
            }
        } else {
            if (this.state == GuestState.WAITING) {
                this.state = GuestState.IDLE;
                this.waitingSince = 0;
            }
        }
    }

    // 隐式偏好分数 (0-10)
    @Setter(AccessLevel.NONE)
    private int preferenceScore = 0;

    /**
     * 奖励物品列表
     *
     * <p>旅客退房时可能给予的奖励物品。
     */
    private final List<RewardItem> rewardItems = new ArrayList<>();

    /**
     * 构造一个新的旅客数据
     *
     * @param uuid 旅客UUID
     * @param checkoutTime 预计退房时间 (GameTime)
     */
    public GuestData(UUID uuid, long checkoutTime) {
        this.uuid = uuid;
        this.checkoutTime = checkoutTime;
    }

    public void setComfortPreference(int min, int max) {
        this.comfortPreference = new IntRange(min, max);
    }

    public void setLightPreference(int min, int max) {
        this.lightPreference = new IntRange(min, max);
    }

    public void setHumidityPreference(int min, int max) {
        this.humidityPreference = new IntRange(min, max);
    }

    public void setPreferenceScore(int preferenceScore) {
        this.preferenceScore = Math.max(0, Math.min(10, preferenceScore));
    }

    /**
     * 根据房间属性更新偏好分数
     *
     * <p>计算逻辑： 1. 基础分 10 分。 2. 对每个属性（舒适度、光照、湿度），计算房间属性值与偏好范围中位数的差距。 3. 差距越大，扣分越多。 - 差距 <=
     * 范围半径：不扣分（即在偏好范围内）。 - 差距 > 范围半径：每超出 5 点扣 1 分。
     *
     * @param room 房间数据
     */
    public void updatePreferenceScore(RoomData room) {
        if (room == null) {
            this.preferenceScore = 0;
            return;
        }

        int totalPenalty = 0;

        totalPenalty += calculatePenalty(comfortPreference, room.getComfort());
        totalPenalty += calculatePenalty(lightPreference, room.getLight());
        totalPenalty += calculatePenalty(humidityPreference, room.getHumidity());

        this.preferenceScore = Math.max(0, 10 - totalPenalty);
    }

    private int calculatePenalty(IntRange preference, int actualValue) {
        double median = (preference.min + preference.max) / 2.0;
        double radius = (preference.max - preference.min) / 2.0;
        double diff = Math.abs(actualValue - median);

        // 如果在范围内（差距小于等于半径），不扣分
        if (diff <= radius) {
            return 0;
        }

        // 超出范围的部分
        double excess = diff - radius;

        // 每超出 5 点扣 1 分
        return (int) Math.ceil(excess / 5.0);
    }

    // --- 奖励物品管理 ---

    public void addRewardItem(ResourceLocation item, int min, int max) {
        this.rewardItems.add(new RewardItem(item, new IntRange(min, max)));
    }

    /**
     * 在指定位置掉落奖励物品并清空列表
     *
     * @param level 服务器等级
     * @param pos 掉落位置
     */
    public void dropRewards(ServerLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();

        // 根据偏好分数计算掉落概率乘数 (0.0 - 1.0)
        // 0 分 -> 0.0 (不掉落)
        // 10 分 -> 1.0 (正常掉落)
        float multiplier = this.preferenceScore / 10.0f;

        for (RewardItem reward : rewardItems) {
            // 计算基础数量
            int count = random.nextInt(reward.count.max - reward.count.min + 1) + reward.count.min;

            // 应用概率判定
            // 只有当随机值 < multiplier 时才掉落
            if (random.nextFloat() >= multiplier) {
                count = 0;
            }

            if (count > 0) {
                // 为了避免 lambda 问题，先获取 Item 再处理
                var itemOptional = BuiltInRegistries.ITEM.getOptional(reward.item);
                if (itemOptional.isPresent()) {
                    ItemStack stack = new ItemStack(itemOptional.get(), count);
                    ItemEntity itemEntity =
                            new ItemEntity(
                                    level,
                                    pos.getX() + 0.5,
                                    pos.getY() + 0.5,
                                    pos.getZ() + 0.5,
                                    stack);
                    itemEntity.setDeltaMovement(0.0, 0.0, 0.0);

                    // 设置特殊属性：发光、无重力、无敌、永不消失
                    itemEntity.setGlowingTag(true);
                    itemEntity.setNoGravity(true);
                    itemEntity.setInvulnerable(true);
                    itemEntity.setUnlimitedLifetime();

                    level.addFreshEntity(itemEntity);
                }
            }
        }
        // 掉落后清空奖励列表，防止重复获取
        rewardItems.clear();
    }

    // --- NBT 序列化 ---

    /**
     * 保存数据到 NBT
     *
     * @param tag 目标标签
     * @return 写入数据的标签
     */
    public CompoundTag save(CompoundTag tag) {
        tag.putUUID("UUID", uuid);
        tag.putLong("CheckoutTime", checkoutTime);
        tag.putInt("RoomID", roomId);
        if (assignedBedPos != null) {
            tag.putLong("AssignedBedPos", assignedBedPos.asLong());
        }
        tag.putInt("State", state.ordinal());
        tag.putLong("WaitingSince", waitingSince);

        tag.put("Comfort", comfortPreference.save());
        tag.put("Light", lightPreference.save());
        tag.put("Humidity", humidityPreference.save());
        tag.putInt("PreferenceScore", preferenceScore);

        ListTag rewardsTag = new ListTag();
        for (RewardItem reward : rewardItems) {
            rewardsTag.add(reward.save());
        }
        tag.put("Rewards", rewardsTag);

        return tag;
    }

    /**
     * 从 NBT 加载数据
     *
     * @param tag 源标签
     * @return 加载的旅客数据
     */
    public static GuestData load(CompoundTag tag) {
        UUID uuid = tag.getUUID("UUID");
        long checkoutTime = tag.getLong("CheckoutTime");
        GuestData guest = new GuestData(uuid, checkoutTime);

        if (tag.contains("RoomID")) {
            guest.roomId = tag.getInt("RoomID");
        }
        if (tag.contains("AssignedBedPos")) {
            guest.assignedBedPos = BlockPos.of(tag.getLong("AssignedBedPos"));
        }

        if (tag.contains("State")) {
            int stateOrdinal = tag.getInt("State");
            if (stateOrdinal >= 0 && stateOrdinal < GuestState.values().length) {
                guest.state = GuestState.values()[stateOrdinal];
            }
        } else if (tag.contains("CheckedOut")) {
            // 兼容旧数据
            boolean checkedOut = tag.getBoolean("CheckedOut");
            if (checkedOut) {
                guest.state = GuestState.CHECKED_OUT;
            } else if (guest.roomId != -1) {
                guest.state = GuestState.CHECKED_IN;
            } else {
                guest.state = GuestState.IDLE;
            }
        }

        if (tag.contains("WaitingSince")) {
            guest.waitingSince = tag.getLong("WaitingSince");
        }

        if (tag.contains("Comfort")) {
            guest.comfortPreference = IntRange.load(tag.getCompound("Comfort"));
        }
        if (tag.contains("Light")) {
            guest.lightPreference = IntRange.load(tag.getCompound("Light"));
        }
        if (tag.contains("Humidity")) {
            guest.humidityPreference = IntRange.load(tag.getCompound("Humidity"));
        }
        if (tag.contains("PreferenceScore")) {
            guest.preferenceScore = tag.getInt("PreferenceScore");
        }

        if (tag.contains("Rewards")) {
            ListTag rewardsTag = tag.getList("Rewards", Tag.TAG_COMPOUND);
            for (Tag t : rewardsTag) {
                if (t instanceof CompoundTag rewardTag) {
                    guest.rewardItems.add(RewardItem.load(rewardTag));
                }
            }
        }

        return guest;
    }

    /** 奖励物品记录类 */
    public record RewardItem(ResourceLocation item, IntRange count) {
        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Item", item.toString());
            tag.put("Count", count.save());
            return tag;
        }

        public static RewardItem load(CompoundTag tag) {
            ResourceLocation item = ResourceLocation.parse(tag.getString("Item"));
            IntRange count = IntRange.load(tag.getCompound("Count"));
            return new RewardItem(item, count);
        }
    }

    /**
     * 整数区间记录类
     *
     * <p>用于存储属性偏好范围 (min, max)。
     */
    public record IntRange(int min, int max) {
        /**
         * 检查值是否在区间内
         *
         * @param value 待检查值
         * @return 是否包含
         */
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
