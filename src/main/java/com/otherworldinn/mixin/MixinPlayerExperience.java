package com.otherworldinn.mixin;

import com.otherworldinn.compat.FixedXpCurve;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayerExperience {
    @Inject(method = "getXpNeededForNextLevel", at = @At("HEAD"), cancellable = true)
    private void useFixedXpCurve(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(FixedXpCurve.XP_PER_LEVEL);
    }
}
