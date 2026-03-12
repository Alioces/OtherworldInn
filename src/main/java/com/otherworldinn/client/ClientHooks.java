package com.otherworldinn.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;

/**
 * 客户端钩子
 * <p>
 * 用于在客户端执行特定的逻辑，避免在服务端加载客户端类。
 * </p>
 */
public class ClientHooks {

    public static void displayRecallScrollActivation(ItemStack stack) {
        // 只有在客户端才会被调用，因此这里引用 Minecraft 是安全的（只要类不被提前加载/验证）
        Minecraft.getInstance().gameRenderer.displayItemActivation(stack);
    }
}
