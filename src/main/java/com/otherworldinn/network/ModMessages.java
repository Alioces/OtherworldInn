package com.otherworldinn.network;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.network.packet.C2STeleportPacket;
import com.otherworldinn.network.packet.S2CTeamSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = OtherworldInn.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModMessages {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        
        registrar.playToClient(
                S2CTeamSyncPacket.TYPE,
                S2CTeamSyncPacket.STREAM_CODEC,
                S2CTeamSyncPacket::handle
        );
        
        registrar.playToServer(
                C2STeleportPacket.TYPE,
                C2STeleportPacket.STREAM_CODEC,
                C2STeleportPacket::handle
        );
    }

    public static void sendToPlayer(S2CTeamSyncPacket packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
    
    public static void sendToServer(C2STeleportPacket packet) {
        PacketDistributor.sendToServer(packet);
    }
}
