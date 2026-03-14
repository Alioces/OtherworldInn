package com.otherworldinn.client.event;

import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import com.simibubi.create.content.redstone.deskBell.DeskBellBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class DeskBellClientHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide) return;

        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof DeskBellBlockEntity) {
            Player player = event.getEntity();
            TeamData team = TeamManager.getInstance().getClientPlayerTeam();

            if (player.isShiftKeyDown()) {
                return;
            }
            if (team == null) return;

            // 检查是否在旅社区域内
            boolean inside = false;
            for (TeamData.InnRegion region : team.getInnRegions()) {
                if (region.contains(pos)) {
                    inside = true;
                    break;
                }
            }
            if (!inside) return;

            InnData.InnState state = team.getInnData().getState();
            MutableComponent message;
            int color;

            switch (state) {
                case OPEN:
                    message = Component.translatable("message.otherworldinn.desk_bell.status.open");
                    color = 0x00FF7F;
                    break;
                case CLOSED:
                    message =
                            Component.translatable("message.otherworldinn.desk_bell.status.closed");
                    color = 0xFF6A6A;
                    break;
                case EDIT_MODE:
                    message =
                            Component.translatable(
                                    "message.otherworldinn.desk_bell.status.edit_mode");
                    color = 0x1E90FF;
                    break;
                default:
                    return;
            }

            Minecraft.getInstance()
                    .gui
                    .setOverlayMessage(message.withStyle(Style.EMPTY.withColor(color)), false);
        }
    }
}
