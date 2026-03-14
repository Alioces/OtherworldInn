package com.otherworldinn.network.packet;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.map.MapPoint;
import com.otherworldinn.world.map.TownDataProvider;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 -> 服务端 数据包
 *
 * <p>请求传送到指定的地图点。 包含目标点的 ResourceLocation ID。
 */
public record C2STeleportPacket(ResourceLocation pointId) implements CustomPacketPayload {

    public static final Type<C2STeleportPacket> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "teleport_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2STeleportPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    C2STeleportPacket::pointId,
                    C2STeleportPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 处理数据包
     *
     * @param context 数据包上下文
     */
    public void handle(IPayloadContext context) {
        context.enqueueWork(
                () -> {
                    if (context.player() instanceof ServerPlayer player) {
                        // 验证玩家队伍信息
                        TeamData team = TeamManager.getInstance().getPlayerTeam(player);

                        // 验证地图点是否解锁
                        if (!team.isMapPointUnlocked(pointId)) {
                            return;
                        }

                        // 执行传送
                        Optional<MapPoint> pointOpt = TownDataProvider.getPoint(pointId);
                        if (pointOpt.isPresent()) {
                            Vec3 target = pointOpt.get().worldPosition();
                            // 传送到目标位置
                            player.teleportTo(
                                    player.serverLevel(),
                                    target.x,
                                    target.y,
                                    target.z,
                                    player.getYRot(),
                                    player.getXRot());

                            // 播放末影人传送音效
                            player.serverLevel()
                                    .playSound(
                                            null,
                                            player.getX(),
                                            player.getY(),
                                            player.getZ(),
                                            SoundEvents.ENDERMAN_TELEPORT,
                                            SoundSource.PLAYERS,
                                            8.0F,
                                            1.0F);

                            // 生成传送粒子效果
                            player.serverLevel()
                                    .sendParticles(
                                            ParticleTypes.PORTAL,
                                            player.getX(),
                                            player.getY() + 1.0,
                                            player.getZ(),
                                            32,
                                            0.5,
                                            1.0,
                                            0.5,
                                            0.1);
                        }
                    }
                });
    }
}
