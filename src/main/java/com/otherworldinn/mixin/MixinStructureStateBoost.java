package com.otherworldinn.mixin;

import com.otherworldinn.world.expedition.ExpeditionService;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGeneratorStructureState.class)
public class MixinStructureStateBoost {

    @Inject(method = "hasStructureChunkInRange", at = @At("HEAD"), cancellable = true)
    private void otherworldinn$forceStructureChunk(
            net.minecraft.core.Holder<StructureSet> structureSet,
            int x, int z, int range,
            CallbackInfoReturnable<Boolean> cir) {
        if (ExpeditionService.isStructureBoostActive()
                && (x % 2 == 0) && (z % 2 == 0)) {
            cir.setReturnValue(true);
        }
    }
}
