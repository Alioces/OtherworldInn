package com.otherworldinn.mixin;

import com.otherworldinn.compat.FixedXpCurve;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.bandit.reskillable.client.screen.SkillScreen", remap = false)
public abstract class MixinReskillableSkillScreen {
    @Inject(method = "calculateTotalXP", at = @At("HEAD"), cancellable = true, remap = false)
    private void useFixedTotalXp(Player player, CallbackInfoReturnable<Integer> cir) {
        if (player == null) {
            cir.setReturnValue(0);
            return;
        }
        cir.setReturnValue(FixedXpCurve.getTotalXp(player.experienceLevel, player.experienceProgress));
    }
}
