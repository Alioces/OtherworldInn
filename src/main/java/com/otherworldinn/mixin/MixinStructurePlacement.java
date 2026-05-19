package com.otherworldinn.mixin;

import com.otherworldinn.world.expedition.ExpeditionService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.levelgen.structure.placement.StructurePlacement$RandomSpreadStructurePlacement")
public class MixinStructurePlacement {

    @Inject(method = "spacing", at = @At("RETURN"), cancellable = true)
    private void otherworldinn$boostSpacing(CallbackInfoReturnable<Integer> cir) {
        if (ExpeditionService.isStructureBoostActive()) {
            cir.setReturnValue(Math.max(4, cir.getReturnValue() / 2));
        }
    }
}
