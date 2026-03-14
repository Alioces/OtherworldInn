package com.otherworldinn.mixin;

import com.otherworldinn.client.renderer.DeskBellIconRenderer;
import com.simibubi.create.content.redstone.deskBell.DeskBellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeskBellBlockEntity.class)
public abstract class MixinDeskBellBlockEntity extends BlockEntity {

    public MixinDeskBellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Inject(method = "ding", at = @At("HEAD"), remap = false)
    private void onDing(CallbackInfo ci) {
        if (this.hasLevel() && this.level.isClientSide) {
            DeskBellIconRenderer.triggerAnimation(this.getBlockPos(), this.level.getGameTime());
        }
    }
}
