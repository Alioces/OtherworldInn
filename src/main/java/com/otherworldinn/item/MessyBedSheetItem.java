package com.otherworldinn.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import com.otherworldinn.init.ModItems;

/**
 * 脏乱的床单物品
 * <p>
 * 通过清理脏床获得。
 * 对着水源或含水方块长按右键可清洗回干净的床单，消耗耐久。
 * </p>
 */
public class MessyBedSheetItem extends Item {

    public MessyBedSheetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // 获取视线目标
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.ANY);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(pos);
            
            // 检查是否为水或含水方块
            boolean isWater = state.getBlock() == Blocks.WATER;
            boolean isWaterlogged = state.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
            
            if (isWater || isWaterlogged) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(stack);
            }
        }
        
        return super.use(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (livingEntity instanceof Player player && level.isClientSide) {
            // 播放洗刷刷的粒子效果
            if (level.getGameTime() % 5 == 0) {
                 BlockHitResult hitResult = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.ANY);
                 if (hitResult.getType() == HitResult.Type.BLOCK) {
                     BlockPos pos = hitResult.getBlockPos();
                     for(int i = 0; i < 25; ++i) {
                        level.addParticle(ParticleTypes.SPLASH, 
                            pos.getX() + 0.5 + (level.random.nextDouble() - 0.5), 
                            pos.getY() + 1.0, 
                            pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5), 
                            (level.random.nextDouble() - 0.5) * 0.5, 
                            level.random.nextDouble() * 0.5, 
                            (level.random.nextDouble() - 0.5) * 0.5);
                     }
                 }
            }
            // 播放音效
            if (level.getGameTime() % 10 == 0) {
                level.playSound(player, player.blockPosition(), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide && livingEntity instanceof Player player) {
            // 消耗耐久
            stack.hurtAndBreak(1, player, Player.getSlotForHand(player.getUsedItemHand()));
            
            // 如果物品没损坏，转换回干净的床单
            if (!stack.isEmpty()) {
                ItemStack cleanSheet = new ItemStack(ModItems.BED_SHEET.get());
                // 继承耐久度
                cleanSheet.setDamageValue(stack.getDamageValue());
                
                // 播放声音
                level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
                
                return cleanSheet;
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 200; // 10秒 = 200 ticks
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH; // 刷子动画
    }
}
