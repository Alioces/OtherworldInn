package com.otherworldinn.item;

import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import com.otherworldinn.world.teleport.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SpaceSphereItem extends Item {
    public SpaceSphereItem(Properties properties) {
        super(properties);
    }

    private static final int RANDOM_TELEPORT_RADIUS = 256;

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

        if (level.dimension() != TownDimensions.TOWN_LEVEL) {
            ServerLevel currentLevel = (ServerLevel) level;
            BlockPos playerPos = serverPlayer.blockPosition();
            RandomSource random = currentLevel.getRandom();
            int offsetX = random.nextInt(RANDOM_TELEPORT_RADIUS * 2 + 1) - RANDOM_TELEPORT_RADIUS;
            int offsetZ = random.nextInt(RANDOM_TELEPORT_RADIUS * 2 + 1) - RANDOM_TELEPORT_RADIUS;
            BlockPos randomBase = playerPos.offset(offsetX, 0, offsetZ);
            BlockPos safePos = TeleportUtils.findSafeSpawnPos(currentLevel, randomBase);
            serverPlayer.teleportTo(
                    currentLevel,
                    safePos.getX() + 0.5,
                    safePos.getY(),
                    safePos.getZ() + 0.5,
                    serverPlayer.getYRot(),
                    serverPlayer.getXRot());
            level.playSound(
                    null,
                    safePos.getX() + 0.5,
                    safePos.getY(),
                    safePos.getZ() + 0.5,
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);

            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.success(stack);
        }

        TeamData team = TeamManager.getInstance().getPlayerTeam(serverPlayer);
        if (team == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.otherworldinn.space_sphere.no_team")
                            .withStyle(style -> style.withColor(ModColors.ERROR)),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        if (team.isTeleportUnlocked()) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.otherworldinn.space_sphere.already_unlocked")
                            .withStyle(style -> style.withColor(ModColors.ERROR)),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        TeamManager.getInstance().setTeamTeleportUnlocked(team, true, serverPlayer.getServer());

        Component broadcast =
                Component.translatable("message.otherworldinn.space_sphere.teleport_unlocked")
                        .withStyle(style -> style.withColor(ModColors.INFO));

        team.getMembers()
                .forEach(
                        memberId -> {
                            ServerPlayer member =
                                    serverPlayer.getServer().getPlayerList().getPlayer(memberId);
                            if (member != null) {
                                member.displayClientMessage(broadcast, false);
                                member.playNotifySound(
                                        SoundEvents.BEACON_ACTIVATE,
                                        SoundSource.PLAYERS,
                                        1.0F,
                                        1.0F);
                            }
                        });

        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }
}
