package com.otherworldinn.mixin;

import com.otherworldinn.world.expedition.ExpeditionService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NeutralMob.class)
public interface MixinNeutralMobAnger {

    @Inject(method = "isAngry", at = @At("RETURN"), cancellable = true)
    default void otherworldinn$forceAnger(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        Entity self = (Entity) this;
        if (self.level().isClientSide) return;
        if (ExpeditionService.hasDimComponent(
                self.level().dimension(), "universal_anger")) {
            cir.setReturnValue(true);
        }
    }
}
