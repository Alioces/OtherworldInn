package com.otherworldinn.world.teleport;

import com.otherworldinn.world.dimension.TownDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * 传送工具类
 *
 * <p>提供与玩家传送相关的通用方法。
 */
public class TeleportUtils {
    private static final int OVERWORLD_RANDOM_RADIUS = 1024;
    private static final int NETHER_RANDOM_RADIUS = 1024;

    /**
     * 将玩家传送到资源主世界出生点。
     *
     * <p>会在维度出生点附近寻找安全的高度位置。
     *
     * @param player 需要传送的玩家
     */
    public static void teleportToOverworldSpawn(ServerPlayer player) {
        ServerLevel targetLevel = player.getServer().getLevel(TownDimensions.RESOURCE_OVERWORLD_LEVEL);
        if (targetLevel == null) {
            targetLevel = player.getServer().getLevel(Level.OVERWORLD);
        }
        if (targetLevel == null) return;

        BlockPos spawnPos = targetLevel.getSharedSpawnPos();
        BlockPos randomBase = randomizeHorizontalBase(targetLevel, spawnPos, OVERWORLD_RANDOM_RADIUS);
        BlockPos safePos = findSafeSpawnPos(targetLevel, randomBase);
        changeDimensionTo(player, targetLevel, safePos);
    }

    public static void changeDimensionTo(ServerPlayer player, ServerLevel targetLevel, BlockPos safePos) {
        DimensionTransition transition = new DimensionTransition(
                targetLevel,
                new Vec3(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5),
                player.getDeltaMovement(),
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.PLACE_PORTAL_TICKET);
        player.changeDimension(transition);
    }

    public static void changeDimensionTo(ServerPlayer player, ServerLevel targetLevel, Vec3 target) {
        DimensionTransition transition = new DimensionTransition(
                targetLevel,
                target,
                player.getDeltaMovement(),
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.PLACE_PORTAL_TICKET);
        player.changeDimension(transition);
    }

    public static BlockPos getRandomizedNetherBase(ServerLevel nether, BlockPos convertedBase) {
        return randomizeHorizontalBase(nether, convertedBase, NETHER_RANDOM_RADIUS);
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
        BlockPos fallback = findSafeYAround(level, baseX, fallbackY, baseZ, 16);
        if (fallback != null) {
            return fallback;
        }
        return new BlockPos(baseX, fallbackY, baseZ);
    }

    public static BlockPos findSafeSpawnPosInNether(ServerLevel level, BlockPos basePos) {
        int baseX = basePos.getX();
        int baseZ = basePos.getZ();
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        int roofLimitY = 120;

        for (int r = 0; r <= 8; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int x = baseX + dx * 4;
                    int z = baseZ + dz * 4;
                    ensureChunk(level, x, z);
                    for (int y = minY; y <= roofLimitY; y++) {
                        BlockPos candidate = new BlockPos(x, y, z);
                        if (isSafeSpawn(level, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        ensureChunk(level, baseX, baseZ);
        int y = Math.min(level.getSeaLevel() + 1, roofLimitY);
        int fallbackStartY = Math.max(y, minY);
        BlockPos fallback = findSafeYAround(level, baseX, fallbackStartY, baseZ, 64);
        if (fallback != null) {
            return fallback;
        }
        return new BlockPos(baseX, fallbackStartY, baseZ);
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

    private static BlockPos findSafeYAround(ServerLevel level, int x, int baseY, int z, int searchRange) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        for (int dy = 0; dy <= searchRange; dy++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                int y = baseY + dy * sign;
                if (y < minY || y > maxY) {
                    continue;
                }
                BlockPos candidate = new BlockPos(x, y, z);
                if (isSafeSpawn(level, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static void ensureChunk(ServerLevel level, int x, int z) {
        int chunkX = SectionPos.blockToSectionCoord(x);
        int chunkZ = SectionPos.blockToSectionCoord(z);
        level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
    }

    private static BlockPos randomizeHorizontalBase(ServerLevel level, BlockPos basePos, int radius) {
        if (radius <= 0) {
            return basePos;
        }
        RandomSource random = level.getRandom();
        int offsetX = random.nextInt(radius * 2 + 1) - radius;
        int offsetZ = random.nextInt(radius * 2 + 1) - radius;
        return basePos.offset(offsetX, 0, offsetZ);
    }
}
