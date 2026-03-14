package com.otherworldinn.mixin;

import com.otherworldinn.init.ModItems;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes$SplashingType")
public class MixinFanProcessing {

    @Inject(
            method = "process",
            at = @At("RETURN"),
            cancellable = true
    )
    private void preserveBedSheetDamage(ItemStack input, Level level, CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> results = cir.getReturnValue();
        if (results == null || results.isEmpty() || !input.is(ModItems.MESSY_BED_SHEET.get())) {
            return;
        }
        ItemStack firstResult = results.getFirst();
        if (!firstResult.is(ModItems.BED_SHEET.get())) {
            return;
        }
        ItemStack adjusted = firstResult.copy();
        adjusted.setDamageValue(Math.min(adjusted.getMaxDamage(), input.getDamageValue()));
        results.set(0, adjusted);
        cir.setReturnValue(results);
    }
}
