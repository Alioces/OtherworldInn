package com.otherworldinn.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 床方块 Mixin
 * <p>
 * 为床添加“是否脏乱”(messy) 属性。
 * </p>
 */
@Mixin(BedBlock.class)
public abstract class MixinBedBlock extends HorizontalDirectionalBlock implements BedBlockExtension {

    // 移除本地定义的 MESSY，使用接口中的定义
    // private static final BooleanProperty MESSY = BooleanProperty.create("messy");

    protected MixinBedBlock(Properties properties) {
        super(properties);
    }

    /**
     * 注入 createBlockStateDefinition 方法
     * <p>
     * 将 MESSY 属性注册到 BlockState 定义中。
     * </p>
     */
    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    protected void injectCreateBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(MESSY);
    }

    /**
     * 注入构造函数
     * <p>
     * 设置默认状态时，将 MESSY 设为 false。
     * </p>
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void injectConstructor(Properties properties, CallbackInfo ci) {
        this.registerDefaultState(this.defaultBlockState().setValue(MESSY, false));
    }

    /**
     * 客户端粒子效果
     * <p>
     * 如果床是脏乱的，随机生成灰尘粒子。
     * </p>
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(MESSY) && random.nextInt(5) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble() * 0.5 + 0.2; // 略高于床面
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.MYCELIUM, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }
}
