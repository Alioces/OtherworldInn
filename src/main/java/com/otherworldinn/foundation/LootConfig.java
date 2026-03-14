package com.otherworldinn.foundation;

import java.util.ArrayList;
import java.util.List;

/**
 * 战利品表配置
 *
 * @param type 战利品类型 (DROP_SELF, DROP_NOTHING, CUSTOM)
 * @param entries 战利品条目列表 (仅当类型为 CUSTOM 时使用)
 * @param silkTouchDropSelf 是否在精准采集时掉落方块自身
 */
public record LootConfig(LootType type, List<LootEntry> entries, boolean silkTouchDropSelf) {
    public static final LootConfig DEFAULT =
            new LootConfig(LootType.DROP_SELF, new ArrayList<>(), false);
    public static final LootConfig EMPTY =
            new LootConfig(LootType.DROP_NOTHING, new ArrayList<>(), false);

    public enum LootType {
        DROP_SELF,
        DROP_NOTHING,
        CUSTOM
    }

    /**
     * 单个战利品条目
     *
     * @param itemId 物品ID (namespace:id)
     * @param chance 掉落几率 (0.0 - 1.0)
     * @param minCount 最小数量
     * @param maxCount 最大数量
     * @param requiresSilkTouch 是否需要精准采集 (通常不需要设置，除非有特殊需求)
     */
    public record LootEntry(
            String itemId, float chance, int minCount, int maxCount, boolean requiresSilkTouch) {}
}
