package com.otherworldinn.mixin;

import com.otherworldinn.compat.FixedXpCurve;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.bandit.reskillable.client.screen.buttons.SkillButton", remap = false)
public abstract class MixinReskillableSkillButton {
    @Inject(method = "getPlayerTotalXP", at = @At("HEAD"), cancellable = true, remap = false)
    private void useFixedPlayerTotalXp(Player player, CallbackInfoReturnable<Integer> cir) {
        if (player == null) {
            cir.setReturnValue(0);
            return;
        }
        cir.setReturnValue(FixedXpCurve.getTotalXp(player.experienceLevel, player.experienceProgress));
    }
}
