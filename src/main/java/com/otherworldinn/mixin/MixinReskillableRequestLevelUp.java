package com.otherworldinn.mixin;

import com.otherworldinn.compat.FixedXpCurve;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.bandit.reskillable.common.network.payload.RequestLevelUp", remap = false)
public abstract class MixinReskillableRequestLevelUp {
    @Inject(method = "getTotalXp", at = @At("HEAD"), cancellable = true, remap = false)
    private static void useFixedTotalXp(
            ServerPlayer player, CallbackInfoReturnable<Integer> cir) {
        if (player == null) {
            cir.setReturnValue(0);
            return;
        }
        cir.setReturnValue(
                FixedXpCurve.getTotalXp(player.experienceLevel, player.experienceProgress));
    }

    @Inject(method = "getXpForLevel", at = @At("HEAD"), cancellable = true, remap = false)
    private static void useFixedXpForLevel(int level, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(FixedXpCurve.getXpForLevel(level));
    }

    @Inject(method = "getLevelForTotalXp", at = @At("HEAD"), cancellable = true, remap = false)
    private static void useFixedLevelForTotalXp(int totalXp, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(FixedXpCurve.getLevelForTotalXp(totalXp));
    }

    @Inject(method = "getProgressForLevel", at = @At("HEAD"), cancellable = true, remap = false)
    private static void useFixedProgressForLevel(
            int totalXp, int level, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(FixedXpCurve.getProgressForLevel(totalXp, level));
    }
}
