package com.otherworldinn.item;

import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.init.ModEntities;
import com.otherworldinn.init.ModSounds;
import com.otherworldinn.world.entity.projectile.CoinProjectileEntity;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
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
            player.startUsingItem(usedHand);
            return InteractionResultHolder.consume(stack);
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
            return InteractionResult.FAIL;
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

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player) || player.isShiftKeyDown()) {
            return;
        }
        int useTicks = getUseDuration(stack, livingEntity) - timeLeft;
        float power = getPowerForTime(useTicks);
        if (power < 0.1F) {
            return;
        }
        if (!level.isClientSide) {
            CoinProjectileEntity projectile =
                    new CoinProjectileEntity(ModEntities.COIN_PROJECTILE.get(), player, level);
            projectile.setItem(new ItemStack(this));
            projectile.shootFromRotation(
                    player, player.getXRot(), player.getYRot(), 0.0F, power * 1.2F, 0.8F);
            level.addFreshEntity(projectile);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.COIN_PROJECTILE.get(),
                SoundSource.PLAYERS,
                0.5F,
                0.6F / (level.random.nextFloat() * 0.4F + 0.8F));
    }

    private static float getPowerForTime(int charge) {
        float f = (float) charge / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
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
