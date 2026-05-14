package com.otherworldinn.item;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.client.ClientHooks;
import com.otherworldinn.world.dimension.TownDimensions;
import com.otherworldinn.world.map.MapPoint;
import com.otherworldinn.world.map.TownDataProvider;
import com.otherworldinn.world.teleport.TeleportUtils;
import java.util.Optional;
import com.otherworldinn.world.expedition.ExpeditionDimensions;
import com.otherworldinn.world.expedition.ExpeditionService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * 回程卷轴物品
 *
 * <p>在非城镇维度长按使用，可将玩家传送回旅社（城镇维度）。 使用时会播放末影人传送音效，触发传送粒子，并显示不死图腾动画。
 */
public class RecallScrollItem extends Item {

    public RecallScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        // 如果在城镇维度，不允许使用
        if (level.dimension() == TownDimensions.TOWN_LEVEL) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("item.otherworldinn.recall_scroll.fail_in_town"),
                        true);
            }
            return InteractionResultHolder.fail(itemstack);
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(itemstack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 60; // 3秒
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        // 先处理逻辑，因为 teleport 可能会切换维度导致实体失效或变化
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            // 获取目标维度
            ServerLevel townLevel = player.getServer().getLevel(TownDimensions.TOWN_LEVEL);
            if (townLevel != null) {
                if (ExpeditionDimensions.isExpeditionDimension(player.level().dimension())) {
                    ExpeditionService.markDeparted(player.getUUID(), player.getServer());
                }
                // 获取旅社坐标
                Optional<MapPoint> innPoint =
                        TownDataProvider.getPoint(
                                ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "inn"));
                if (innPoint.isPresent()) {
                    Vec3 target = innPoint.get().worldPosition();

                    // 触发不死图腾动画 (仅在客户端执行)
                    // 使用 FMLEnvironment.dist 确保仅在客户端执行
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        ClientHooks.displayRecallScrollActivation(stack);
                    }

                    // 播放传送前的音效 (在当前维度)
                    level.playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.ENDERMAN_TELEPORT,
                            SoundSource.PLAYERS,
                            1.0F,
                            1.0F);

                    TeleportUtils.changeDimensionTo(player, townLevel, target);
                }
            }
        }

        // 消耗物品
        if (livingEntity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return stack;
    }
}
