package com.otherworldinn.world.event.runtime;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModGameRules;
import com.otherworldinn.world.dimension.TownDimensions;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 维度重置管理器
 *
 * <p>负责每8天（从第9天开始）重置除了城镇维度外的所有其他维度。
 */
@EventBusSubscriber(modid = OtherworldInn.MODID)
public class DimensionResetManager {

    // 8天 = 192000 ticks
    private static final long RESET_CYCLE_TICKS = 192000L;

    private static boolean hasWarned10Min = false;
    private static boolean hasWarned5Min = false;
    private static boolean hasWarned2Min = false;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (!server.getGameRules()
                .getBoolean(ModGameRules.RULE_EXTERNAL_DIMENSION_SCHEDULED_RESET)) {
            hasWarned10Min = false;
            hasWarned5Min = false;
            hasWarned2Min = false;
            return;
        }
        // 使用资源主世界作为时间基准，或者默认主世界
        ServerLevel timeLevel = server.getLevel(Level.OVERWORLD);
        if (timeLevel == null) return;

        long gameTime = timeLevel.getGameTime();

        // 仅在游戏时间大于0时检查
        if (gameTime <= 0) return;

        // 计算当前周期的剩余时间
        long ticksIntoCycle = gameTime % RESET_CYCLE_TICKS;
        long ticksRemaining = RESET_CYCLE_TICKS - ticksIntoCycle;

        // 重置状态标志 (当新的一天开始时)
        // 10分钟 = 12000 ticks, 留一点余量
        if (ticksRemaining > 12005L) {
            hasWarned10Min = false;
            hasWarned5Min = false;
            hasWarned2Min = false;
        }

        // 检查预警
        // 10分钟 = 12000 ticks
        if (!hasWarned10Min && ticksRemaining <= 12000 && ticksRemaining > 11900) {
            broadcastWarning(server, 10);
            hasWarned10Min = true;
        }
        // 5分钟 = 6000 ticks
        if (!hasWarned5Min && ticksRemaining <= 6000 && ticksRemaining > 5900) {
            broadcastWarning(server, 5);
            hasWarned5Min = true;
        }
        // 2分钟 = 2400 ticks
        if (!hasWarned2Min && ticksRemaining <= 2400 && ticksRemaining > 2300) {
            broadcastWarning(server, 2);
            hasWarned2Min = true;
        }

        // 执行重置 (在周期结束的时刻)
        // 使用一个范围检查，防止跳过 tick
        if (ticksRemaining <= 5 || ticksIntoCycle <= 5) {
            // 实际上是在接近 0 的时候触发
            // 为了避免重复触发，我们可以检查 gameTime 是否是 RESET_CYCLE_TICKS 的倍数
            // 或者增加一个简单的冷却/锁定机制，这里简化处理
            if (gameTime % RESET_CYCLE_TICKS == 0
                    || (ticksRemaining == 0 && !hasWarned2Min)) { // 注意：这里逻辑上稍微有点变化，但能保证触发
                performReset(server);
            }
        }
    }

    private static void broadcastWarning(MinecraftServer server, int minutesRemaining) {
        Component message =
                Component.translatable("message.otherworldinn.reset.warning", minutesRemaining)
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void performReset(MinecraftServer server) {
        OtherworldInn.LOGGER.info("Starting scheduled dimension reset...");

        server.getPlayerList()
                .broadcastSystemMessage(
                        Component.translatable("message.otherworldinn.reset.start")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        false);

        // 1. 传送所有非城镇维度的玩家到城镇
        ServerLevel townLevel = server.getLevel(TownDimensions.TOWN_LEVEL);
        if (townLevel != null) {
            BlockPos spawnPos = new BlockPos(10, 71, 0); // 城镇固定出生点

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                // 如果玩家不在城镇维度，将其传送回城镇
                if (player.level().dimension() != TownDimensions.TOWN_LEVEL) {
                    player.teleportTo(
                            townLevel,
                            spawnPos.getX() + 0.5,
                            spawnPos.getY() + 1,
                            spawnPos.getZ() + 0.5,
                            player.getYRot(),
                            player.getXRot());
                    player.displayClientMessage(
                            Component.translatable("message.otherworldinn.reset.teleported")
                                    .withStyle(ChatFormatting.YELLOW),
                            true);
                }
            }
        } else {
            OtherworldInn.LOGGER.error(
                    "Town dimension not found! Aborting reset to prevent data loss.");
            return;
        }

        // 2. 尝试删除非城镇维度的数据文件 (资源主世界, 下界, 末地)
        Path worldDir = server.getWorldPath(LevelResource.ROOT);

        List<ResourceKey<Level>> dimsToReset =
                List.of(TownDimensions.RESOURCE_OVERWORLD_LEVEL, Level.NETHER, Level.END);

        for (ResourceKey<Level> dimKey : dimsToReset) {
            Path dimPath;
            if (dimKey == Level.NETHER) {
                dimPath = worldDir.resolve("DIM-1");
            } else if (dimKey == Level.END) {
                dimPath = worldDir.resolve("DIM1");
            } else {
                // 自定义维度: dimensions/namespace/path
                dimPath =
                        worldDir.resolve("dimensions")
                                .resolve(dimKey.location().getNamespace())
                                .resolve(dimKey.location().getPath());
            }

            // 尝试热重置：强制刷新并关闭 IO
            ServerLevel levelToReset = server.getLevel(dimKey);
            if (levelToReset != null) {
                flushAndCloseIO(levelToReset);

                // 刷新种子
                refreshDimensionSeed(levelToReset);
            }

            // 尝试删除 region, poi, entities 文件夹
            // 注意：如果文件被占用（如维度未卸载），删除可能会失败
            deleteDirectoryContents(dimPath.resolve("region"));
            deleteDirectoryContents(dimPath.resolve("poi"));
            deleteDirectoryContents(dimPath.resolve("entities"));

            OtherworldInn.LOGGER.info(
                    "Attempted to reset dimension files for {}", dimKey.location());
        }

        server.getPlayerList()
                .broadcastSystemMessage(
                        Component.translatable("message.otherworldinn.reset.complete")
                                .withStyle(ChatFormatting.GREEN),
                        false);

        OtherworldInn.LOGGER.info("Dimension reset sequence completed.");
    }

    // 辅助方法：强制刷新并关闭 IO (热重置核心)
    private static void flushAndCloseIO(ServerLevel level) {
        try {
            OtherworldInn.LOGGER.info(
                    "Flushing and closing IO for level {}", level.dimension().location());

            // 1. 保存所有数据
            level.save(null, true, false);

            // 2. 获取 ChunkMap
            ChunkMap chunkMap = level.getChunkSource().chunkMap;

            // 3. 处理区块 IO (ChunkMap -> IOWorker)
            // 反射获取 IOWorker
            // 注意：字段名在不同环境下可能不同，这里假设是开发环境 (Mojmap: worker)
            Field workerField =
                    getField(
                            ChunkMap.class,
                            "worker",
                            "f_140125_"); // f_140125_ 是 SRG 名猜测，实际开发环境用 worker
            if (workerField != null) {
                IOWorker chunkWorker = (IOWorker) workerField.get(chunkMap);
                if (chunkWorker != null) {
                    chunkWorker.synchronize(true).join();
                    closeRegionFileStorage(chunkWorker);
                }
            }

            // 4. 处理 POI IO (PoiManager -> IOWorker)
            PoiManager poiManager = null;
            Field poiManagerField = getField(ChunkMap.class, "poiManager", "f_140129_");
            if (poiManagerField != null) {
                poiManager = (PoiManager) poiManagerField.get(chunkMap);
            }

            if (poiManager != null) {
                // PoiManager extends SectionStorage
                Field sectionWorkerField =
                        getField(
                                net.minecraft.world.level.chunk.storage.SectionStorage.class,
                                "worker",
                                "f_63750_");
                if (sectionWorkerField != null) {
                    IOWorker poiWorker = (IOWorker) sectionWorkerField.get(poiManager);
                    if (poiWorker != null) {
                        poiWorker.synchronize(true).join();
                        closeRegionFileStorage(poiWorker);
                    }
                }
            }

            // 5. 处理实体 IO (暂略，如果需要可以添加)

        } catch (Exception e) {
            OtherworldInn.LOGGER.error(
                    "Failed to flush and close IO for level " + level.dimension().location(), e);
        }
    }

    private static void closeRegionFileStorage(IOWorker worker) {
        try {
            // IOWorker -> RegionFileStorage storage
            Field storageField = getField(IOWorker.class, "storage", "f_63566_");
            if (storageField != null) {
                RegionFileStorage storage = (RegionFileStorage) storageField.get(worker);
                if (storage != null) {
                    // RegionFileStorage -> regionCache
                    Field cacheField = getField(RegionFileStorage.class, "regionCache", "f_63542_");
                    if (cacheField != null) {
                        Map<?, ?> regionCache = (Map<?, ?>) cacheField.get(storage);

                        // 遍历并关闭所有 RegionFile
                        for (Object value : regionCache.values()) {
                            if (value instanceof RegionFile regionFile) {
                                regionFile.close();
                            }
                        }
                        // 清空缓存
                        regionCache.clear();
                        OtherworldInn.LOGGER.info("Closed RegionFileStorage cache.");
                    }
                }
            }
        } catch (Exception e) {
            OtherworldInn.LOGGER.error("Failed to close RegionFileStorage", e);
        }
    }

    private static Field getField(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        // 尝试查找父类
        if (clazz.getSuperclass() != null) {
            return getField(clazz.getSuperclass(), names);
        }
        return null;
    }

    // 辅助方法：刷新维度种子
    private static void refreshDimensionSeed(ServerLevel level) {
        try {
            long newSeed = level.getRandom().nextLong();
            OtherworldInn.LOGGER.info(
                    "Refreshing seed for dimension {} to {}",
                    level.dimension().location(),
                    newSeed);

            ChunkGenerator generator = level.getChunkSource().getGenerator();
            if (generator instanceof NoiseBasedChunkGenerator noiseGenerator) {
                // 对于基于噪声的生成器（如主世界、下界、末地）

                // 1. 获取 NoiseGeneratorSettings
                Field settingsField =
                        getFieldWithFallback(
                                NoiseBasedChunkGenerator.class,
                                Holder.class,
                                "settings",
                                "f_64455_");
                if (settingsField != null) {
                    @SuppressWarnings("unchecked")
                    Holder<NoiseGeneratorSettings> settingsHolder =
                            (Holder<NoiseGeneratorSettings>) settingsField.get(noiseGenerator);

                    // 2. 创建新的 RandomState
                    HolderGetter<NormalNoise.NoiseParameters> noiseParametersGetter =
                            level.registryAccess().lookupOrThrow(Registries.NOISE);
                    RandomState newRandomState =
                            RandomState.create(
                                    settingsHolder.value(), noiseParametersGetter, newSeed);

                    // 3. 替换 ServerChunkCache 中的 RandomState
                    // 这一步对于改变地形生成至关重要
                    // 使用混合查找策略（SRG名 + 类型）
                    Field randomStateField =
                            getFieldWithFallback(
                                    net.minecraft.server.level.ServerChunkCache.class,
                                    RandomState.class,
                                    "randomState",
                                    "f_244243_");
                    if (randomStateField != null) {
                        randomStateField.set(level.getChunkSource(), newRandomState);
                        OtherworldInn.LOGGER.info(
                                "Successfully updated RandomState in ServerChunkCache.");
                    } else {
                        OtherworldInn.LOGGER.warn(
                                "Could not find RandomState field in ServerChunkCache.");
                    }

                    // 4. 关键补充：同时更新 ChunkMap 中的 RandomState 和 Generator
                    // ChunkMap 也持有这些对象的副本，如果不更新，新生成的区块可能仍使用旧配置
                    ChunkMap chunkMap = level.getChunkSource().chunkMap;
                    Field chunkMapRandomStateField =
                            getFieldWithFallback(
                                    ChunkMap.class, RandomState.class, "randomState", "f_243793_");
                    if (chunkMapRandomStateField != null) {
                        chunkMapRandomStateField.set(chunkMap, newRandomState);
                        OtherworldInn.LOGGER.info("Successfully updated RandomState in ChunkMap.");
                    } else {
                        OtherworldInn.LOGGER.warn("Could not find RandomState field in ChunkMap.");
                    }

                    // 5. 尝试替换 ChunkGenerator
                    // 虽然 NoiseBasedChunkGenerator 主要是无状态的，但替换它可以确保没有其他缓存
                    // 并且 RandomState 的改变需要配合 Generator 的刷新（尽管 Generator 可能只持有 Settings）
                    try {
                        BiomeSource newBiomeSource = noiseGenerator.getBiomeSource();
                        // 尝试刷新 BiomeSource (如果支持)
                        // 注意：在 1.21+ 中，生物群系分布主要由 RandomState 控制，BiomeSource 本身可能不存储种子
                        // 但为了保险，如果存在 withSeed 方法，还是调用一下
                        try {
                            Method withSeed =
                                    newBiomeSource.getClass().getMethod("withSeed", long.class);
                            Object result = withSeed.invoke(newBiomeSource, newSeed);
                            if (result instanceof BiomeSource biomeSource) {
                                newBiomeSource = biomeSource;
                            }
                        } catch (NoSuchMethodException e) {
                            // 忽略，说明该 BiomeSource 不支持种子更新
                        }

                        NoiseBasedChunkGenerator newGenerator =
                                new NoiseBasedChunkGenerator(newBiomeSource, settingsHolder);

                        // 更新 ServerChunkCache 中的 Generator
                        Field generatorField =
                                getFieldWithFallback(
                                        net.minecraft.server.level.ServerChunkCache.class,
                                        ChunkGenerator.class,
                                        "generator",
                                        "f_8326_");
                        if (generatorField != null) {
                            generatorField.set(level.getChunkSource(), newGenerator);
                            OtherworldInn.LOGGER.info(
                                    "Successfully replaced ChunkGenerator in ServerChunkCache.");
                        } else {
                            OtherworldInn.LOGGER.warn(
                                    "Could not find ChunkGenerator field in ServerChunkCache.");
                        }

                        // 更新 ChunkMap 中的 Generator
                        Field chunkMapGeneratorField =
                                getFieldWithFallback(
                                        ChunkMap.class,
                                        ChunkGenerator.class,
                                        "generator",
                                        "f_140127_");
                        if (chunkMapGeneratorField != null) {
                            chunkMapGeneratorField.set(chunkMap, newGenerator);
                            OtherworldInn.LOGGER.info(
                                    "Successfully replaced ChunkGenerator in ChunkMap.");
                        } else {
                            OtherworldInn.LOGGER.warn(
                                    "Could not find ChunkGenerator field in ChunkMap.");
                        }

                    } catch (Exception ex) {
                        OtherworldInn.LOGGER.warn(
                                "Failed to replace ChunkGenerator, skipping...", ex);
                    }

                    OtherworldInn.LOGGER.info("Seed refresh logic executed. New seed: " + newSeed);
                } else {
                    OtherworldInn.LOGGER.warn(
                            "Could not find settings field in NoiseBasedChunkGenerator.");
                }
            }
        } catch (Exception e) {
            OtherworldInn.LOGGER.error("Failed to refresh dimension seed", e);
        }
    }

    private static Field getFieldWithFallback(
            Class<?> clazz, Class<?> type, String... possibleNames) {
        // 1. 尝试通过名称查找
        for (String name : possibleNames) {
            try {
                Field field = clazz.getDeclaredField(name);
                // 验证类型是否匹配
                if (type.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }

        // 2. 如果名称查找失败，回退到通过类型查找
        // 这种方式在混淆环境下很有用，但要确保该类型在类中是唯一的或我们找到了正确的那个
        for (Field field : clazz.getDeclaredFields()) {
            if (type.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }

        // 3. 尝试查找父类
        if (clazz.getSuperclass() != null) {
            return getFieldWithFallback(clazz.getSuperclass(), type, possibleNames);
        }
        return null;
    }

    private static Field getFieldByType(Class<?> clazz, Class<?> type) {
        return getFieldWithFallback(clazz, type); // 委托给新方法，不带名称参数
    }

    // 辅助方法：删除目录内容
    private static void deleteDirectoryContents(Path directory) {
        if (!Files.exists(directory)) return;

        // 尝试建议 GC，以释放可能被 MappedByteBuffer 占用的文件句柄 (Windows 特性)
        System.gc();

        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(directory))
                    .map(Path::toFile)
                    .forEach(
                            file -> {
                                if (!file.delete()) {
                                    OtherworldInn.LOGGER.warn(
                                            "Failed to delete file: " + file.getAbsolutePath());
                                }
                            });
        } catch (IOException e) {
            OtherworldInn.LOGGER.error("Failed to delete directory contents: " + directory, e);
        }
    }

    /**
     * 强制执行维度重置（管理员指令用）
     *
     * <p>不影响正常的周期计时，仅立即执行重置操作。
     *
     * @param server 服务器实例
     */
    public static void forceReset(MinecraftServer server) {
        OtherworldInn.LOGGER.info("Forced dimension reset triggered by admin.");
        performReset(server);
    }
}
