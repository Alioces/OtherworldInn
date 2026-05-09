package com.otherworldinn.mixin;

import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.event.runtime.ExhaustionRegenBridge;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class MixinPlayerExhaustionRate {
    private static final float NON_TOWN_EXHAUSTION_MULTIPLIER = 1.7F;

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float scaleFoodExhaustionOutsideTown(float exhaustion) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide || player.level().dimension() == TownDimensions.TOWN_LEVEL) {
            return exhaustion;
        }
        if (ExhaustionRegenBridge.isNaturalRegenTick) {
            return exhaustion;
        }
        return exhaustion * NON_TOWN_EXHAUSTION_MULTIPLIER;
    }
}
