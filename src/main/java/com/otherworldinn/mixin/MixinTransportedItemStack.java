package com.otherworldinn.mixin;

import com.otherworldinn.util.KaleidoscopeRenderTargets;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TransportedItemStack.class)
public class MixinTransportedItemStack {

    @Shadow public float sideOffset;

    @Shadow public float prevSideOffset;

    @Inject(method = "<init>(Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"), remap = false)
    private void clearRandomOffsetForBlockItems(ItemStack stack, CallbackInfo ci) {
        if (stack.getItem() instanceof BlockItem blockItem
                && KaleidoscopeRenderTargets.isTargetBlock(blockItem.getBlock())) {
            this.sideOffset = 0.0f;
            this.prevSideOffset = 0.0f;
        }
    }
}
