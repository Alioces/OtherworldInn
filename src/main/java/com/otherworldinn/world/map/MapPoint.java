package com.otherworldinn.world.map;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * 地图点（POI）数据结构
 *
 * @param id 唯一标识符
 * @param worldPosition 世界坐标
 * @param screenOffset 屏幕显示偏移（相对于世界坐标映射后的屏幕位置），可用于微调图标位置
 * @param iconTexture 图标纹理路径
 * @param displayName 显示名称
 * @param type 地图点类型
 * @param unlockCondition 解锁条件（例如任务ID或特定物品）
 */
public record MapPoint(
        ResourceLocation id,
        Vec3 worldPosition,
        Vec2 screenOffset,
        ResourceLocation iconTexture,
        Component displayName,
        MapPointType type,
        String unlockCondition) {
    public enum MapPointType {
        SHOP, // 商店/功能点
        EXIT_GATE, // 离开城镇的出口
        LANDMARK // 地标（仅展示）
    }
}
