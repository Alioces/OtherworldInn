package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.BlockDataGenInfo;
import com.otherworldinn.foundation.BlockReg;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 方块注册中心
 */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OtherworldInn.MODID);

    // 存储方块的数据生成信息
    public static final Map<DeferredBlock<?>, BlockDataGenInfo> BLOCK_INFOS = new HashMap<>();

    /**
     * 开始一个方块的链式注册
     *
     * @param name    方块注册名
     * @param factory 方块工厂方法 (例如 Block::new)
     * @return BlockReg 构建器
     */
    public static <T extends Block> BlockReg<T> register(String name, java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, T> factory) {
        return new BlockReg<>(name, factory);
    }
}

