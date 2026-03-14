package com.otherworldinn.util;

import com.otherworldinn.world.event.runtime.DisappearManager;
import net.minecraft.world.entity.Entity;

/**
 * 实体工具类
 *
 * <p>提供实体的通用辅助方法。
 */
public class EntityUtils {

    /**
     * 安排实体消失
     *
     * <p>随机等待 3-5 秒 (60-100 ticks)，随后移除自身并发出生物死亡的粒子效果。
     *
     * @param entity 目标实体
     */
    public static void scheduleDisappear(Entity entity) {
        if (entity == null || entity.level().isClientSide) return;

        // 随机延迟 60 - 100 ticks (3 - 5 秒)
        int delay = 60 + entity.level().random.nextInt(41);
        DisappearManager.schedule(entity, delay);
    }
}
