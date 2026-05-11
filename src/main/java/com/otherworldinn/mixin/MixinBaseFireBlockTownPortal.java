package com.otherworldinn.mixin;

import com.otherworldinn.world.dimension.TownDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseFireBlock.class)
public class MixinBaseFireBlockTownPortal {
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void otherworldinn$blockPortalInTown(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean isMoving,
            CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)
                || serverLevel.dimension() != TownDimensions.TOWN_LEVEL) {
            return;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(dir)).is(Blocks.OBSIDIAN)) {
                ci.cancel();
                return;
            }
        }
    }
}
