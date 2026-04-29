package com.otherworldinn.mixin;

import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.inn.facility.FacilityRegistry;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 替换 Serene Seasons 的“玻璃温室豁免”逻辑：
 * 仅当作物位于本模组定义的温室范围时，才有 50% 概率获得季节豁免（等效半速）。
 */
@Pseudo
@Mixin(targets = "sereneseasons.season.SeasonalCropGrowthHandler", remap = false)
public class MixinSeasonalCropGrowthHandler {
    private static final String GREENHOUSE_FACILITY_ID = "greenhouse";
    private static final double GREENHOUSE_SEASON_BYPASS_CHANCE = 0.5D;

    @Redirect(
            method = {"onCropGrowth", "applyBonemeal"},
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lsereneseasons/season/SeasonalCropGrowthHandler;isGlassAboveBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"),
            remap = false)
    private static boolean otherworldinn$replaceGlassExemption(Level level, BlockPos pos) {
        if (!isInsideActiveGreenhouse(level, pos)) {
            return false;
        }
        // 温室内按 50% 放行，整体等效为“温室正常速度（含加成）的一半”。
        return level.getRandom().nextDouble() < GREENHOUSE_SEASON_BYPASS_CHANCE;
    }

    private static boolean isInsideActiveGreenhouse(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)
                || pos == null
                || level.dimension() != TownDimensions.TOWN_LEVEL) {
            return false;
        }
        FacilityRegistry.FacilityDefinition greenhouse = FacilityRegistry.get(GREENHOUSE_FACILITY_ID);
        if (greenhouse == null) {
            return false;
        }
        TeamManager manager = TeamManager.getInstance();
        TeamData teamAtPos = manager.getTeamAt(pos, serverLevel.getServer());
        if (getGreenhouseLevelForTeamAtPos(teamAtPos, greenhouse, pos) > 0) {
            return true;
        }
        TeamData nearestTeam = manager.getNearestInn(pos, serverLevel.getServer());
        return nearestTeam != null
                && nearestTeam != teamAtPos
                && getGreenhouseLevelForTeamAtPos(nearestTeam, greenhouse, pos) > 0;
    }

    private static int getGreenhouseLevelForTeamAtPos(
            TeamData team, FacilityRegistry.FacilityDefinition greenhouse, BlockPos pos) {
        if (team == null || greenhouse == null || pos == null) {
            return 0;
        }
        int greenhouseLevel = Math.max(0, team.getInnData().getFacilityLevel(GREENHOUSE_FACILITY_ID));
        if (greenhouseLevel <= 0) {
            return 0;
        }
        if (greenhouse.facilityRange().contains(pos)) {
            return greenhouseLevel;
        }
        for (FacilityRegistry.FacilityRange range : greenhouse.getExtraBuildAllowRanges(greenhouseLevel)) {
            if (range.contains(pos)) {
                return greenhouseLevel;
            }
        }
        return 0;
    }
}
