package com.otherworldinn.mixin;

import com.otherworldinn.foundation.ModBlockProperties;
import com.otherworldinn.item.BedSheetItem;
import com.otherworldinn.item.MessyBedSheetItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 床方块 Mixin
 *
 * <p>为床添加“是否脏乱”(messy) 属性。
 */
@Mixin(BedBlock.class)
public abstract class MixinBedBlock extends HorizontalDirectionalBlock {

    protected MixinBedBlock(Properties properties) {
        super(properties);
    }

    /**
     * 注入 createBlockStateDefinition 方法
     *
     * <p>将 MESSY 属性注册到 BlockState 定义中。
     */
    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    protected void injectCreateBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(ModBlockProperties.MESSY);
    }

    /**
     * 注入构造函数
     *
     * <p>设置默认状态时，将 MESSY 设为 false。
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void injectConstructor(DyeColor color, Properties properties, CallbackInfo ci) {
        this.registerDefaultState(
                this.defaultBlockState().setValue(ModBlockProperties.MESSY, false));
    }

    /**
     * 拦截右键交互
     *
     * <p>如果床是脏乱的，且玩家手持床单，则禁止睡觉/设置重生点。 这样可以确保优先执行床单的清理/交互逻辑。
     */
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    public void injectUseWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (state.getValue(ModBlockProperties.MESSY)) {
            // 检查玩家是否手持床单或脏乱的床单
            if (player.getMainHandItem().getItem() instanceof BedSheetItem
                    || player.getOffhandItem().getItem() instanceof BedSheetItem
                    || player.getMainHandItem().getItem() instanceof MessyBedSheetItem
                    || player.getOffhandItem().getItem() instanceof MessyBedSheetItem) {
                // 阻止默认交互 (睡觉)
                cir.setReturnValue(InteractionResult.PASS);
            }
        }
    }

    /**
     * 客户端粒子效果
     *
     * <p>如果床是脏乱的，随机生成灰尘粒子。
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ModBlockProperties.MESSY)) {
            for (int i = 0; i < 20; i++) {
                double x = pos.getX() + random.nextDouble();
                double y = pos.getY() + random.nextDouble() * 0.5 + 0.2; // 略高于床面
                double z = pos.getZ() + random.nextDouble();
                level.addParticle(ParticleTypes.MYCELIUM, x, y, z, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
