package com.otherworldinn.world.dimension;

import com.otherworldinn.OtherworldInn;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * 维度定义
 */
public class TownDimensions {
    // 城镇维度定义
    public static final ResourceKey<LevelStem> TOWN_LEVEL_STEM = ResourceKey.create(Registries.LEVEL_STEM, 
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "town"));
    public static final ResourceKey<Level> TOWN_LEVEL = ResourceKey.create(Registries.DIMENSION, 
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "town"));
    public static final ResourceKey<DimensionType> TOWN_DIM_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, 
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "town_type"));

    // 资源主世界定义
    public static final ResourceKey<LevelStem> RESOURCE_OVERWORLD_STEM = ResourceKey.create(Registries.LEVEL_STEM, 
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "resource_overworld"));
    public static final ResourceKey<Level> RESOURCE_OVERWORLD_LEVEL = ResourceKey.create(Registries.DIMENSION, 
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "resource_overworld"));

    public static void register() {}
}
