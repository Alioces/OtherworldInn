package com.otherworldinn.network.packet;

import com.otherworldinn.OtherworldInn;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 -> 服务端 地图模式同步包。
 *
 * <p>负责在服务端执行地图模式相关的玩家传送与可见状态切换。
 */
@EventBusSubscriber(modid = OtherworldInn.MODID)
public record C2SMapModeSyncPacket(int action, double x, double y, double z, float yaw, float pitch)
        implements CustomPacketPayload {
    private static final String MAP_MODE_HIDDEN_TAG = "otherworldinn.map_mode_hidden";

    public static final int ACTION_ENTER = 0;
    public static final int ACTION_MOVE = 1;
    public static final int ACTION_EXIT = 2;

    public static final Type<C2SMapModeSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "map_mode_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMapModeSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    C2SMapModeSyncPacket::action,
                    ByteBufCodecs.DOUBLE,
                    C2SMapModeSyncPacket::x,
                    ByteBufCodecs.DOUBLE,
                    C2SMapModeSyncPacket::y,
                    ByteBufCodecs.DOUBLE,
                    C2SMapModeSyncPacket::z,
                    ByteBufCodecs.FLOAT,
                    C2SMapModeSyncPacket::yaw,
                    ByteBufCodecs.FLOAT,
                    C2SMapModeSyncPacket::pitch,
                    C2SMapModeSyncPacket::new);

    private static final Map<UUID, PlayerMapModeState> ACTIVE_STATES = new ConcurrentHashMap<>();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SMapModeSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(
                () -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }

                    switch (packet.action()) {
                        case ACTION_ENTER -> enterMapMode(player, packet);
                        case ACTION_MOVE -> moveInMapMode(player, packet);
                        case ACTION_EXIT -> exitMapMode(player);
                        default -> {}
                    }
                });
    }

    private static void enterMapMode(ServerPlayer player, C2SMapModeSyncPacket packet) {
        ACTIVE_STATES.computeIfAbsent(player.getUUID(), id -> captureState(player));
        applyHiddenAppearance(player);
        teleportTo(player, packet);
    }

    private static void moveInMapMode(ServerPlayer player, C2SMapModeSyncPacket packet) {
        if (!ACTIVE_STATES.containsKey(player.getUUID())) {
            ACTIVE_STATES.put(player.getUUID(), captureState(player));
            applyHiddenAppearance(player);
        }
        teleportTo(player, packet);
    }

    private static void exitMapMode(ServerPlayer player) {
        PlayerMapModeState state = ACTIVE_STATES.remove(player.getUUID());
        if (state == null) {
            return;
        }

        restoreAppearance(player, state);
        restorePosition(player, state);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerMapModeState state = ACTIVE_STATES.remove(player.getUUID());
            if (state != null) {
                restoreAppearance(player, state);
                restorePosition(player, state);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerMapModeState state = ACTIVE_STATES.remove(player.getUUID());
            if (state != null) {
                restoreAppearance(player, state);
            }
        }
    }

    private static PlayerMapModeState captureState(ServerPlayer player) {
        return new PlayerMapModeState(
                player.serverLevel().dimension(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                player.isInvisible(),
                player.isInvulnerable(),
                player.getTags().contains(MAP_MODE_HIDDEN_TAG));
    }

    private static void applyHiddenAppearance(ServerPlayer player) {
        player.addTag(MAP_MODE_HIDDEN_TAG);
        player.setInvisible(true);
        player.setInvulnerable(true);
    }

    private static void restoreAppearance(ServerPlayer player, PlayerMapModeState state) {
        if (state.wasMapModeHidden) {
            player.addTag(MAP_MODE_HIDDEN_TAG);
        } else {
            player.removeTag(MAP_MODE_HIDDEN_TAG);
        }
        player.setInvisible(state.wasInvisible);
        player.setInvulnerable(state.wasInvulnerable);
    }

    private static void restorePosition(ServerPlayer player, PlayerMapModeState state) {
        ServerLevel level = player.server.getLevel(state.dimension);
        if (level == null) {
            level = player.serverLevel();
        }

        player.teleportTo(level, state.x, state.y, state.z, state.yaw, state.pitch);
    }

    private static void teleportTo(ServerPlayer player, C2SMapModeSyncPacket packet) {
        player.teleportTo(player.serverLevel(), packet.x(), packet.y(), packet.z(), packet.yaw(), packet.pitch());
    }

    private record PlayerMapModeState(
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            boolean wasInvisible,
            boolean wasInvulnerable,
            boolean wasMapModeHidden) {}
}
