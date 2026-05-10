package com.otherworldinn.mixin;

import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.event.runtime.ExhaustionRegenBridge;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class MixinFoodDataNaturalRegenRate {

    @Inject(method = "tick", at = @At("HEAD"))
    private void markNaturalRegenTick(Player player, CallbackInfo ci) {
        if (!player.level().isClientSide && player.level().dimension() != TownDimensions.TOWN_LEVEL) {
            ExhaustionRegenBridge.isNaturalRegenTick = true;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void clearNaturalRegenTick(Player player, CallbackInfo ci) {
        ExhaustionRegenBridge.isNaturalRegenTick = false;
    }

    @Redirect(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void halveNaturalRegenOutsideTown(Player player, float amount) {
        if (player.level().isClientSide || player.level().dimension() == TownDimensions.TOWN_LEVEL) {
            player.heal(amount);
            return;
        }
        if ((player.tickCount & 1) == 0) {
            player.heal(amount);
        }
    }
}
