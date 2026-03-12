package com.otherworldinn.item;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.inn.RoomData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * 房间钥匙
 * <p>
 * 用于绑定特定房间。
 * 右键房间内方块绑定，左键点击取消绑定。
 * </p>
 */
public class RoomKeyItem extends Item {

    public RoomKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.FAIL;

        if (level instanceof ServerLevel serverLevel) {
            TeamData team = TeamManager.getInstance().getTeamAt(pos, serverLevel.getServer());
            if (team != null) {
                RoomData room = team.getInnData().getRoomAt(pos);
                if (room != null) {
                    // 绑定到该房间
                    bindRoom(stack, room.getId());
                    player.displayClientMessage(Component.translatable("message.otherworldinn.room_key.bound", room.getId()), true);
                    return InteractionResult.SUCCESS;
                }
            }
            
            player.displayClientMessage(Component.translatable("message.otherworldinn.room_key.no_room"), true);
        }

        return InteractionResult.FAIL;
    }

    @Override
    public Component getName(ItemStack stack) {
        Optional<Integer> roomId = getBoundRoomId(stack);
        if (roomId.isPresent()) {
            return Component.translatable("item.otherworldinn.room_key.bound", roomId.get());
        }
        return super.getName(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getBoundRoomId(stack).isPresent();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        getBoundRoomId(stack).ifPresent(roomId -> {
            // 从客户端缓存获取房间信息
            TeamData team = TeamManager.getInstance().getClientPlayerTeam();
            if (team != null) {
                RoomData room = team.getInnData().getRoom(roomId);
                if (room != null) {
                    // 房间ID
                    tooltipComponents.add(Component.translatable("tooltip.otherworldinn.room_key.room_id", roomId).withStyle(ChatFormatting.GOLD));
                    
                    // 位置
                    tooltipComponents.add(Component.translatable("tooltip.otherworldinn.room_key.pos", 
                            room.getMinPos().toShortString(), room.getMaxPos().toShortString()).withStyle(ChatFormatting.GRAY));
                    
                    // 价格
                    int rating = team.getInnData().getRating();
                    int price = room.getBedPrice(rating);
                    tooltipComponents.add(Component.translatable("tooltip.otherworldinn.room_key.price", price).withStyle(ChatFormatting.YELLOW));
                    
                    // 床位数
                    int maxGuests = room.getMaxGuests();
                    int currentGuests = room.getCurrentGuests().size();
                    tooltipComponents.add(Component.translatable("tooltip.otherworldinn.room_key.beds", currentGuests, maxGuests).withStyle(ChatFormatting.BLUE));
                }
            }
        });
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    // --- 辅助方法 ---

    public static void bindRoom(ItemStack stack, int roomId) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("RoomId", roomId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void unbindRoom(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove("RoomId");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static Optional<Integer> getBoundRoomId(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
        if (tag != null && tag.contains("RoomId")) {
            return Optional.of(tag.getInt("RoomId"));
        }
        return Optional.empty();
    }
}
