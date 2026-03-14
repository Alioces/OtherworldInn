package com.otherworldinn.world.map;

import com.otherworldinn.OtherworldInn;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * 城镇地图数据提供者
 *
 * <p>负责管理城镇中的所有地图点（POI）。 目前使用静态数据
 */
public class TownDataProvider {

    private static final List<MapPoint> POINTS = new ArrayList<>();

    static {
        // 示例数据：初始化城镇中的几个关键点
        // 注意：这里的世界坐标 (x, y, z) 和屏幕偏移 (x, y) 仅为示例，需要根据实际地图调整

        // 旅社 (Inn)
        POINTS.add(
                new MapPoint(
                        ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "inn"),
                        new Vec3(27, 71, 0),
                        new Vec2(-20, -40),
                        ResourceLocation.fromNamespaceAndPath(
                                OtherworldInn.MODID, "textures/gui/map/icon_inn.png"),
                        Component.translatable("map_point.otherworldinn.inn"),
                        MapPoint.MapPointType.SHOP,
                        null // 默认解锁
                        ));

        // 铁匠铺 (Blacksmith)
        POINTS.add(
                new MapPoint(
                        ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "blacksmith"),
                        new Vec3(17, 71, 3),
                        new Vec2(0, -40),
                        ResourceLocation.fromNamespaceAndPath(
                                OtherworldInn.MODID, "textures/gui/map/icon_blacksmith.png"),
                        Component.translatable("map_point.otherworldinn.blacksmith"),
                        MapPoint.MapPointType.SHOP,
                        null // 默认解锁
                        ));

        // 城镇大门 (Exit Gate)
        POINTS.add(
                new MapPoint(
                        ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "town_gate"),
                        new Vec3(-30, 71, 0),
                        new Vec2(30, 20),
                        ResourceLocation.fromNamespaceAndPath(
                                OtherworldInn.MODID, "textures/gui/map/icon_gate.png"),
                        Component.translatable("map_point.otherworldinn.town_gate"),
                        MapPoint.MapPointType.EXIT_GATE,
                        null // 默认解锁
                        ));
    }

    /**
     * 获取所有地图点
     *
     * @return 地图点列表
     */
    public static List<MapPoint> getPoints() {
        return POINTS;
    }

    /**
     * 根据 ID 获取地图点
     *
     * @param id 地图点 ID
     * @return Optional 地图点
     */
    public static Optional<MapPoint> getPoint(ResourceLocation id) {
        return POINTS.stream().filter(p -> p.id().equals(id)).findFirst();
    }
}
