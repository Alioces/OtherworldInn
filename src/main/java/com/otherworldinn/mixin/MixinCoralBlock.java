package com.otherworldinn.mixin;

import com.otherworldinn.world.dimension.TownDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CoralBlock.class)
public class MixinCoralBlock {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void preventDeathInTown(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (level.dimension() == TownDimensions.TOWN_LEVEL) {
            ci.cancel();
        }
    }
}
