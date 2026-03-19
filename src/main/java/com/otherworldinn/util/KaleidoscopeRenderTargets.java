package com.otherworldinn.util;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BottleBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public final class KaleidoscopeRenderTargets {
    private static final String FOOD_BITE_ONE_BY_TWO_CLASS_NAME = "FoodBiteOneByTwoBlock";
    private static final ResourceLocation COLD_CUT_HAM_SLICES_ID =
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "cold_cut_ham_slices");

    private KaleidoscopeRenderTargets() {}

    public static boolean isTargetBlock(Block block) {
        return block instanceof FoodBlock || block instanceof BottleBlock;
    }

    public static boolean isBottleBlock(Block block) {
        return block instanceof BottleBlock;
    }

    public static float getFoodScaleMultiplier(Block block) {
        if (isBlockSimpleName(block, FOOD_BITE_ONE_BY_TWO_CLASS_NAME)) {
            return 0.5f;
        }
        return 1.0f;
    }

    public static boolean isOneByTwoFoodBlock(Block block) {
        return isBlockSimpleName(block, FOOD_BITE_ONE_BY_TWO_CLASS_NAME);
    }

    public static boolean isColdCutHamSlicesBlock(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return COLD_CUT_HAM_SLICES_ID.equals(id);
    }

    public static BlockState[] resolveOneByTwoStates(BlockState state) {
        BlockState left = state;
        BlockState right = state;
        IntegerProperty positionProperty = findPositionProperty(state);
        if (positionProperty != null) {
            left = left.setValue(positionProperty, 0);
            right = right.setValue(positionProperty, 1);
            return new BlockState[] {resolveFoodRenderState(left), resolveFoodRenderState(right)};
        }
        return new BlockState[] {resolveFoodRenderState(left), resolveFoodRenderState(right)};
    }

    public static BlockState resolveFoodRenderState(BlockState state) {
        return state;
    }

    private static boolean isBlockSimpleName(Block block, String simpleName) {
        return block.getClass().getSimpleName().equals(simpleName);
    }

    private static IntegerProperty findPositionProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty
                    && "position".equals(property.getName())
                    && integerProperty.getPossibleValues().contains(0)
                    && integerProperty.getPossibleValues().contains(1)) {
                return integerProperty;
            }
        }
        return null;
    }
}
