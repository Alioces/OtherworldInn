package com.otherworldinn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.event.TownStructurePlacer;
import com.otherworldinn.world.inn.facility.FacilityRegistry;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import com.otherworldinn.world.event.runtime.DimensionResetManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

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
                                        .executes(AdminCommands::resetDimensions))
                        .then(
                                Commands.literal("facility")
                                        .then(
                                                Commands.literal("downgrade")
                                                        .then(
                                                                Commands.argument(
                                                                                "target",
                                                                                EntityArgument
                                                                                        .player())
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "facilityId",
                                                                                                StringArgumentType
                                                                                                        .word())
                                                                                        .suggests(
                                                                                                (ctx,
                                                                                                        builder) -> {
                                                                                                    for (FacilityRegistry.FacilityDefinition facility :
                                                                                                            FacilityRegistry.getAll()) {
                                                                                                        builder.suggest(
                                                                                                                facility
                                                                                                                        .id());
                                                                                                    }
                                                                                                    return builder
                                                                                                            .buildFuture();
                                                                                                })
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "level",
                                                                                                                IntegerArgumentType
                                                                                                                        .integer(
                                                                                                                                0))
                                                                                                        .executes(
                                                                                                                AdminCommands
                                                                                                                        ::downgradeFacility)))))));
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

    private static int downgradeFacility(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            String facilityId = StringArgumentType.getString(context, "facilityId");
            int targetLevel = IntegerArgumentType.getInteger(context, "level");

            TeamManager manager = TeamManager.getInstance();
            TeamData team = manager.getPlayerTeam(target);
            if (team == null) {
                context.getSource()
                        .sendFailure(Component.translatable("command.otherworldinn.team.target_no_team"));
                return 0;
            }

            FacilityRegistry.FacilityDefinition facility = FacilityRegistry.get(facilityId);
            if (facility == null) {
                context.getSource()
                        .sendFailure(
                                Component.translatable(
                                        "command.otherworldinn.admin.facility.not_found", facilityId));
                return 0;
            }

            if (targetLevel > facility.maxLevel()) {
                context.getSource()
                        .sendFailure(
                                Component.translatable(
                                        "command.otherworldinn.admin.facility.level_out_of_range",
                                        facility.maxLevel()));
                return 0;
            }

            int currentLevel = team.getInnData().getFacilityLevel(facility.id());
            if (targetLevel >= currentLevel) {
                context.getSource()
                        .sendFailure(
                                Component.translatable(
                                        "command.otherworldinn.admin.facility.not_lower",
                                        currentLevel));
                return 0;
            }

            ServerLevel townLevel = context.getSource().getServer().getLevel(TownDimensions.TOWN_LEVEL);
            if (townLevel == null) {
                context.getSource()
                        .sendFailure(
                                Component.translatable(
                                        "command.otherworldinn.admin.facility.town_unavailable"));
                return 0;
            }

            FacilityRegistry.FacilityLevelDefinition levelDefinition = facility.getLevel(targetLevel);
            boolean placed =
                    TownStructurePlacer.placeStructureTemplate(
                            townLevel, levelDefinition.structureId(), facility.centerPos());
            if (!placed) {
                context.getSource()
                        .sendFailure(
                                Component.translatable(
                                        "command.otherworldinn.admin.facility.place_fail"));
                return 0;
            }

            team.getInnData().setFacilityLevel(facility.id(), levelDefinition.level());
            if (levelDefinition.level() == 0) {
                manager.lockMapPoint(team, facility.mapPointId(), context.getSource().getServer());
            } else {
                manager.unlockMapPoint(team, facility.mapPointId(), context.getSource().getServer());
            }

            int downgradedLevel = levelDefinition.level();
            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.translatable(
                                            "command.otherworldinn.admin.facility.downgrade_success",
                                            team.getName(),
                                            Component.translatable(facility.translationKey()),
                                            downgradedLevel),
                            true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
