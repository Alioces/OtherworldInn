package com.otherworldinn.world.expedition;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.item.ExpeditionChartItem;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.otherworldinn.world.expedition.ExpeditionNbtHelper;

@EventBusSubscriber(modid = OtherworldInn.MODID)
public final class ChartDistributionService {

    private static final int INITIAL_CHART_COUNT = 3;
    private static final int WEEKLY_CHART_COUNT = 1;
    private static final long WEEK_TICKS = 168000L;

    private ChartDistributionService() {}

    public static void onInnOpened(TeamData team, ServerLevel level) {
        InnData innData = team.getInnData();
        if (innData.isInitialChartsGiven()) return;

        innData.setInitialChartsGiven(true);
        giveChartsToTeam(team, level, INITIAL_CHART_COUNT);
        TeamManager.getInstance().syncTeam(team, level.getServer());
        OtherworldInn.LOGGER.info("Distributed {} initial pioneer charts to team", INITIAL_CHART_COUNT);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 1200 != 0) return;

        ServerLevel overworld = event.getServer().overworld();
        int currentWeek = (int) (overworld.getGameTime() / WEEK_TICKS);
        if (currentWeek <= 0) return;

        var data = TeamManager.getInstance().getData(event.getServer());

        for (TeamData team : data.getTeams().values()) {
            InnData innData = team.getInnData();

            if (!innData.isInitialChartsGiven()) continue;
            if (innData.getLastChartDistributionWeek() >= currentWeek) continue;

            innData.setLastChartDistributionWeek(currentWeek);

            ServerLevel townLevel = event.getServer().getLevel(TownDimensions.TOWN_LEVEL);
            if (townLevel == null) continue;

            giveChartsToTeam(team, townLevel, WEEKLY_CHART_COUNT);
            TeamManager.getInstance().syncTeam(team, event.getServer());
            TeamManager.getInstance().getData(event.getServer()).markDirty();
        }
    }

    private static void giveChartsToTeam(TeamData team, ServerLevel level, int count) {
        ItemStack chart = createChart(team);
        int given = 0;

        for (var memberId : team.getMembers()) {
            if (given >= count) break;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(memberId);
            if (player != null) {
                if (player.getInventory().add(chart.copy())) {
                    given++;
                }
            }
        }

        if (given < count) {
            var players = level.getServer().getPlayerList().getPlayers();
            for (ServerPlayer player : players) {
                if (given >= count) break;
                if (team.hasMember(player.getUUID()) && !hasChartInInventory(player)) {
                    if (player.getInventory().add(chart.copy())) {
                        given++;
                    }
                }
            }
        }
    }

    private static ItemStack createChart(TeamData team) {
        ItemStack chart = new ItemStack(ModItems.PIONEER_CHART.get());
        CompoundTag tag = ExpeditionNbtHelper.readTag(chart);
        int starLevel = team.getInnData().getRating();
        int slots = starLevel + 1;
        tag.putInt("star_level", starLevel);
        tag.putInt("max_slots", Math.min(6, slots));
        ExpeditionNbtHelper.writeTag(chart, tag);
        return chart;
    }

    private static boolean hasChartInInventory(ServerPlayer player) {
        for (var stack : player.getInventory().items) {
            if (stack.getItem() instanceof ExpeditionChartItem) return true;
        }
        return false;
    }
}