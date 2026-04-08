package com.otherworldinn.mixin;

import com.otherworldinn.compat.ReskillableCompat;
import com.otherworldinn.compat.ultimine.FTBUltimineConfigState;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbultimine.config.FTBUltimineServerConfig", remap = false)
public interface MixinFTBUltimineServerConfig {
    @Shadow @Final SNBTConfig COSTS_LIMITS = null;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void otherworldinn$injectMinBlocksConfig(CallbackInfo ci) {
        if (FTBUltimineConfigState.minBlocks != null) {
            return;
        }
        FTBUltimineConfigState.minBlocks =
                COSTS_LIMITS.addInt("min_blocks", 0)
                        .range(0, 32768)
                        .comment(
                                "Min amount of blocks that can be ultimined at once when skill-scaling is active",
                                "When used with Reskillable Reimagined mining skill, actual max blocks scales linearly from min_blocks to max_blocks");
    }

    @Inject(method = "getMaxBlocks", at = @At("RETURN"), cancellable = true, remap = false)
    private static void otherworldinn$scaleMaxBlocksByMiningLevel(
            ServerPlayer player, CallbackInfoReturnable<Integer> cir) {
        if (player == null
                || !ReskillableCompat.isLoaded()
                || FTBUltimineConfigState.minBlocks == null) {
            return;
        }
        int configuredMax = Math.max(0, cir.getReturnValue());
        int configuredMin = Mth.clamp(FTBUltimineConfigState.minBlocks.get(), 0, configuredMax);
        if (configuredMax <= configuredMin) {
            cir.setReturnValue(configuredMax);
            return;
        }
        int maxSkillLevel = Math.max(1, ReskillableCompat.getMaxLevel());
        int miningSkillLevel =
                Mth.clamp(ReskillableCompat.getSkillLevel(player, "mining"), 1, maxSkillLevel);
        double ratio = (double) miningSkillLevel / (double) maxSkillLevel;
        int scaledBlocks =
                configuredMin
                        + (int) Math.round((configuredMax - configuredMin) * ratio);
        cir.setReturnValue(Mth.clamp(scaledBlocks, configuredMin, configuredMax));
    }
}
