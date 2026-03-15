package com.otherworldinn.world.event;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.store.BlacksmithEntity;
import com.otherworldinn.init.ModEntities;
import com.otherworldinn.world.data.TownSavedData;
import com.otherworldinn.world.dimension.TownDimensions;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID)
public class TownStructurePlacer {

    private static final ResourceLocation TOWN_STRUCTURE =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "town_main");
    private static final BlockPos ORIGIN = new BlockPos(0, 70, 0);
    private static final int CENTER_CHUNK_RADIUS = 2;
    private static final BlockPos BLACKSMITH_FALLBACK_POS = new BlockPos(17, 71, 3);

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension() == TownDimensions.TOWN_LEVEL) {
            // 读取城镇生成状态
            TownSavedData data = TownSavedData.get(level);

            // 如果城镇尚未生成，则生成它
            if (!data.isGenerated()) {
                generateTown(level);
                data.setGenerated(true);
            }
            // 常加载中心 5x5 区块，降低关键设施被卸载风险
            ensureCenterChunksAlwaysLoaded(level);
        }
    }

    private static void ensureCenterChunksAlwaysLoaded(ServerLevel level) {
        // 半径 2 个区块 => (2*2+1)^2 = 25 区块
        for (int chunkX = -CENTER_CHUNK_RADIUS; chunkX <= CENTER_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = -CENTER_CHUNK_RADIUS; chunkZ <= CENTER_CHUNK_RADIUS; chunkZ++) {
                level.setChunkForced(chunkX, chunkZ, true);
            }
        }
    }

    private static void generateTown(ServerLevel level) {
        OtherworldInn.LOGGER.info(
                "Generating town structure in dimension: {}", level.dimension().location());

        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> templateOptional = manager.get(TOWN_STRUCTURE);

        if (templateOptional.isPresent()) {
            // 主路径：按模板放置整座城镇
            StructureTemplate template = templateOptional.get();
            StructurePlaceSettings settings =
                    new StructurePlaceSettings()
                            .setRotation(Rotation.NONE)
                            .setMirror(Mirror.NONE)
                            .setIgnoreEntities(false);

            // 直接放置整个结构
            template.placeInWorld(level, ORIGIN, BlockPos.ZERO, settings, level.getRandom(), 2);
            OtherworldInn.LOGGER.info("Town structure placed successfully.");
        } else {
            // 兜底路径：模板缺失时生成简化平台
            OtherworldInn.LOGGER.warn(
                    "Town structure not found: {}. Generating fallback platform.", TOWN_STRUCTURE);
            generateFallbackPlatform(level);
        }
    }

    private static void generateFallbackPlatform(ServerLevel level) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState glowstone = Blocks.GLOWSTONE.defaultBlockState();

        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                pos.set(x, 70, z);
                level.setBlock(pos, stone, 3);

                // 在四个角放置萤石
                if ((x == -8 || x == 8) && (z == -8 || z == 8)) {
                    pos.set(x, 71, z);
                    level.setBlock(pos, glowstone, 3);
                }
            }
        }
    }
}
