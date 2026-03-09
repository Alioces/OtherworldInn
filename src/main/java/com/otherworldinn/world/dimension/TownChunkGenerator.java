package com.otherworldinn.world.dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

/**
 * 城镇区块生成器
 * <p>
 * 生成一个虚空世界，并预留接口生成固定的城镇结构。
 * </p>
 */
public class TownChunkGenerator extends ChunkGenerator {
    // 编解码器
    public static final MapCodec<TownChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(TownChunkGenerator::getBiomeSource)
            ).apply(instance, TownChunkGenerator::new));

    public TownChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // 不生成洞穴
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        // 虚空世界，不生成地表
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // 不自然生成生物
    }

    @Override
    public int getGenDepth() {
        return 384; // 标准高度 -64 到 320
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState state, StructureManager manager,
            ChunkAccess chunk) {
        // 虚空世界，什么都不生成
        return CompletableFuture.completedFuture(chunk);
    }


    @Override
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState chunkGeneratorStructureState, StructureManager structureManager, ChunkAccess chunkAccess, StructureTemplateManager structureTemplateManager) {
        // 不生成任何结构
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 0;
    }


    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        
    }

    @Override
    public NoiseColumn getBaseColumn(int arg0, int arg1, LevelHeightAccessor arg2, RandomState arg3) {
        return new NoiseColumn(0, new BlockState[0]);
    }

}
