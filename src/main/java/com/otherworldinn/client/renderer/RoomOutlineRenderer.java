package com.otherworldinn.client.renderer;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.world.inn.InnData;
import com.otherworldinn.world.inn.RoomData;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import com.simibubi.create.AllSpecialTextures;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 房间轮廓渲染器
 * <p>
 * 使用 Create 模组的 Outliner API 渲染房间边框和预览区域。
 * 仅在客户端运行。
 * </p>
 */
@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
public class RoomOutlineRenderer {

    private static final Object PREVIEW_SLOT = "room_preview";

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // 检查是否手持房间登记册
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean holdingRegistry = stack.is(ModItems.ROOM_REGISTER.get());
        if (!holdingRegistry) {
            stack = player.getItemInHand(InteractionHand.OFF_HAND);
            holdingRegistry = stack.is(ModItems.ROOM_REGISTER.get());
        }

        if (!holdingRegistry) {
            return;
        }
        

        // 1. 渲染已有房间 (钢蓝色)
        TeamData team = TeamManager.getInstance().getClientPlayerTeam();
        if (team != null) {
            InnData innData = team.getInnData();
            for (RoomData room : innData.getRooms().values()) {
                AABB box = new AABB(
                        room.getMinPos().getX(), room.getMinPos().getY(), room.getMinPos().getZ(),
                        room.getMaxPos().getX() + 1.0, room.getMaxPos().getY() + 1.0, room.getMaxPos().getZ() + 1.0
                );
                Outliner.getInstance().showAABB(room.getId(), box)
                        .colored(0x4682B4)
                        .lineWidth(1/16f)
                        .withFaceTextures(AllSpecialTextures.CUTOUT_CHECKERED, AllSpecialTextures.CUTOUT_CHECKERED)
                        ;
            }
        }

        // 2. 渲染预览区域 (黄绿色)
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("Pos1")) {
            BlockPos pos1 = BlockPos.of(tag.getLong("Pos1"));
            
            HitResult hitResult = mc.hitResult;
            if (hitResult instanceof BlockHitResult blockHitResult) {
                // 确定 Pos2 (当前所指方块的相邻面)
                BlockPos pos2 = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
                
                BlockPos minPos = new BlockPos(
                        Math.min(pos1.getX(), pos2.getX()),
                        Math.min(pos1.getY(), pos2.getY()),
                        Math.min(pos1.getZ(), pos2.getZ())
                );
                BlockPos maxPos = new BlockPos(
                        Math.max(pos1.getX(), pos2.getX()),
                        Math.max(pos1.getY(), pos2.getY()),
                        Math.max(pos1.getZ(), pos2.getZ())
                );
                
                AABB previewBox = new AABB(
                        minPos.getX(), minPos.getY(), minPos.getZ(),
                        maxPos.getX() + 1.0, maxPos.getY() + 1.0, maxPos.getZ() + 1.0
                );
                
                Outliner.getInstance().showAABB(PREVIEW_SLOT, previewBox)
                        .colored(0xC0FF3E)
                        .lineWidth(1/16f)
                        .withFaceTextures(AllSpecialTextures.CUTOUT_CHECKERED, AllSpecialTextures.CUTOUT_CHECKERED)
                        ;
            }
        }
    }
}
