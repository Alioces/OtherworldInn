package com.otherworldinn.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.otherworldinn.client.renderer.DeskBellIconRenderer;
import com.simibubi.create.content.redstone.deskBell.DeskBellBlockEntity;
import com.simibubi.create.content.redstone.deskBell.DeskBellRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeskBellRenderer.class)
public class MixinDeskBellRenderer {

    @Inject(method = "renderSafe", at = @At("TAIL"), remap = false)
    private void injectRenderSafe(
            DeskBellBlockEntity blockEntity,
            float partialTicks,
            PoseStack ms,
            MultiBufferSource buffer,
            int light,
            int overlay,
            CallbackInfo ci) {
        DeskBellIconRenderer.render(blockEntity, partialTicks, ms, buffer, light, overlay);
    }
}
