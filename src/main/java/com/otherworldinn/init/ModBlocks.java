package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.BlockDataGenInfo;
import com.otherworldinn.foundation.BlockReg;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;

import com.otherworldinn.block.OverworldPortalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 方块注册中心
 * <p>
 * 负责注册模组中的所有方块。
 * 同时注册对应的方块物品（如果启用）。
 */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OtherworldInn.MODID);

    /** 存储方块的数据生成信息，用于 DataGen */
    public static final Map<DeferredBlock<?>, BlockDataGenInfo> BLOCK_INFOS = new HashMap<>();

    // --- 方块注册 ---

    public static final BlockReg<OverworldPortalBlock> OVERWORLD_PORTAL_REG = register("overworld_portal", OverworldPortalBlock::new)
            .properties(props -> props.noCollission().strength(-1.0F, 3600000.8F).noLootTable().isValidSpawn(ModBlocks::never))
            .lang("Overworld Portal", "主世界传送门")
            .rarity(Rarity.EPIC)
            .stacksTo(64)
            .fireResistant();
            
    public static final DeferredBlock<OverworldPortalBlock> OVERWORLD_PORTAL = OVERWORLD_PORTAL_REG.register();

    /**
     * 判断实体是否可以在该方块上生成
     * 用于 isValidSpawn 属性
     */
    private static Boolean never(BlockState state, BlockGetter level, BlockPos pos, EntityType<?> entityType) {
        return false;
    }

    // --- 辅助方法 ---

    /**
     * 开始一个方块的链式注册
     *
     * @param name    方块注册名
     * @param factory 方块工厂方法 (例如 Block::new)
     * @param <T>     方块类型
     * @return BlockReg 构建器
     */
    public static <T extends Block> BlockReg<T> register(String name, java.util.function.Function<BlockBehaviour.Properties, T> factory) {
        return new BlockReg<>(name, factory);
    }
}

