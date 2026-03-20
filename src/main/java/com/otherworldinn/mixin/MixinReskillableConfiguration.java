package com.otherworldinn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.bandit.reskillable.Configuration", remap = false)
public abstract class MixinReskillableConfiguration {
    @Inject(method = "getXpScalingMultiplier", at = @At("RETURN"), cancellable = true, remap = false)
    private static void halveXpScalingMultiplier(CallbackInfoReturnable<Double> cir) {
        double value = cir.getReturnValue();
        cir.setReturnValue(value * 8);
    }
}
