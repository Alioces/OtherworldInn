package com.otherworldinn.network.packet;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dialogue.DialogueService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SDialogueOptionPacket(int entityId, String optionId) implements CustomPacketPayload {
    public static final Type<C2SDialogueOptionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "dialogue_option"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SDialogueOptionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    C2SDialogueOptionPacket::entityId,
                    ByteBufCodecs.STRING_UTF8,
                    C2SDialogueOptionPacket::optionId,
                    C2SDialogueOptionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SDialogueOptionPacket packet, IPayloadContext context) {
        context.enqueueWork(
                () -> {
                    if (context.player() instanceof ServerPlayer player) {
                        DialogueService.selectOption(player, packet.entityId(), packet.optionId());
                    }
                });
    }
}
