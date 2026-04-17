package com.otherworldinn.network.packet;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dialogue.DialogueService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SDialogueClosePacket() implements CustomPacketPayload {
    public static final Type<C2SDialogueClosePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "dialogue_close_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SDialogueClosePacket> STREAM_CODEC =
            StreamCodec.unit(new C2SDialogueClosePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SDialogueClosePacket packet, IPayloadContext context) {
        context.enqueueWork(
                () -> {
                    if (context.player() instanceof ServerPlayer player) {
                        DialogueService.closeDialogue(player, false);
                    }
                });
    }
}
