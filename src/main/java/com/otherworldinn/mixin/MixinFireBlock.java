package com.otherworldinn.mixin;

import com.otherworldinn.world.dimension.TownDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class MixinFireBlock {

    /**
     * 在城镇维度禁止火焰的随机刻逻辑（扩散、烧毁方块、自然熄灭）。
     *
     * <p>取消整个 tick 方法，火焰在城镇维度被放置后只保持静态显示状态。
     * 城镇外仍保留原版火灾行为。
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void cancelFireTickInTown(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci) {
        if (level.dimension() == TownDimensions.TOWN_LEVEL) {
            ci.cancel();
        }
    }
}
