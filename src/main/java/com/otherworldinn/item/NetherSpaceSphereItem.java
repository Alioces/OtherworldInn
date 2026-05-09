package com.otherworldinn.item;

import com.otherworldinn.world.teleport.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetherSpaceSphereItem extends Item {
    public NetherSpaceSphereItem(Properties properties) {
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

        ServerLevel nether = serverPlayer.getServer().getLevel(Level.NETHER);
        if (nether == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.otherworldinn.space_sphere.target_unavailable"),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        BlockPos targetBase =
                BlockPos.containing(
                        serverPlayer.getX() / 8.0D, serverPlayer.getY(), serverPlayer.getZ() / 8.0D);
        BlockPos randomizedBase = TeleportUtils.getRandomizedNetherBase(nether, targetBase);
        BlockPos safePos = TeleportUtils.findSafeSpawnPosInNether(nether, randomizedBase);
        TeleportUtils.changeDimensionTo(serverPlayer, nether, safePos);

        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }
}
