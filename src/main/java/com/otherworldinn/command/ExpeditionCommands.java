package com.otherworldinn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.otherworldinn.item.ExpeditionChartItem;
import com.otherworldinn.world.expedition.ExpeditionService;
import com.otherworldinn.world.expedition.ExpeditionSession;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.otherworldinn.world.expedition.ExpeditionNbtHelper;

public class ExpeditionCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("expedition")
                        .then(Commands.literal("join")
                                .then(Commands.argument("chartId", StringArgumentType.word())
                                        .executes(ExpeditionCommands::join)))
                        .then(Commands.literal("cancel")
                                .executes(ExpeditionCommands::cancel))
                        .then(Commands.literal("status")
                                .executes(ExpeditionCommands::status)));
    }

    private static int join(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        String chartIdStr = StringArgumentType.getString(ctx, "chartId");
        UUID chartUuid;
        try {
            chartUuid = UUID.fromString(chartIdStr);
        } catch (IllegalArgumentException e) {
            player.displayClientMessage(
                    Component.translatable("message.otherworldinn.expedition.invalid_id")
                            .withStyle(ChatFormatting.RED), false);
            return 0;
        }

        ServerPlayer leader = findRecruitingLeader(player, chartUuid);
        if (leader == null) {
            player.displayClientMessage(
                    Component.translatable("message.otherworldinn.expedition.recruit_expired")
                            .withStyle(ChatFormatting.RED), false);
            return 0;
        }

        TeamData playerTeam = TeamManager.getInstance().getPlayerTeam(player);
        TeamData leaderTeam = TeamManager.getInstance().getPlayerTeam(leader);
        if (playerTeam == null || leaderTeam == null
                || !playerTeam.equals(leaderTeam)) {
            player.displayClientMessage(
                    Component.translatable("message.otherworldinn.expedition.not_same_team")
                            .withStyle(ChatFormatting.RED), false);
            return 0;
        }

        if (ExpeditionService.getPlayerSession(player.getUUID()) != null) {
            player.displayClientMessage(
                    Component.translatable("message.otherworldinn.expedition.already_in")
                            .withStyle(ChatFormatting.RED), false);
            return 0;
        }

        ItemStack chart = findRecruitingChart(leader, chartUuid);
        if (chart == null) return 0;

        CompoundTag tag = ExpeditionNbtHelper.readTag(chart);
        int maxSlots = tag.getInt("max_slots");
        if (maxSlots <= 0) maxSlots = 1;

        List<UUID> members = ExpeditionChartItem.getMembers(chart);
        if (members.size() >= maxSlots) {
            player.displayClientMessage(
                    Component.translatable("message.otherworldinn.expedition.full")
                            .withStyle(ChatFormatting.RED), false);
            return 0;
        }

        if (members.contains(player.getUUID())) {
            player.displayClientMessage(
                    Component.translatable("message.otherworldinn.expedition.already_member")
                            .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        ListTag memberList = tag.getList("members", Tag.TAG_STRING);
        memberList.add(StringTag.valueOf(player.getUUID().toString()));
        tag.put("members", memberList);
        ExpeditionNbtHelper.writeTag(chart, tag);

        player.displayClientMessage(
                Component.translatable("message.otherworldinn.expedition.joined")
                        .withStyle(ChatFormatting.GREEN), false);
        leader.displayClientMessage(
                Component.literal(player.getName().getString())
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.translatable("message.otherworldinn.expedition.member_joined",
                                memberList.size()).withStyle(ChatFormatting.WHITE)),
                false);

        return 1;
    }

    private static int cancel(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ExpeditionChartItem
                    && "recruiting".equals(ExpeditionChartItem.getChartState(stack))) {
                ExpeditionChartItem.onLeftClickCancel(player, stack);
                return 1;
            }
        }

        player.displayClientMessage(
                Component.translatable("message.otherworldinn.expedition.no_recruiting_chart")
                        .withStyle(ChatFormatting.RED), false);
        return 0;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return 0;
        ExpeditionSession session = ExpeditionService.getPlayerSession(player.getUUID());
        if (session == null) {
            player.displayClientMessage(
                    Component.translatable("message.otherworldinn.expedition.not_in").withStyle(ChatFormatting.GRAY),
                    false);
        } else {
            long remaining = session.deadlineTick() - player.getServer().getTickCount();
            long remainingMin = remaining / (20 * 60);
            player.displayClientMessage(
                    Component.translatable("message.otherworldinn.expedition.status",
                            remainingMin).withStyle(ChatFormatting.GREEN), false);
        }
        return 1;
    }

    private static ItemStack findRecruitingChart(ServerPlayer leader, UUID chartUuid) {
        for (int i = 0; i < leader.getInventory().getContainerSize(); i++) {
            ItemStack stack = leader.getInventory().getItem(i);
            if (stack.getItem() instanceof ExpeditionChartItem
                    && "recruiting".equals(ExpeditionChartItem.getChartState(stack))
                    && chartUuid.equals(ExpeditionChartItem.getChartUuid(stack))) {
                return stack;
            }
        }
        return null;
    }

    private static ServerPlayer findRecruitingLeader(ServerPlayer requester, UUID chartUuid) {
        for (ServerPlayer player : requester.getServer().getPlayerList().getPlayers()) {
            if (findRecruitingChart(player, chartUuid) != null) return player;
        }
        return null;
    }
}