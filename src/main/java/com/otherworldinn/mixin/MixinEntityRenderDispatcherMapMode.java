package com.otherworldinn.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcherMapMode {
    private static final String MAP_MODE_HIDDEN_TAG = "otherworldinn.map_mode_hidden";

    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void otherworldinn$skipRenderForMapModeHidden(
            Entity entity,
            double x,
            double y,
            double z,
            float yRot,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci) {
        if (entity.getTags().contains(MAP_MODE_HIDDEN_TAG)) {
            ci.cancel();
        }
    }
}
