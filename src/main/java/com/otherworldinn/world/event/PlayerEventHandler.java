package com.otherworldinn.world.event;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.getServer();
            
            // 确保玩家加入队伍
            if (server != null) {
                TeamManager.getInstance().onPlayerJoin(player, server);
            }
            

            if (!player.getTags().contains("otherworldinn.joined")) {
                ServerLevel townLevel = player.getServer().getLevel(TownDimensions.TOWN_LEVEL);
                if (townLevel != null) {
                    BlockPos spawnPos = new BlockPos(0, 70, 0);
                    player.teleportTo(townLevel, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                    player.setRespawnPosition(TownDimensions.TOWN_LEVEL, spawnPos, 0, true, false);
                    player.addTag("otherworldinn.joined");
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // If player has no respawn point, send to town spawn?
        // Default behavior might send them to Overworld spawn if bed is missing.
        // We set respawn position on first join, so it should be fine.
        // But if they lose it, we might want to enforce it.
        if (event.getEntity() instanceof ServerPlayer player && !event.isEndConquered()) {
             if (player.getRespawnPosition() == null) {
                 ServerLevel townLevel = player.getServer().getLevel(TownDimensions.TOWN_LEVEL);
                 if (townLevel != null) {
                     BlockPos spawnPos = new BlockPos(0, 70, 0);
                     player.teleportTo(townLevel, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                 }
             }
        }
    }
}
