package com.otherworldinn.network.packet;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.teleport.TeleportUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 -> 服务端 数据包
 *
 * <p>请求传送到主世界出生点。
 */
public record C2STeleportOverworldPacket() implements CustomPacketPayload {

    public static final Type<C2STeleportOverworldPacket> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            OtherworldInn.MODID, "teleport_overworld"));

    // 编解码器
    public static final StreamCodec<RegistryFriendlyByteBuf, C2STeleportOverworldPacket>
            STREAM_CODEC = StreamCodec.unit(new C2STeleportOverworldPacket());

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
                        TeleportUtils.teleportToOverworldSpawn(player);
                    }
                });
    }
}
