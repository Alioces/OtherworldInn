package com.otherworldinn.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.otherworldinn.world.event.listener.TownProtectionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.tartaricacid.touhoulittlemaid.entity.task.TaskNormalFarm", remap = false)
public abstract class MixinLittleMaidTaskNormalFarm {
    @Inject(method = "canPlant", at = @At("RETURN"), cancellable = true, remap = false)
    private void filterTownProtectedPlantTarget(
            EntityMaid maid,
            BlockPos basePos,
            BlockState baseState,
            ItemStack seed,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }
        if (!TownProtectionHandler.canMaidOperateAt(maid, basePos, maid.level())
                || !TownProtectionHandler.canMaidOperateAt(maid, basePos.above(), maid.level())) {
            cir.setReturnValue(false);
        }
    }
}
