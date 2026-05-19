package com.otherworldinn.mixin;

import com.otherworldinn.world.expedition.ExpeditionService;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServerSave {

    @Inject(method = "saveAllChunks", at = @At("HEAD"))
    private void otherworldinn$cleanupExpeditionLevels(
            boolean bl, boolean bl2, boolean bl3,
            CallbackInfoReturnable<Boolean> cir) {
        if (!ExpeditionService.isShuttingDown()) return;
        MinecraftServer self = (MinecraftServer) (Object) this;
        ExpeditionService.cleanupAllExpeditionLevels(self);
    }
}
