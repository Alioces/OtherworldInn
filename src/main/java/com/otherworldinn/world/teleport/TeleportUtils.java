package com.otherworldinn.world.teleport;

import com.otherworldinn.world.dimension.TownDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 传送工具类
 *
 * <p>提供与玩家传送相关的通用方法。
 */
public class TeleportUtils {

    /**
     * 将玩家传送到资源主世界出生点。
     *
     * <p>会在维度出生点附近寻找安全的高度位置。
     *
     * @param player 需要传送的玩家
     */
    public static void teleportToOverworldSpawn(ServerPlayer player) {
        // 优先尝试传送到资源主世界
        ServerLevel targetLevel =
                player.getServer().getLevel(TownDimensions.RESOURCE_OVERWORLD_LEVEL);

        // 如果资源主世界不存在（例如未正确注册），则回退到原版主世界
        if (targetLevel == null) {
            targetLevel = player.getServer().getLevel(Level.OVERWORLD);
        }

        if (targetLevel == null) return;

        BlockPos spawnPos = targetLevel.getSharedSpawnPos();
        BlockPos safePos = findSafeSpawnPos(targetLevel, spawnPos);
        player.teleportTo(
                targetLevel,
                safePos.getX() + 0.5,
                safePos.getY(),
                safePos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot());
    }

    public static BlockPos findSafeSpawnPos(ServerLevel level, BlockPos basePos) {
        int baseX = basePos.getX();
        int baseZ = basePos.getZ();

        for (int r = 0; r <= 8; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int x = baseX + dx * 4;
                    int z = baseZ + dz * 4;
                    ensureChunk(level, x, z);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos base = new BlockPos(x, y, z);
                    for (int dy = 0; dy <= 2; dy++) {
                        BlockPos candidate = base.above(dy);
                        if (isSafeSpawn(level, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        ensureChunk(level, baseX, baseZ);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseX, baseZ);
        int fallbackY = Math.max(y, level.getSeaLevel() + 1);
        return new BlockPos(baseX, fallbackY, baseZ);
    }

    private static boolean isSafeSpawn(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.isAir() || !below.getFluidState().isEmpty()) {
            return false;
        }

        BlockState body = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        if (!body.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) {
            return false;
        }
        if (!body.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        return head.getCollisionShape(level, pos.above()).isEmpty();
    }

    private static void ensureChunk(ServerLevel level, int x, int z) {
        int chunkX = SectionPos.blockToSectionCoord(x);
        int chunkZ = SectionPos.blockToSectionCoord(z);
        level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
    }
}
