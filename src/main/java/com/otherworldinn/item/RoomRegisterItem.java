package com.otherworldinn.item;

import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.inn.RoomData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 房间登记册物品
 * <p>
 * 用于在编辑模式下创建和删除房间。
 * </p>
 */
public class RoomRegisterItem extends Item {

    public RoomRegisterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        InteractionHand hand = context.getHand();

        if (level.isClientSide || player == null || hand != InteractionHand.OFF_HAND) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            TeamData team = TeamManager.getInstance().getPlayerTeam(serverPlayer);
            if (team == null || !team.getInnData().isEditMode()) {
                player.displayClientMessage(Component.translatable("message.otherworldinn.room_register.not_edit_mode"), true);
                return InteractionResult.FAIL;
            }

            InnData innData = team.getInnData();
            ItemStack stack = player.getItemInHand(hand);
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();

            // 潜行状态下删除房间
            if (player.isShiftKeyDown()) {
                RoomData room = innData.getRoomAt(pos);
                if (room != null) {
                    innData.removeRoom(room.getId());
                    player.displayClientMessage(Component.translatable("message.otherworldinn.room_register.remove_success", room.getId()), true);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }

            // 标记坐标
            if (!tag.contains("Pos1")) {
                tag.putLong("Pos1", pos.asLong());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.displayClientMessage(Component.translatable("message.otherworldinn.room_register.pos1_set", pos.toShortString()), true);
            } else {
                BlockPos pos1 = BlockPos.of(tag.getLong("Pos1"));
                
                // 计算最小最大坐标
                BlockPos minPos = new BlockPos(Math.min(pos1.getX(), pos.getX()), Math.min(pos1.getY(), pos.getY()), Math.min(pos1.getZ(), pos.getZ()));
                BlockPos maxPos = new BlockPos(Math.max(pos1.getX(), pos.getX()), Math.max(pos1.getY(), pos.getY()), Math.max(pos1.getZ(), pos.getZ()));
                
                // 判定是否有效
                if (RoomData.isRoomValid(minPos, maxPos, level, team)) {
                    // 生成新ID (简单的自增逻辑，实际项目中可能需要更复杂的ID管理)
                    int newId = innData.getRooms().keySet().stream().max(Integer::compareTo).orElse(0) + 1;
                    
                    RoomData newRoom = new RoomData(newId, minPos, maxPos);
                    innData.addRoom(newRoom);
                    innData.calculateRoomStats(newId, level);
                    
                    player.displayClientMessage(Component.translatable("message.otherworldinn.room_register.create_success", newId), true);
                    
                    // 清除标记
                    tag.remove("Pos1");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                } else {
                    player.displayClientMessage(Component.translatable("message.otherworldinn.room_register.invalid_room"), true);
                    // 也可以选择不清除 Pos1，让玩家重新选择第二个点
                    tag.remove("Pos1");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
            }
            
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide || usedHand != InteractionHand.OFF_HAND) {
            return InteractionResultHolder.pass(player.getItemInHand(usedHand));
        }

        if (player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()) {
            TeamData team = TeamManager.getInstance().getPlayerTeam(serverPlayer);
            if (team != null && team.getInnData().isEditMode()) {
                InnData innData = team.getInnData();
                BlockPos pos = player.blockPosition();
                RoomData room = innData.getRoomAt(pos);
                
                if (room != null) {
                    innData.removeRoom(room.getId());
                    player.displayClientMessage(Component.translatable("message.otherworldinn.room_register.remove_success", room.getId()), true);
                    return InteractionResultHolder.success(player.getItemInHand(usedHand));
                }
            }
        }

        return InteractionResultHolder.pass(player.getItemInHand(usedHand));
    }
}
