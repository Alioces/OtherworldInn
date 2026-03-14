package com.otherworldinn.init;

import com.mojang.serialization.MapCodec;
import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dimension.TownChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 维度注册中心
 *
 * <p>负责注册自定义维度的 ChunkGenerator。
 */
public class ModDimensions {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, OtherworldInn.MODID);

    public static void register(IEventBus eventBus) {
        CHUNK_GENERATORS.register("town_chunk_generator", () -> TownChunkGenerator.CODEC);
        CHUNK_GENERATORS.register(eventBus);
    }
}
