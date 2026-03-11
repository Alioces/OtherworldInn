package com.otherworldinn.item;

import com.otherworldinn.mixin.BedBlockExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.server.level.ServerLevel;
import com.otherworldinn.world.team.TeamManager;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.inn.RoomData;

import net.minecraft.world.item.component.CustomData;

/**
 * 床单物品
 * <p>
 * 用于清理脏乱的床。拥有“干净”和“脏乱”两种状态。
 * 右键点击脏乱的床时，消耗耐久并将床变干净，同时自身变为脏乱状态。
 * </p>
 */
public class BedSheetItem extends Item {

    public BedSheetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // 检查目标是否为脏乱的床
        if (state.getBlock() instanceof BedBlock && 
            state.hasProperty(BedBlockExtension.MESSY) && 
            state.getValue(BedBlockExtension.MESSY)) {

            // 检查床单是否为干净状态
            boolean isSheetMessy = isMessy(stack);

            if (!isSheetMessy) {
                if (!level.isClientSide) {
                    // 1. 清理床铺
                    level.setBlock(pos, state.setValue(BedBlockExtension.MESSY, false), 3);
                    
                    // 同步清理床的另一半
                    BedPart part = state.getValue(BedBlock.PART);
                    BlockPos otherPos = pos.relative(part == BedPart.HEAD ? 
                            state.getValue(BedBlock.FACING).getOpposite() : 
                            state.getValue(BedBlock.FACING));
                    BlockState otherState = level.getBlockState(otherPos);
                    if (otherState.getBlock() instanceof BedBlock && 
                        otherState.hasProperty(BedBlockExtension.MESSY) &&
                        otherState.getValue(BedBlockExtension.MESSY)) {
                        level.setBlock(otherPos, otherState.setValue(BedBlockExtension.MESSY, false), 3);
                    }

                    // 更新房间数据（最大入住人数 +1）并同步
                    if (level instanceof ServerLevel serverLevel) {
                        TeamData team = TeamManager.getInstance().getTeamAt(pos, serverLevel.getServer());
                        if (team != null) {
                            InnData innData = team.getInnData();
                            RoomData room = innData.getRoomAt(pos);
                            if (room != null) {
                                room.setMaxGuests(room.getMaxGuests() + 1);
                                TeamManager.getInstance().syncTeam(team, serverLevel.getServer());
                            }
                        }
                    }

                    // 2. 处理物品消耗与状态变更
                    if (stack.getCount() > 1) {
                        // 堆叠情况：分离出一个
                        ItemStack dirtySheet = stack.copy();
                        dirtySheet.setCount(1);
                        
                        stack.shrink(1);
                        
                        // 新分离出的床单消耗耐久并变脏
                        dirtySheet.hurtAndBreak(1, player, Player.getSlotForHand(context.getHand()));
                        // 如果没有损坏消失
                        if (!dirtySheet.isEmpty()) {
                            setMessy(dirtySheet, true);
                            if (!player.getInventory().add(dirtySheet)) {
                                player.drop(dirtySheet, false);
                            }
                        }
                    } else {
                        // 单个情况
                        stack.hurtAndBreak(1, player, Player.getSlotForHand(context.getHand()));
                        if (!stack.isEmpty()) {
                            setMessy(stack, true);
                        }
                    }
                } else {
                    // 客户端效果
                    level.playSound(player, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    for (int i = 0; i < 5; i++) {
                        level.addParticle(ParticleTypes.HAPPY_VILLAGER, 
                                pos.getX() + 0.5 + (level.random.nextDouble() - 0.5), 
                                pos.getY() + 0.5, 
                                pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5), 
                                0, 0, 0);
                    }
                }
                
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    public static boolean isMessy(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("Messy");
    }

    public static void setMessy(ItemStack stack, boolean messy) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("Messy", messy);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
