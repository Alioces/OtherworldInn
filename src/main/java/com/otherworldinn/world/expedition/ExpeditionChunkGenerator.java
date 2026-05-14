package com.otherworldinn.world.expedition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

public class ExpeditionChunkGenerator extends ChunkGenerator {

    public static final MapCodec<ExpeditionChunkGenerator> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source")
                            .forGetter(ExpeditionChunkGenerator::getBiomeSource),
                    Codec.STRING.listOf().optionalFieldOf("components", List.of())
                            .forGetter(g -> g.componentIds))
                    .apply(instance, ExpeditionChunkGenerator::new));

    private final List<String> componentIds = new java.util.ArrayList<>();
    private NoiseBasedChunkGenerator delegate;
    private boolean structureBoost;
    private BlockState stoneReplacement = Blocks.STONE.defaultBlockState();
    private boolean lavaFlood;
    private boolean dryLand;
    private boolean waterWorld;

    private static final Set<Block> REPLACEABLE_STONES = Set.of(
            Blocks.STONE, Blocks.DEEPSLATE,
            Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE,
            Blocks.TUFF, Blocks.SANDSTONE);

    public ExpeditionChunkGenerator(BiomeSource biomeSource, List<String> componentIds) {
        super(biomeSource);
        this.componentIds.addAll(componentIds);
    }

    public List<String> componentIds() { return componentIds; }

    public void setComponentIds(List<String> ids) {
        componentIds.clear();
        if (ids != null) componentIds.addAll(ids);
    }

    public void setDelegate(NoiseBasedChunkGenerator delegate) {
        this.delegate = delegate;
    }

    public NoiseBasedChunkGenerator getDelegate() { return delegate; }

    public void setStructureBoost(boolean structureBoost) {
        this.structureBoost = structureBoost;
    }

    public boolean isStructureBoost() { return structureBoost; }

    public void setStoneReplacement(BlockState stone) { this.stoneReplacement = stone; }

    public BlockState stoneReplacement() { return stoneReplacement; }

    public void setLavaFlood(boolean lavaFlood) { this.lavaFlood = lavaFlood; }

    public boolean isLavaFlood() { return lavaFlood; }

    public void setDryLand(boolean dryLand) { this.dryLand = dryLand; }

    public void setWaterWorld(boolean waterWorld) { this.waterWorld = waterWorld; }

    @Override
    public @NotNull BiomeSource getBiomeSource() {
        if (delegate != null) return delegate.getBiomeSource();
        return super.getBiomeSource();
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(
            @NotNull RandomState state, @NotNull Blender blender,
            @NotNull StructureManager structureManager, @NotNull ChunkAccess chunk) {
        if (delegate != null)
            return delegate.createBiomes(state, blender, structureManager, chunk);
        return super.createBiomes(state, blender, structureManager, chunk);
    }

    @Override
    public ChunkGeneratorStructureState createState(
            HolderLookup<StructureSet> structureSets,
            RandomState randomState,
            long seed) {
        if (structureBoost) {
            ExpeditionService.setStructureBoostActive(true);
        }
        if (delegate != null) {
            return delegate.createState(structureSets, randomState, seed);
        }
        return super.createState(structureSets, randomState, seed);
    }

    @Override
    protected @NotNull MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(@NotNull WorldGenRegion region, long seed,
            @NotNull RandomState random, @NotNull BiomeManager biomeManager,
            @NotNull StructureManager structureManager, @NotNull ChunkAccess chunk,
            GenerationStep.@NotNull Carving step) {
        if (delegate != null)
            delegate.applyCarvers(region, seed, random, biomeManager,
                    structureManager, chunk, step);
    }

    @Override
    public void buildSurface(@NotNull WorldGenRegion region,
            @NotNull StructureManager structureManager, @NotNull RandomState random,
            @NotNull ChunkAccess chunk) {
        if (delegate != null)
            delegate.buildSurface(region, structureManager, random, chunk);
    }

    @Override
    public void spawnOriginalMobs(@NotNull WorldGenRegion region) {
        if (delegate != null) delegate.spawnOriginalMobs(region);
    }

    @Override
    public int getGenDepth() {
        return delegate != null ? delegate.getGenDepth() : 384;
    }

    @Override
    public @NotNull CompletableFuture<ChunkAccess> fillFromNoise(@NotNull Blender blender,
            @NotNull RandomState state, @NotNull StructureManager manager,
            @NotNull ChunkAccess chunk) {
        if (delegate != null) {
            boolean needsStone = stoneReplacement.getBlock() != Blocks.STONE;
            if (needsStone || lavaFlood) {
                return delegate.fillFromNoise(blender, state, manager, chunk)
                        .thenApply(c -> postProcess(c));
            }
            return delegate.fillFromNoise(blender, state, manager, chunk);
        }
        return CompletableFuture.completedFuture(chunk);
    }

    private ChunkAccess postProcess(ChunkAccess chunk) {
        int min = chunk.getMinSection();
        int max = chunk.getMaxSection();
        for (int i = min; i < max; i++) {
            LevelChunkSection section = chunk.getSection(
                    chunk.getSectionIndexFromSectionY(i));
            if (section.hasOnlyAir()) continue;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState bs = section.getBlockState(x, y, z);
                        int wy = chunk.getSectionYFromSectionIndex(i) * 16 + y;

                        if (stoneReplacement.getBlock() != Blocks.STONE
                                && REPLACEABLE_STONES.contains(bs.getBlock())) {
                            section.setBlockState(x, y, z, stoneReplacement);
                        }

                        if (lavaFlood && bs.getFluidState().is(Fluids.WATER)) {
                            if (wy < 63) {
                                section.setBlockState(x, y, z,
                                        Blocks.LAVA.defaultBlockState());
                            }
                        }
                    }
                }
            }
        }
        return chunk;
    }

    @Override
    public void createStructures(@NotNull RegistryAccess registryAccess,
            @NotNull ChunkGeneratorStructureState structureState,
            @NotNull StructureManager structureManager,
            @NotNull ChunkAccess chunkAccess,
            @NotNull StructureTemplateManager structureTemplateManager) {
        if (delegate != null)
            delegate.createStructures(registryAccess, structureState, structureManager,
                    chunkAccess, structureTemplateManager);
    }

    @Override
    public int getSeaLevel() {
        if (dryLand) return getMinY();
        if (waterWorld) return 127;
        return delegate != null ? delegate.getSeaLevel() : 63;
    }

    @Override
    public int getMinY() {
        return delegate != null ? delegate.getMinY() : -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.@NotNull Types type,
            @NotNull LevelHeightAccessor level, @NotNull RandomState random) {
        if (delegate != null) return delegate.getBaseHeight(x, z, type, level, random);
        return 63;
    }

    @Override
    public @NotNull NoiseColumn getBaseColumn(int x, int z,
            @NotNull LevelHeightAccessor level, @NotNull RandomState random) {
        if (delegate != null) return delegate.getBaseColumn(x, z, level, random);
        return new NoiseColumn(0, new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(@NotNull List<String> info,
            @NotNull RandomState random, @NotNull BlockPos pos) {
        if (delegate != null) delegate.addDebugScreenInfo(info, random, pos);
    }
}
