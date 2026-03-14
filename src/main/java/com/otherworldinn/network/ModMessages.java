package com.otherworldinn.network;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.network.packet.C2SStorePurchasePacket;
import com.otherworldinn.network.packet.C2STeleportOverworldPacket;
import com.otherworldinn.network.packet.C2STeleportPacket;
import com.otherworldinn.network.packet.S2CTeamSyncPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络消息注册中心
 *
 * <p>负责注册和发送自定义数据包。
 */
@EventBusSubscriber(modid = OtherworldInn.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModMessages {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // 注册 S2C 数据包
        registrar.playToClient(
                S2CTeamSyncPacket.TYPE, S2CTeamSyncPacket.STREAM_CODEC, S2CTeamSyncPacket::handle);

        // 注册 C2S 数据包
        registrar.playToServer(
                C2STeleportPacket.TYPE, C2STeleportPacket.STREAM_CODEC, C2STeleportPacket::handle);

        registrar.playToServer(
                C2STeleportOverworldPacket.TYPE,
                C2STeleportOverworldPacket.STREAM_CODEC,
                C2STeleportOverworldPacket::handle);

        registrar.playToServer(
                C2SStorePurchasePacket.TYPE,
                C2SStorePurchasePacket.STREAM_CODEC,
                C2SStorePurchasePacket::handle);
    }

    /**
     * 发送数据包给指定玩家 (Server -> Client)
     *
     * @param packet 数据包
     * @param player 目标玩家
     */
    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    /**
     * 发送数据包给服务端 (Client -> Server)
     *
     * @param packet 数据包
     */
    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
