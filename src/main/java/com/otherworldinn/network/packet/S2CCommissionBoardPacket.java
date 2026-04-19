package com.otherworldinn.network.packet;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.client.commission.CommissionClientManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2CCommissionBoardPacket(CompoundTag data) implements CustomPacketPayload {
    public static final Type<S2CCommissionBoardPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "commission_board"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CCommissionBoardPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG,
                    S2CCommissionBoardPacket::data,
                    S2CCommissionBoardPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CCommissionBoardPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> CommissionClientManager.handleBoardData(packet.data()));
    }
}
