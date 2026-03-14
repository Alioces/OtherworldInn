package com.otherworldinn.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.otherworldinn.client.renderer.DisplayPriceRenderer;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.content.logistics.depot.EjectorRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EjectorRenderer.class)
public class MixinEjectorRenderer {

    @Inject(method = "renderSafe", at = @At("TAIL"), remap = false)
    private void injectRenderSafe(
            EjectorBlockEntity blockEntity,
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
