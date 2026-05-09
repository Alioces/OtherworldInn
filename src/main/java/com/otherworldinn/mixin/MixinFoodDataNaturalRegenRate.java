package com.otherworldinn.mixin;

import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.event.runtime.ExhaustionRegenBridge;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FoodData.class)
public abstract class MixinFoodDataNaturalRegenRate {

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void reduceNaturalRegenOutsideTown(Player player, float amount) {
        if (player.level().isClientSide || player.level().dimension() == TownDimensions.TOWN_LEVEL) {
            player.heal(amount);
            return;
        }
        ExhaustionRegenBridge.isNaturalRegenTick = true;
        try {
            if ((player.tickCount & 1) == 0) {
                player.heal(amount);
            }
        } finally {
            ExhaustionRegenBridge.isNaturalRegenTick = false;
        }
    }
}
