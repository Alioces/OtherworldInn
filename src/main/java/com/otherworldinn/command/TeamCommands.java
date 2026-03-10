package com.otherworldinn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * 队伍命令
 * <p>
 * /innteam create <name> - 创建队伍
 * /innteam invite <player> - 邀请玩家（简化：直接加入）
 * /innteam join <player_in_team> - 加入某玩家所在的队伍
 * /innteam leave - 离开队伍
 * /innteam kick <player> - 踢出成员（仅队长）
 * /innteam transfer <player> - 转让队长（仅队长）
 * /innteam rename <name> - 重命名队伍（仅队长）
 * /innteam info - 查看队伍信息
 * /innteam teleport <enabled> - 开启/关闭队伍传送（管理员）
 * </p>
 */
public class TeamCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("innteam")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(TeamCommands::createTeam)))
                .then(Commands.literal("join")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(TeamCommands::joinTeam)))
                .then(Commands.literal("leave")
                        .executes(TeamCommands::leaveTeam))
                .then(Commands.literal("kick")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(TeamCommands::kickMember)))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(TeamCommands::transferLeader)))
                .then(Commands.literal("rename")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(TeamCommands::renameTeam)))
                .then(Commands.literal("info")
                        .executes(TeamCommands::teamInfo))
                .then(Commands.literal("teleport")
                        .requires(s -> s.hasPermission(2)) // 需要管理员权限
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(TeamCommands::toggleTeleport)))
        );
    }

    private static int toggleTeleport(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            boolean enabled = BoolArgumentType.getBool(context, "enabled");
            
            TeamManager manager = TeamManager.getInstance();
            TeamData team = manager.getPlayerTeam(player);
            
            if (team == null) {
                context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.not_in_team"));
                return 0;
            }
            
            manager.setTeamTeleportUnlocked(team, enabled, context.getSource().getServer());
            context.getSource().sendSuccess(() -> Component.translatable("command.otherworldinn.team.teleport_set", enabled), true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int createTeam(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");
            
            TeamManager manager = TeamManager.getInstance();
            if (manager.getPlayerTeam(player) != null) {
                context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.already_in_team"));
                return 0;
            }
            
            TeamData team = manager.createTeam(player, name);
            context.getSource().sendSuccess(() -> Component.translatable("command.otherworldinn.team.created", team.getName()), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int joinTeam(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            
            TeamManager manager = TeamManager.getInstance();
            TeamData targetTeam = manager.getPlayerTeam(target);
            
            if (targetTeam == null) {
                context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.target_no_team"));
                return 0;
            }
            
            // 简化逻辑：直接加入，无需同意
            manager.joinTeam(player, targetTeam.getTeamId());
            context.getSource().sendSuccess(() -> Component.translatable("command.otherworldinn.team.joined", targetTeam.getName()), true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int leaveTeam(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            TeamManager manager = TeamManager.getInstance();
            
            if (manager.getPlayerTeam(player) == null) {
                context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.not_in_team"));
                return 0;
            }
            
            manager.leaveTeam(player);
            context.getSource().sendSuccess(() -> Component.translatable("command.otherworldinn.team.left"), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int kickMember(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            
            TeamManager manager = TeamManager.getInstance();
            TeamData team = manager.getPlayerTeam(player);
            
            if (team == null) {
                context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.not_in_team"));
                return 0;
            }
            
            if (!player.getUUID().equals(team.getLeaderId())) {
                context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.not_leader"));
                return 0;
            }
            
            if (!team.hasMember(target.getUUID())) {
                context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.target_not_in_team"));
                return 0;
            }
            
            if (player.getUUID().equals(target.getUUID())) {
                 context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.kick_self"));
                 return 0;
            }
            
            manager.leaveTeam(target);
            context.getSource().sendSuccess(() -> Component.translatable("command.otherworldinn.team.kicked", target.getName().getString()), true);
            target.sendSystemMessage(Component.translatable("command.otherworldinn.team.you_were_kicked"));
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int transferLeader(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            
            TeamManager manager = TeamManager.getInstance();
            TeamData team = manager.getPlayerTeam(player);
            
            if (team == null) {
                context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.not_in_team"));
                return 0;
            }
            
            if (!player.getUUID().equals(team.getLeaderId())) {
                context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.not_leader"));
                return 0;
            }
            
            if (!team.hasMember(target.getUUID())) {
                context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.target_not_in_team"));
                return 0;
            }
            
            manager.transferLeader(team, target.getUUID(), context.getSource().getServer());
            context.getSource().sendSuccess(() -> Component.translatable("command.otherworldinn.team.transferred", target.getName().getString()), true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int renameTeam(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(context, "name");
            
            TeamManager manager = TeamManager.getInstance();
            TeamData team = manager.getPlayerTeam(player);
            
            if (team == null) {
                context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.not_in_team"));
                return 0;
            }
            
            if (!player.getUUID().equals(team.getLeaderId())) {
                context.getSource().sendFailure(Component.translatable("command.otherworldinn.team.not_leader"));
                return 0;
            }
            
            manager.renameTeam(team, name, context.getSource().getServer());
            context.getSource().sendSuccess(() -> Component.translatable("command.otherworldinn.team.renamed", name), true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int teamInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            TeamManager manager = TeamManager.getInstance();
            TeamData team = manager.getPlayerTeam(player);
            
            if (team == null) {
                context.getSource().sendSuccess(() -> Component.translatable("command.otherworldinn.team.not_in_team"), false);
                return 1;
            }
            
            context.getSource().sendSuccess(() -> Component.literal("Team: " + team.getName()), false);
            context.getSource().sendSuccess(() -> Component.literal("Leader: " + (team.getLeaderId() != null ? team.getLeaderId().toString() : "None")), false); 
            context.getSource().sendSuccess(() -> Component.literal("Members: " + team.getMembers().size()), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
