package com.otherworldinn.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class MixinFishingHook {
    @Mutable
    @Shadow
    @Final
    private int lureSpeed;

    @Inject(
            method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V",
            at = @At("TAIL"))
    private void applyLuckFishingSpeedBonus(
            Player player, Level level, int luck, int lureSpeed, CallbackInfo ci) {
        int luckBonusLureLevel = Mth.clamp(luck, 0, 5);
        if (luckBonusLureLevel > 0) {
            this.lureSpeed += luckBonusLureLevel;
        }
    }
}
