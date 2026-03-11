package com.otherworldinn.mixin;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * BedBlock 扩展接口
 * <p>
 * 提供对 MESSY 属性的全局访问。
 * </p>
 */
public interface BedBlockExtension {
    BooleanProperty MESSY = BooleanProperty.create("messy");
}
