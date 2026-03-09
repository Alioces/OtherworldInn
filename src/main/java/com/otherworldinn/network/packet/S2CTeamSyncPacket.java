package com.otherworldinn.network.packet;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.UUID;

/**
 * 服务端 -> 客户端
 * 同步队伍数据
 */
public record S2CTeamSyncPacket(
        UUID teamId,
        String teamName,
        UUID leaderId,
        List<UUID> members,
        List<ResourceLocation> unlockedPoints,
        boolean teleportUnlocked
) implements CustomPacketPayload {

    public static final Type<S2CTeamSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "team_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CTeamSyncPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, S2CTeamSyncPacket::teamId,
            ByteBufCodecs.STRING_UTF8, S2CTeamSyncPacket::teamName,
            UUIDUtil.STREAM_CODEC, S2CTeamSyncPacket::leaderId,
            ByteBufCodecs.collection(ArrayList::new, UUIDUtil.STREAM_CODEC), S2CTeamSyncPacket::members,
            ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC), S2CTeamSyncPacket::unlockedPoints,
            ByteBufCodecs.BOOL, S2CTeamSyncPacket::teleportUnlocked,
            S2CTeamSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // 在客户端主线程执行
            Set<UUID> memberSet = new HashSet<>(members());
            Set<ResourceLocation> pointSet = new HashSet<>(unlockedPoints());
            TeamManager.getInstance().updateClientTeamData(
                    teamId(),
                    teamName(),
                    leaderId(),
                    memberSet,
                    pointSet,
                    teleportUnlocked()
            );
        });
    }
}
