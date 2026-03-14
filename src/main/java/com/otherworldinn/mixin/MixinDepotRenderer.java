package com.otherworldinn.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.otherworldinn.client.renderer.DisplayPriceRenderer;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DepotRenderer.class)
public class MixinDepotRenderer {

    @Inject(method = "renderSafe", at = @At("TAIL"), remap = false)
    private void injectRenderSafe(
            DepotBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay,
            CallbackInfo ci) {
        DisplayPriceRenderer.render(
                blockEntity, partialTicks, poseStack, bufferSource, light, overlay);
    }
}
