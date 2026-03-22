package com.otherworldinn.item;

import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class CoinItem extends Item {
    public CoinItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        return depositOne(serverPlayer, usedHand)
                ? InteractionResultHolder.success(stack)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }
        return depositOne(serverPlayer, context.getHand())
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
    }

    private boolean depositOne(ServerPlayer player, InteractionHand hand) {
        TeamData team = TeamManager.getInstance().getPlayerTeam(player);
        if (team == null) {
            player.displayClientMessage(
                    Component.translatable("message.otherworldinn.coin.no_team")
                            .withStyle(style -> style.withColor(ModColors.ERROR)),
                    true);
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        team.addCoins(1, player.getServer());
        TeamManager.getInstance().syncTeam(team, player.getServer());
        return true;
    }
}
