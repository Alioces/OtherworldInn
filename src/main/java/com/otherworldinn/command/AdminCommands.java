package com.otherworldinn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.otherworldinn.world.event.runtime.DimensionResetManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 管理员命令
 *
 * <p>/innadmin reset_dimensions - 强制触发维度重置
 */
public class AdminCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("innadmin")
                        .requires(s -> s.hasPermission(2)) // 需要管理员权限 (Level 2)
                        .then(
                                Commands.literal("reset_dimensions")
                                        .executes(AdminCommands::resetDimensions)));
    }

    private static int resetDimensions(CommandContext<CommandSourceStack> context) {
        context.getSource()
                .sendSuccess(
                        () ->
                                Component.translatable(
                                        "command.otherworldinn.admin.reset_dimensions.start"),
                        true);
        // 调用 DimensionResetManager 的强制重置方法
        DimensionResetManager.forceReset(context.getSource().getServer());
        return 1;
    }
}
