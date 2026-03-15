package com.otherworldinn.util;

import java.util.function.Consumer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public final class BlockEntitySearchUtils {
    private BlockEntitySearchUtils() {}

    public static void forEachInChunkRange(
            Level level,
            int minChunkX,
            int maxChunkX,
            int minChunkZ,
            int maxChunkZ,
            Consumer<BlockEntity> consumer) {
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    consumer.accept(blockEntity);
                }
            }
        }
    }

    public static void forEachInBlockRange(
            Level level,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ,
            Consumer<BlockEntity> consumer) {
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        forEachInChunkRange(
                level,
                minChunkX,
                maxChunkX,
                minChunkZ,
                maxChunkZ,
                blockEntity -> {
                    int x = blockEntity.getBlockPos().getX();
                    int y = blockEntity.getBlockPos().getY();
                    int z = blockEntity.getBlockPos().getZ();
                    if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
                        return;
                    }
                    consumer.accept(blockEntity);
                });
    }
}
