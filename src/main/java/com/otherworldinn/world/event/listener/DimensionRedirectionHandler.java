package com.otherworldinn.world.event.listener;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.teleport.TeleportUtils;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/**
 * 维度重定向处理器
 *
 * <p>负责拦截玩家前往原版主世界（minecraft:overworld）的请求，并将其重定向到资源主世界。 这包括通过下界传送门、末地传送门等方式。
 */
@EventBusSubscriber(modid = OtherworldInn.MODID)
public class DimensionRedirectionHandler {

    @SubscribeEvent
    public static void onDimensionTravel(EntityTravelToDimensionEvent event) {
        // 仅处理玩家
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 检查目标维度是否是原版主世界
        if (event.getDimension() == Level.OVERWORLD) {
            // 获取资源主世界
            ServerLevel resourceOverworld =
                    player.getServer().getLevel(TownDimensions.RESOURCE_OVERWORLD_LEVEL);

            // 如果资源主世界存在，则取消原事件并手动执行重定向
            if (resourceOverworld != null) {
                event.setCanceled(true);

                player.getServer()
                        .tell(
                                new TickTask(
                                        player.getServer().getTickCount() + 1,
                                        () -> {
                                            // 构建传送过渡数据
                                            if (player.level().dimension() == Level.NETHER) {
                                                // 坐标转换 (下界 -> 主世界 比例 1:8)
                                                double scale = 8.0;
                                                Vec3 targetPos =
                                                        new Vec3(
                                                                player.getX() * scale,
                                                                player.getY(),
                                                                player.getZ() * scale);

                                                // 使用 PLACE_PORTAL_TICKET 触发传送门搜索/生成
                                                DimensionTransition transition =
                                                        new DimensionTransition(
                                                                resourceOverworld,
                                                                targetPos,
                                                                player.getDeltaMovement(),
                                                                player.getYRot(),
                                                                player.getXRot(),
                                                                DimensionTransition
                                                                        .PLACE_PORTAL_TICKET);
                                                player.changeDimension(transition);
                                            } else if (player.level().dimension() == Level.END) {
                                                // 末地返回：通常是重生点
                                                DimensionTransition transition =
                                                        new DimensionTransition(
                                                                resourceOverworld,
                                                                resourceOverworld
                                                                        .getSharedSpawnPos()
                                                                        .getCenter(),
                                                                Vec3.ZERO,
                                                                0.0f,
                                                                0.0f,
                                                                DimensionTransition
                                                                        .PLAY_PORTAL_SOUND);
                                                player.changeDimension(transition);
                                            } else {
                                                TeleportUtils.changeDimensionTo(
                                                        player,
                                                        resourceOverworld,
                                                        new Vec3(
                                                                player.getX(),
                                                                player.getY(),
                                                                player.getZ()));
                                            }
                                        }));
            }
        }
    }
}
