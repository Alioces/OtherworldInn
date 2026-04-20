package com.otherworldinn.item;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.util.AdvancementUtils;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID)
public class InnUpgradeVoucherItem extends Item {
    private static final int STAR_STEP_TICKS = 4; // 0.2s
    // 十二平均律大调音阶（半音数，首个音作为主音）
    private static final int[] MAJOR_SCALE_SEMITONES = {0, 2, 4, 5, 7, 9, 11, 12};
    private static final Map<UUID, StarAnimationState> STAR_ANIMATIONS = new HashMap<>();

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
        boolean upgraded;
        if (serverPlayer.getAbilities().instabuild) {
            // 创造模式始终判定为成功，不受常规升星条件限制
            innData.setRating(Math.min(5, innData.getRating() + 1));
            upgraded = true;
        } else {
            upgraded = innData.checkLevelUp();
        }
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
        AdvancementUtils.awardInnRatingProgress(serverPlayer, innData.getRating());
        int upgradedRating = Math.max(1, Math.min(5, innData.getRating()));
        startUpgradeStarAnimation(serverPlayer, upgradedRating);
        return InteractionResultHolder.success(stack);
    }

    private static void startUpgradeStarAnimation(ServerPlayer player, int targetRating) {
        int clampedTarget = Math.max(1, Math.min(5, targetRating));
        STAR_ANIMATIONS.put(
                player.getUUID(),
                new StarAnimationState(clampedTarget, player.server.getTickCount(), 0));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (STAR_ANIMATIONS.isEmpty()) {
            return;
        }
        long currentTick = event.getServer().getTickCount();
        Iterator<Map.Entry<UUID, StarAnimationState>> iterator = STAR_ANIMATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, StarAnimationState> entry = iterator.next();
            StarAnimationState state = entry.getValue();
            if (currentTick < state.nextDisplayTick()) {
                continue;
            }

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isRemoved()) {
                iterator.remove();
                continue;
            }

            int starsToShow = state.currentStars();
            sendUpgradeStarMessage(player, starsToShow);

            if (starsToShow > 0) {
                int semitone =
                        MAJOR_SCALE_SEMITONES[
                                Math.min(starsToShow - 1, MAJOR_SCALE_SEMITONES.length - 1)];
                float pitch = (float) Math.pow(2.0D, semitone / 12.0D);
                player.level()
                        .playSound(
                                null,
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                SoundEvents.NOTE_BLOCK_PLING.value(),
                                SoundSource.PLAYERS,
                                0.8F,
                                pitch);
            }

            if (starsToShow >= state.targetStars()) {
                iterator.remove();
                continue;
            }

            entry.setValue(
                    new StarAnimationState(
                            state.targetStars(), currentTick + STAR_STEP_TICKS, starsToShow + 1));
        }
    }

    private static void sendUpgradeStarMessage(ServerPlayer player, int starCount) {
        String stars = starCount == 0 ? "" : "\uE005".repeat(starCount);
        player.displayClientMessage(
                Component.literal(stars + " ")
                        .append(
                                Component.translatable("message.otherworldinn.inn_upgrade_voucher.success")
                                        .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" " + stars)),
                true);
    }

    private record StarAnimationState(int targetStars, long nextDisplayTick, int currentStars) {}
}
