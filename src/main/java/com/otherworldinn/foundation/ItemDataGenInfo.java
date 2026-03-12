package com.otherworldinn.foundation;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录物品的数据生成配置信息
 *
 * @param generateModel 是否生成模型
 * @param modelType     模型类型 (generated, handheld)
 * @param enName        英文名称
 * @param cnName        中文名称
 * @param enTooltips    英文工具提示
 * @param cnTooltips    中文工具提示
 */
public record ItemDataGenInfo(
        boolean generateModel,
        String modelType,
        String enName,
        String cnName,
        List<String> enTooltips,
        List<String> cnTooltips
) {
    public static final ItemDataGenInfo DEFAULT = new ItemDataGenInfo(true, "generated", "", "", new ArrayList<>(), new ArrayList<>());
}
