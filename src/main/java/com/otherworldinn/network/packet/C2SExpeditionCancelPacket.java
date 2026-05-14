package com.otherworldinn.network.packet;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.item.ExpeditionChartItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SExpeditionCancelPacket() implements CustomPacketPayload {
    public static final Type<C2SExpeditionCancelPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "expedition_cancel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SExpeditionCancelPacket> STREAM_CODEC =
            StreamCodec.unit(new C2SExpeditionCancelPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SExpeditionCancelPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof ExpeditionChartItem
                        && "recruiting".equals(ExpeditionChartItem.getChartState(stack))) {
                    ExpeditionChartItem.onLeftClickCancel(player, stack);
                }
            }
        });
    }
}
