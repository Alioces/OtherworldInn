package com.otherworldinn.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.otherworldinn.world.event.listener.TownProtectionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidFarmMoveTask",
        remap = false)
public abstract class MixinLittleMaidFarmMoveTask {
    @Inject(method = "shouldMoveTo", at = @At("RETURN"), cancellable = true, remap = false)
    private void filterTownProtectedFarmTarget(
            ServerLevel worldIn,
            EntityMaid maid,
            BlockPos basePos,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }
        if (!TownProtectionHandler.canMaidOperateAt(maid, basePos, worldIn)
                || !TownProtectionHandler.canMaidOperateAt(maid, basePos.above(), worldIn)) {
            cir.setReturnValue(false);
        }
    }
}
