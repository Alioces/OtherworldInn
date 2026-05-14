package com.otherworldinn.world.expedition.recipe;

import com.otherworldinn.OtherworldInn;
import java.util.function.Supplier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ExpeditionRecipeSerializers {

    private ExpeditionRecipeSerializers() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER,
                    OtherworldInn.MODID);

    public static final Supplier<RecipeSerializer<?>> CHART_COMPONENT =
            SERIALIZERS.register("chart_component",
                    () -> {
                        RecipeSerializer<?> s =
                                new SimpleCraftingRecipeSerializer<>(ChartComponentRecipe::new);
                        ChartComponentRecipe.SERIALIZER = s;
                        return s;
                    });

    public static final Supplier<RecipeSerializer<?>> CHART_ATTACHMENT =
            SERIALIZERS.register("chart_attachment",
                    () -> {
                        RecipeSerializer<?> s =
                                new SimpleCraftingRecipeSerializer<>(ChartAttachmentRecipe::new);
                        ChartAttachmentRecipe.SERIALIZER = s;
                        return s;
                    });

    public static final Supplier<RecipeSerializer<?>> CHART_DIMENSION =
            SERIALIZERS.register("chart_dimension",
                    () -> {
                        RecipeSerializer<?> s =
                                new SimpleCraftingRecipeSerializer<>(ChartDimensionRecipe::new);
                        ChartDimensionRecipe.SERIALIZER = s;
                        return s;
                    });

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
