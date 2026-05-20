package com.otherworldinn.network.packet;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.client.ExpeditionTimerClientManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2CExpeditionTimerPacket(long remainingTicks) implements CustomPacketPayload {
    public static final Type<S2CExpeditionTimerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "expedition_timer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CExpeditionTimerPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    S2CExpeditionTimerPacket::remainingTicks,
                    S2CExpeditionTimerPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CExpeditionTimerPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ExpeditionTimerClientManager.syncRemaining(packet.remainingTicks());
        });
    }
}
