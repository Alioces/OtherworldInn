package com.otherworldinn.foundation;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录方块的数据生成配置信息
 *
 * @param generateModel 是否生成模型
 * @param renderType 渲染类型 (solid, cutout, translucent)
 * @param lootConfig 战利品表配置
 * @param toolType 挖掘工具类型
 * @param miningLevel 挖掘等级
 * @param enName 英文名称
 * @param cnName 中文名称
 * @param enTooltips 英文工具提示
 * @param cnTooltips 中文工具提示
 */
public record BlockDataGenInfo(
        boolean generateModel,
        String renderType,
        LootConfig lootConfig,
        ToolType toolType,
        MiningLevel miningLevel,
        String enName,
        String cnName,
        List<String> enTooltips,
        List<String> cnTooltips) {
    public static final BlockDataGenInfo DEFAULT =
            new BlockDataGenInfo(
                    true,
                    "solid",
                    LootConfig.DEFAULT,
                    ToolType.NONE,
                    MiningLevel.NONE,
                    "",
                    "",
                    new ArrayList<>(),
                    new ArrayList<>());

    public enum ToolType {
        PICKAXE,
        AXE,
        SHOVEL,
        HOE,
        NONE
    }

    public enum MiningLevel {
        NONE,
        STONE,
        IRON,
        DIAMOND,
        NETHERITE
    }
}
