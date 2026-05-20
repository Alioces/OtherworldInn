package com.otherworldinn.init;

import com.mojang.serialization.MapCodec;
import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.loot.ChartComponentLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM =
            DeferredRegister.create(
                    NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    OtherworldInn.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>,
            MapCodec<ChartComponentLootModifier>> CHART_COMPONENT =
            GLM.register("chart_component", () -> ChartComponentLootModifier.CODEC.get());
}
