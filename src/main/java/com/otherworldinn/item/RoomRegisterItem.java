package com.otherworldinn.item;

import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.inn.RoomData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

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
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
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


            // 标记坐标
            if (!tag.contains("Pos1")) {
                tag.putLong("Pos1", pos.asLong());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.displayClientMessage(Component.translatable("message.otherworldinn.room_register.pos1_set", pos.toShortString())
                        .withStyle(style -> style.withColor(ModColors.INFO)), true);
            } else {
                BlockPos pos1 = BlockPos.of(tag.getLong("Pos1"));
                
                // 计算最小最大坐标
                BlockPos minPos = new BlockPos(Math.min(pos1.getX(), pos.getX()), Math.min(pos1.getY(), pos.getY()), Math.min(pos1.getZ(), pos.getZ()));
                BlockPos maxPos = new BlockPos(Math.max(pos1.getX(), pos.getX()), Math.max(pos1.getY(), pos.getY()), Math.max(pos1.getZ(), pos.getZ()));
                
                // 判定是否有效
                RoomData.ValidationResult result = RoomData.validate(minPos, maxPos, level, team);
                if (result.isSuccess()) {
                    // 生成新ID
                    int newId = innData.getRooms().keySet().stream().max(Integer::compareTo).orElse(0) + 1;
                    
                    RoomData newRoom = new RoomData(newId, minPos, maxPos);
                    // 统计床位并设置最大旅客数
                    int bedCount = RoomData.countBeds(minPos, maxPos, level);
                    newRoom.setMaxGuests(bedCount);
                    
                    innData.addRoom(newRoom);
                    innData.calculateRoomStats(newId, level);

                    // 立即同步队伍数据
                    TeamManager.getInstance().syncTeam(team, serverPlayer.getServer());
                    
                    player.displayClientMessage(Component.translatable("message.otherworldinn.room_register.create_success", newId)
                            .withStyle(style -> style.withColor(ModColors.SUCCESS)), true);
                    
                    // 清除标记
                    tag.remove("Pos1");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                } else {
                    player.displayClientMessage(Component.translatable(result.getTranslationKey())
                            .withStyle(style -> style.withColor(ModColors.ERROR)), true);
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
        return InteractionResultHolder.pass(player.getItemInHand(usedHand));
    }
}
