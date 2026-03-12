package com.otherworldinn.item;

import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 地契物品
 * <p>
 * 用于扩展旅社区域。
 * </p>
 */
public class LandDeedItem extends Item {

    // 旅社扩展的最大边界
    public static final int MAX_REGION_MIN_X = 30;
    public static final int MAX_REGION_MIN_Z = -28;
    public static final int MAX_REGION_MAX_X = 80;
    public static final int MAX_REGION_MAX_Z = 27;

    public LandDeedItem(Properties properties) {
        super(properties);
    }

    public static boolean isWithinBounds(BlockPos pos1, BlockPos pos2) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        return minX >= MAX_REGION_MIN_X && maxX <= MAX_REGION_MAX_X &&
               minZ >= MAX_REGION_MIN_Z && maxZ <= MAX_REGION_MAX_Z;
    }

    /**
     * 计算地契扩展区域的价格
     * <p>
     * 价格 = 有效面积 * 8
     * 有效面积 = 圈选面积 - 已有旅社区域覆盖的面积
     * </p>
     */
    public static int calculatePrice(TeamData team, BlockPos pos1, BlockPos pos2) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        // 如果没有队伍，则全额计算
        if (team == null) {
             return (maxX - minX + 1) * (maxZ - minZ + 1) * 8;
        }

        int validCount = 0;
        List<TeamData.InnRegion> regions = team.getInnRegions();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean overlapped = false;
                for (TeamData.InnRegion region : regions) {
                    if (region.contains(x, z)) {
                        overlapped = true;
                        break;
                    }
                }
                
                if (!overlapped) {
                    validCount++;
                }
            }
        }
        
        return validCount * 8;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        InteractionHand hand = context.getHand();

        if (level.isClientSide || player == null) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            TeamData team = TeamManager.getInstance().getPlayerTeam(serverPlayer);
            if (team == null) {
                player.displayClientMessage(Component.translatable("command.otherworldinn.team.not_in_team"), true);
                return InteractionResult.FAIL;
            }

            ItemStack stack = player.getItemInHand(hand);
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();

            // 标记坐标
            if (!tag.contains("Pos1")) {
                tag.putLong("Pos1", pos.asLong());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.displayClientMessage(Component.translatable("message.otherworldinn.land_deed.pos1_set", pos.toShortString())
                        .withStyle(style -> style.withColor(ModColors.INFO)), true);
            } else if (!tag.contains("Pos2")) {
                // 设置 Pos2 (预览状态)
                tag.putLong("Pos2", pos.asLong());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                
                BlockPos pos1 = BlockPos.of(tag.getLong("Pos1"));
                
                // 检查是否超出最大范围
                if (!isWithinBounds(pos1, pos)) {
                    player.displayClientMessage(Component.translatable("message.otherworldinn.land_deed.fail_out_of_bounds")
                            .withStyle(style -> style.withColor(ModColors.ERROR)), true);
                    // 清除 Pos2 标记，让用户可以重新选择
                    tag.remove("Pos2");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    return InteractionResult.SUCCESS;
                }
                
                // 计算并显示价格提示
                int price = calculatePrice(team, pos1, pos);
                
                if (team.getCoins() >= price) {
                    player.displayClientMessage(Component.translatable("message.otherworldinn.land_deed.pos2_set_with_cost", price)
                            .withStyle(style -> style.withColor(ModColors.INFO)), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.otherworldinn.land_deed.pos2_set_with_cost_fail", price, team.getCoins())
                            .withStyle(style -> style.withColor(ModColors.ERROR)), true);
                }
            } else {
                // 确认添加
                BlockPos pos1 = BlockPos.of(tag.getLong("Pos1"));
                BlockPos pos2 = BlockPos.of(tag.getLong("Pos2"));
                
                // 检查是否超出最大范围
                if (!isWithinBounds(pos1, pos2)) {
                    player.displayClientMessage(Component.translatable("message.otherworldinn.land_deed.fail_out_of_bounds")
                            .withStyle(style -> style.withColor(ModColors.ERROR)), true);
                    // 清除标记，允许重新选择
                    tag.remove("Pos1");
                    tag.remove("Pos2");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    return InteractionResult.FAIL;
                }
                
                // 计算最小最大坐标
                int minX = Math.min(pos1.getX(), pos2.getX());
                int minZ = Math.min(pos1.getZ(), pos2.getZ());
                int maxX = Math.max(pos1.getX(), pos2.getX());
                int maxZ = Math.max(pos1.getZ(), pos2.getZ());
                
                // 计算价格
                int price = calculatePrice(team, pos1, pos2);
                
                if (team.removeCoins(price, serverPlayer.getServer())) {
                    TeamData.InnRegion newRegion = new TeamData.InnRegion(minX, minZ, maxX, maxZ);
                    
                    // 添加区域
                    team.addRegion(newRegion);
                    
                    // 立即同步队伍数据
                    TeamManager.getInstance().syncTeam(team, serverPlayer.getServer());
                    
                    player.displayClientMessage(Component.translatable("message.otherworldinn.land_deed.success", price)
                            .withStyle(style -> style.withColor(ModColors.SUCCESS)), true);
                    
                    // 播放音效
                    level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
                    
                    // 清除标记
                    tag.remove("Pos1");
                    tag.remove("Pos2");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    
                    // 消耗物品 (如果是生存模式)
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                } else {
                    player.displayClientMessage(Component.translatable("message.otherworldinn.land_deed.fail_no_money", price, team.getCoins())
                            .withStyle(style -> style.withColor(ModColors.ERROR)), true);
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
