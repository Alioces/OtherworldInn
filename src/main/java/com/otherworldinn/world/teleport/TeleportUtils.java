package com.otherworldinn.world.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 传送工具类
 * <p>
 * 提供与玩家传送相关的通用方法。
 */
public class TeleportUtils {

    /**
     * 将玩家传送到主世界出生点。
     * <p>
     * 会在主世界出生点附近寻找安全的高度位置。
     *
     * @param player 需要传送的玩家
     */
    public static void teleportToOverworldSpawn(ServerPlayer player) {
        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        BlockPos spawnPos = overworld.getSharedSpawnPos();
        
        // 寻找安全的 Y 轴高度
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, spawnPos.getX(), spawnPos.getZ());
        
        player.teleportTo(overworld, spawnPos.getX() + 0.5, y + 1, spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
    }
}
