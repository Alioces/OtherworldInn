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
import net.minecraft.world.entity.Entity;
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

@EventBusSubscriber(modid = OtherworldInn.MODID)
public class DimensionResetManager {

    private static final long RESET_CYCLE_TICKS = 192000L;
    private static final double EXTERNAL_DIM_BORDER_SIZE = 20480.0D;

    private static boolean hasWarned10Min = false;
    private static boolean hasWarned5Min = false;
    private static boolean hasWarned2Min = false;
    private static boolean worldBordersApplied = false;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (!server.getGameRules().getBoolean(ModGameRules.RULE_EXTERNAL_DIMENSION_SCHEDULED_RESET)) {
            hasWarned10Min = false;
            hasWarned5Min = false;
            hasWarned2Min = false;
            worldBordersApplied = false;
            return;
        }

        if (!worldBordersApplied) {
            applyWorldBorders(server);
            worldBordersApplied = true;
        }

        ServerLevel timeLevel = server.getLevel(Level.OVERWORLD);
        if (timeLevel == null) return;

        long gameTime = timeLevel.getGameTime();
        if (gameTime <= 0) return;

        long ticksIntoCycle = gameTime % RESET_CYCLE_TICKS;
        long ticksRemaining = RESET_CYCLE_TICKS - ticksIntoCycle;

        if (ticksRemaining > 12005L) {
            hasWarned10Min = false;
            hasWarned5Min = false;
            hasWarned2Min = false;
        }

        if (!hasWarned10Min && ticksRemaining <= 12000 && ticksRemaining > 11900) {
            broadcastWarning(server, 10);
            hasWarned10Min = true;
        }
        if (!hasWarned5Min && ticksRemaining <= 6000 && ticksRemaining > 5900) {
            broadcastWarning(server, 5);
            hasWarned5Min = true;
        }
        if (!hasWarned2Min && ticksRemaining <= 2400 && ticksRemaining > 2300) {
            broadcastWarning(server, 2);
            hasWarned2Min = true;
        }

        if (ticksRemaining <= 5 || ticksIntoCycle <= 5) {
            if (gameTime % RESET_CYCLE_TICKS == 0 || (ticksRemaining == 0 && !hasWarned2Min)) {
                performReset(server);
            }
        }
    }

    private static void broadcastWarning(MinecraftServer server, int minutesRemaining) {
        Component message = Component.translatable("message.otherworldinn.reset.warning", minutesRemaining)
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void performReset(MinecraftServer server) {
        OtherworldInn.LOGGER.info("Starting scheduled dimension reset...");

        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.otherworldinn.reset.start")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                false);

        ServerLevel townLevel = server.getLevel(TownDimensions.TOWN_LEVEL);
        if (townLevel == null) {
            OtherworldInn.LOGGER.error("Town dimension not found! Aborting reset to prevent data loss.");
            return;
        }

        BlockPos spawnPos = new BlockPos(10, 71, 0);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension() != TownDimensions.TOWN_LEVEL) {
                player.teleportTo(townLevel,
                        spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
                player.displayClientMessage(
                        Component.translatable("message.otherworldinn.reset.teleported")
                                .withStyle(ChatFormatting.YELLOW),
                        true);
            }
        }

        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        List<ResourceKey<Level>> dimsToReset = List.of(
                TownDimensions.RESOURCE_OVERWORLD_LEVEL, Level.NETHER, Level.END);

        for (ResourceKey<Level> dimKey : dimsToReset) {
            Path dimPath;
            if (dimKey == Level.NETHER) {
                dimPath = worldDir.resolve("DIM-1");
            } else if (dimKey == Level.END) {
                dimPath = worldDir.resolve("DIM1");
            } else {
                dimPath = worldDir.resolve("dimensions")
                        .resolve(dimKey.location().getNamespace())
                        .resolve(dimKey.location().getPath());
            }

            ServerLevel levelToReset = server.getLevel(dimKey);
            if (levelToReset != null) {
                discardAllEntities(levelToReset);
                flushAndCloseIO(levelToReset);
                unloadAllChunks(levelToReset);
                refreshDimensionSeed(levelToReset);
            }

            deleteDirectoryContents(dimPath.resolve("region"));
            deleteDirectoryContents(dimPath.resolve("poi"));
            deleteDirectoryContents(dimPath.resolve("entities"));
        }

        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.otherworldinn.reset.complete")
                        .withStyle(ChatFormatting.GREEN),
                false);

        applyWorldBorders(server);

        OtherworldInn.LOGGER.info("Dimension reset sequence completed.");
    }

    private static void flushAndCloseIO(ServerLevel level) {
        try {
            level.save(null, true, false);
            ChunkMap chunkMap = level.getChunkSource().chunkMap;

            Field workerField = getField(ChunkMap.class, "worker", "f_140125_");
            if (workerField != null) {
                IOWorker chunkWorker = (IOWorker) workerField.get(chunkMap);
                if (chunkWorker != null) {
                    chunkWorker.synchronize(true).join();
                    closeRegionFileStorage(chunkWorker);
                }
            }

            Field poiManagerField = getField(ChunkMap.class, "poiManager", "f_140129_");
            if (poiManagerField != null) {
                PoiManager poiManager = (PoiManager) poiManagerField.get(chunkMap);
                if (poiManager != null) {
                    Field sectionWorkerField = getField(
                            net.minecraft.world.level.chunk.storage.SectionStorage.class,
                            "worker", "f_63750_");
                    if (sectionWorkerField != null) {
                        IOWorker poiWorker = (IOWorker) sectionWorkerField.get(poiManager);
                        if (poiWorker != null) {
                            poiWorker.synchronize(true).join();
                            closeRegionFileStorage(poiWorker);
                        }
                    }
                }
            }
        } catch (Exception e) {
            OtherworldInn.LOGGER.error("Failed to flush and close IO for " + level.dimension().location(), e);
        }
    }

    private static void closeRegionFileStorage(IOWorker worker) {
        try {
            Field storageField = getField(IOWorker.class, "storage", "f_63566_");
            if (storageField == null) return;

            RegionFileStorage storage = (RegionFileStorage) storageField.get(worker);
            if (storage == null) return;

            Field cacheField = getField(RegionFileStorage.class, "regionCache", "f_63542_");
            if (cacheField == null) return;

            Map<?, ?> regionCache = (Map<?, ?>) cacheField.get(storage);
            for (Object value : regionCache.values()) {
                if (value instanceof RegionFile regionFile) {
                    regionFile.close();
                }
            }
            regionCache.clear();
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
        if (clazz.getSuperclass() != null) {
            return getField(clazz.getSuperclass(), names);
        }
        return null;
    }

    private static void refreshDimensionSeed(ServerLevel level) {
        try {
            long newSeed = level.getRandom().nextLong();
            ChunkGenerator generator = level.getChunkSource().getGenerator();
            if (!(generator instanceof NoiseBasedChunkGenerator noiseGenerator)) return;

            Field settingsField = getFieldWithFallback(
                    NoiseBasedChunkGenerator.class, Holder.class, "settings", "f_64455_");
            if (settingsField == null) return;

            @SuppressWarnings("unchecked")
            Holder<NoiseGeneratorSettings> settingsHolder =
                    (Holder<NoiseGeneratorSettings>) settingsField.get(noiseGenerator);

            HolderGetter<NormalNoise.NoiseParameters> noiseParametersGetter =
                    level.registryAccess().lookupOrThrow(Registries.NOISE);
            RandomState newRandomState =
                    RandomState.create(settingsHolder.value(), noiseParametersGetter, newSeed);

            Field randomStateField = getFieldWithFallback(
                    net.minecraft.server.level.ServerChunkCache.class,
                    RandomState.class, "randomState", "f_244243_");
            if (randomStateField != null) {
                randomStateField.set(level.getChunkSource(), newRandomState);
            }

            ChunkMap chunkMap = level.getChunkSource().chunkMap;
            Field chunkMapRandomStateField = getFieldWithFallback(
                    ChunkMap.class, RandomState.class, "randomState", "f_243793_");
            if (chunkMapRandomStateField != null) {
                chunkMapRandomStateField.set(chunkMap, newRandomState);
            }

            try {
                BiomeSource newBiomeSource = noiseGenerator.getBiomeSource();
                try {
                    Method withSeed = newBiomeSource.getClass().getMethod("withSeed", long.class);
                    Object result = withSeed.invoke(newBiomeSource, newSeed);
                    if (result instanceof BiomeSource biomeSource) {
                        newBiomeSource = biomeSource;
                    }
                } catch (NoSuchMethodException ignored) {
                }

                NoiseBasedChunkGenerator newGenerator = new NoiseBasedChunkGenerator(newBiomeSource, settingsHolder);

                Field generatorField = getFieldWithFallback(
                        net.minecraft.server.level.ServerChunkCache.class,
                        ChunkGenerator.class, "generator", "f_8326_");
                if (generatorField != null) {
                    generatorField.set(level.getChunkSource(), newGenerator);
                }

                Field chunkMapGeneratorField = getFieldWithFallback(
                        ChunkMap.class, ChunkGenerator.class, "generator", "f_140127_");
                if (chunkMapGeneratorField != null) {
                    chunkMapGeneratorField.set(chunkMap, newGenerator);
                }
            } catch (Exception ex) {
                OtherworldInn.LOGGER.warn("Failed to replace ChunkGenerator, skipping...", ex);
            }
        } catch (Exception e) {
            OtherworldInn.LOGGER.error("Failed to refresh dimension seed", e);
        }
    }

    private static Field getFieldWithFallback(Class<?> clazz, Class<?> type, String... possibleNames) {
        for (String name : possibleNames) {
            try {
                Field field = clazz.getDeclaredField(name);
                if (type.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (type.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }

        if (clazz.getSuperclass() != null) {
            return getFieldWithFallback(clazz.getSuperclass(), type, possibleNames);
        }
        return null;
    }

    private static Field getFieldByType(Class<?> clazz, Class<?> type) {
        return getFieldWithFallback(clazz, type);
    }

    private static void applyWorldBorders(MinecraftServer server) {
        for (ResourceKey<Level> dimKey : List.of(
                TownDimensions.RESOURCE_OVERWORLD_LEVEL, Level.NETHER, Level.END)) {
            ServerLevel dim = server.getLevel(dimKey);
            if (dim != null) {
                dim.getWorldBorder().setSize(EXTERNAL_DIM_BORDER_SIZE);
                dim.getWorldBorder().setCenter(0.0, 0.0);
            }
        }
    }

    private static void discardAllEntities(ServerLevel level) {
        try {
            int count = 0;
            for (Entity entity : level.getEntities().getAll()) {
                if (!(entity instanceof ServerPlayer)) {
                    entity.discard();
                    count++;
                }
            }
            OtherworldInn.LOGGER.info("Discarded {} non-player entities in {}", count, level.dimension().location());
        } catch (Exception e) {
            OtherworldInn.LOGGER.error("Failed to discard entities in " + level.dimension().location(), e);
        }
    }

    private static void unloadAllChunks(ServerLevel level) {
        try {
            ChunkMap chunkMap = level.getChunkSource().chunkMap;

            Field updChunksField = getFieldWithFallback(ChunkMap.class, Object.class, "pendingUnloads", "f_140127_");
            if (updChunksField == null) {
                updChunksField = getFieldByType(ChunkMap.class, java.util.Queue.class);
            }
            if (updChunksField != null) {
                Object pending = updChunksField.get(chunkMap);
                if (pending instanceof java.util.Queue) {
                    ((java.util.Queue<?>) pending).clear();
                }
            }

            Field visibleChunkMapField = getFieldWithFallback(ChunkMap.class, Map.class, "visibleChunkMap", "f_140128_");
            if (visibleChunkMapField == null) {
                visibleChunkMapField = getFieldByType(ChunkMap.class, Map.class);
            }
            if (visibleChunkMapField != null) {
                @SuppressWarnings("unchecked")
                Map<?, ?> visibleMap = (Map<?, ?>) visibleChunkMapField.get(chunkMap);
                if (visibleMap != null) {
                    visibleMap.clear();
                }
            }

            Field updatingChunkMapField = getFieldWithFallback(ChunkMap.class, Map.class, "updatingChunkMap", null);
            if (updatingChunkMapField == null) {
                for (Field f : ChunkMap.class.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(f.getType())) {
                        String name = f.getName();
                        if (name.contains("updating") || name.contains("Update")) {
                            updatingChunkMapField = f;
                            updatingChunkMapField.setAccessible(true);
                            break;
                        }
                    }
                }
            }
            if (updatingChunkMapField != null) {
                @SuppressWarnings("unchecked")
                Map<?, ?> updatingMap = (Map<?, ?>) updatingChunkMapField.get(chunkMap);
                if (updatingMap != null) {
                    updatingMap.clear();
                }
            }
        } catch (Exception e) {
            OtherworldInn.LOGGER.error("Failed to unload chunks in " + level.dimension().location(), e);
        }
    }

    private static void deleteDirectoryContents(Path directory) {
        if (!Files.exists(directory)) return;

        System.gc();

        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(directory))
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (!file.delete()) {
                            OtherworldInn.LOGGER.warn("Failed to delete file: " + file.getAbsolutePath());
                        }
                    });
        } catch (IOException e) {
            OtherworldInn.LOGGER.error("Failed to delete directory contents: " + directory, e);
        }
    }

    public static void forceReset(MinecraftServer server) {
        OtherworldInn.LOGGER.info("Forced dimension reset triggered by admin.");
        performReset(server);
    }
}
