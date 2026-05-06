package com.otherworldinn.world.event;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dimension.TownChunkGenerator;
import com.otherworldinn.world.dimension.TownDimensions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID)
public class TownPresetApplier {
    private static final Path PRESET_ROOT =
            FMLPaths.CONFIGDIR.get().resolve(OtherworldInn.MODID).resolve("town_preset");

    private TownPresetApplier() {}

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Path worldRoot = event.getServer().getWorldPath(LevelResource.ROOT);
        Path townRoot =
                worldRoot.resolve("dimensions")
                        .resolve(TownDimensions.TOWN_LEVEL.location().getNamespace())
                        .resolve(TownDimensions.TOWN_LEVEL.location().getPath());
        Path townRegion = townRoot.resolve("region");
        Path presetRoot = resolvePresetRoot();

        if (hasRegionData(townRegion)) {
            OtherworldInn.LOGGER.info(
                    "Town region data already exists, skip preset apply: {}", townRegion.toAbsolutePath());
            return;
        }
        if (presetRoot == null) {
            OtherworldInn.LOGGER.warn(
                    "Town preset not found. Expected path: {}", PRESET_ROOT.toAbsolutePath());
            return;
        }
        if (!hasRegionData(presetRoot.resolve("region"))) {
            OtherworldInn.LOGGER.error(
                    "Town preset is invalid (missing region .mca files): {}",
                    presetRoot.toAbsolutePath());
            return;
        }

        try {
            copyDirectory(presetRoot, townRoot);
            OtherworldInn.LOGGER.info(
                    "Applied town preset from {} to {}",
                    presetRoot.toAbsolutePath(),
                    townRoot.toAbsolutePath());
        } catch (IOException ex) {
            OtherworldInn.LOGGER.error("Failed to apply town preset.", ex);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        var townLevel = event.getServer().getLevel(TownDimensions.TOWN_LEVEL);
        if (townLevel == null) {
            OtherworldInn.LOGGER.error("Town dimension is not available on server start.");
            return;
        }
        var generator = townLevel.getChunkSource().getGenerator();
        if (!(generator instanceof TownChunkGenerator)) {
            OtherworldInn.LOGGER.error(
                    "Town dimension generator mismatch. Expected {}, but got {}. "
                            + "Existing world may contain stale dimension settings.",
                    TownChunkGenerator.class.getName(),
                    generator.getClass().getName());
        }
    }

    private static Path resolvePresetRoot() {
        // Layout A: config/otherworldinn/town_preset/region|poi|entities
        if (Files.exists(PRESET_ROOT)) {
            if (Files.isDirectory(PRESET_ROOT.resolve("region"))
                    || Files.isDirectory(PRESET_ROOT.resolve("poi"))
                    || Files.isDirectory(PRESET_ROOT.resolve("entities"))) {
                return PRESET_ROOT;
            }
        }
        // Layout B: config/otherworldinn/town_preset/dimensions/otherworldinn/town/...
        Path nested =
                PRESET_ROOT.resolve("dimensions")
                        .resolve(TownDimensions.TOWN_LEVEL.location().getNamespace())
                        .resolve(TownDimensions.TOWN_LEVEL.location().getPath());
        if (Files.exists(nested)) {
            return nested;
        }
        return null;
    }

    private static boolean hasRegionData(Path regionDir) {
        if (!Files.isDirectory(regionDir)) {
            return false;
        }
        try (Stream<Path> stream = Files.list(regionDir)) {
            return stream.anyMatch(path -> path.getFileName().toString().endsWith(".mca"));
        } catch (IOException ex) {
            OtherworldInn.LOGGER.warn("Failed to inspect town region directory: {}", regionDir, ex);
            return false;
        }
    }

    private static void copyDirectory(Path sourceRoot, Path targetRoot) throws IOException {
        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            for (Path source : (Iterable<Path>) stream::iterator) {
                Path relative = sourceRoot.relativize(source);
                Path target = targetRoot.resolve(relative);
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                    continue;
                }
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
