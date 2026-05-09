package com.otherworldinn.item;

import com.otherworldinn.world.teleport.TeleportUtils;
import java.lang.reflect.Method;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class EndSpaceSphereItem extends Item {
    public EndSpaceSphereItem(Properties properties) {
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

        ServerLevel endLevel = serverPlayer.getServer().getLevel(Level.END);
        if (endLevel == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.otherworldinn.space_sphere.target_unavailable"),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        Vec3 target = new Vec3(100.5D, 50.0D, 0.5D);
        BlockPos platformBase = BlockPos.containing(target).below();
        this.createEndPlatform(endLevel, platformBase);
        TeleportUtils.changeDimensionTo(serverPlayer, endLevel, target);

        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }

    private void createEndPlatform(ServerLevel level, BlockPos platformBase) {
        try {
            Method createMethod =
                    ServerPlayer.class.getDeclaredMethod(
                            "createEndPlatform", ServerLevel.class, BlockPos.class);
            createMethod.setAccessible(true);
            createMethod.invoke(null, level, platformBase);
            return;
        } catch (ReflectiveOperationException ignored) {
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos floorPos = platformBase.offset(x, 0, z);
                level.setBlockAndUpdate(floorPos, Blocks.OBSIDIAN.defaultBlockState());
                level.setBlockAndUpdate(floorPos.above(), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(floorPos.above(2), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(floorPos.above(3), Blocks.AIR.defaultBlockState());
            }
        }
    }
}
