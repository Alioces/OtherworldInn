package com.otherworldinn.item;

import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class InnUpgradeVoucherItem extends Item {
    public InnUpgradeVoucherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        TeamData team = TeamManager.getInstance().getPlayerTeam(serverPlayer);
        if (team == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.otherworldinn.inn_upgrade_voucher.no_team")
                            .withStyle(style -> style.withColor(ModColors.ERROR)),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        InnData innData = team.getInnData();
        int beforeRating = innData.getRating();
        boolean upgraded = innData.checkLevelUp();
        if (!upgraded) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.otherworldinn.inn_upgrade_voucher.fail")
                            .withStyle(style -> style.withColor(ModColors.ERROR)),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        TeamManager.getInstance().syncTeam(team, serverPlayer.getServer());
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        serverPlayer.displayClientMessage(
                Component.translatable(
                                "message.otherworldinn.inn_upgrade_voucher.success",
                                beforeRating,
                                innData.getRating())
                        .withStyle(style -> style.withColor(ModColors.INFO)),
                true);
        serverPlayer.level()
                .playSound(
                        null,
                        serverPlayer.getX(),
                        serverPlayer.getY(),
                        serverPlayer.getZ(),
                        SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS,
                        0.8F,
                        1.2F);
        return InteractionResultHolder.success(stack);
    }
}
