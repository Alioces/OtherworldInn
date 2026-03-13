package com.otherworldinn.client.renderer;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.item.LandDeedItem;
import com.otherworldinn.item.RoomKeyItem;
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

import java.util.List;

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

        // 检查是否手持房间登记册或地契
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean holdingRegistry = stack.is(ModItems.ROOM_REGISTER.get());
        boolean holdingLandDeed = stack.is(ModItems.LAND_DEED.get());
        boolean holdingRoomKey = stack.is(ModItems.ROOM_KEY.get());
        
        if (!holdingRegistry && !holdingLandDeed && !holdingRoomKey) {
            stack = player.getItemInHand(InteractionHand.OFF_HAND);
            holdingRegistry = stack.is(ModItems.ROOM_REGISTER.get());
            holdingLandDeed = stack.is(ModItems.LAND_DEED.get());
            holdingRoomKey = stack.is(ModItems.ROOM_KEY.get());
        }

        if (!holdingRegistry && !holdingLandDeed && !holdingRoomKey) {
            return;
        }
        
        // 1. 渲染已有房间 (钢蓝色) - 仅当手持房间登记册时
        TeamData team = TeamManager.getInstance().getClientPlayerTeam();
        if (team != null) {
            InnData innData = team.getInnData();
            
            if (holdingRegistry) {
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
            } else if (holdingRoomKey) {
                // 手持房间钥匙：渲染所有房间
                // 绑定的房间：蓝色 (0x4682B4)
                // 其他房间：灰色 (0xA9A9A9)
                java.util.Optional<java.util.UUID> boundRoomUUID = RoomKeyItem.getBoundRoomUUID(stack);
                
                for (RoomData room : innData.getRooms().values()) {
                    boolean isBound = boundRoomUUID.isPresent() && boundRoomUUID.get().equals(room.getUuid());
                    int color = isBound ? 0x4682B4 : 0xA9A9A9;
                    
                    AABB box = new AABB(
                            room.getMinPos().getX(), room.getMinPos().getY(), room.getMinPos().getZ(),
                            room.getMaxPos().getX() + 1.0, room.getMaxPos().getY() + 1.0, room.getMaxPos().getZ() + 1.0
                    );
                    Outliner.getInstance().showAABB(room.getId(), box)
                            .colored(color)
                            .lineWidth(1/16f)
                            .withFaceTextures(AllSpecialTextures.CUTOUT_CHECKERED, AllSpecialTextures.CUTOUT_CHECKERED);
                }
            }

            // 3. 渲染旅社范围 (青色) - 手持房间登记册或地契时都显示
            if (innData.getState() == InnData.InnState.EDIT_MODE || holdingLandDeed) {
                int minBuildHeight = mc.level.getMinBuildHeight();
                int maxBuildHeight = mc.level.getMaxBuildHeight();

                List<TeamData.InnRegion> regions = team.getInnRegions();
                for (int i = 0; i < regions.size(); i++) {
                    TeamData.InnRegion region = regions.get(i);
                    AABB innBox = new AABB(
                        region.minX(), minBuildHeight, region.minZ(),
                        region.maxX() + 1.0, maxBuildHeight, region.maxZ() + 1.0
                    );
                    
                    Outliner.getInstance().showAABB("inn_region_" + i, innBox)
                        .colored(0x97FFFF)
                        .lineWidth(1/16f)
                        .withFaceTextures(AllSpecialTextures.CUTOUT_CHECKERED, AllSpecialTextures.CUTOUT_CHECKERED);
                }
            }
        }

        // 2. 渲染预览区域 (黄绿色/地契颜色)
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        
        if (holdingLandDeed) {
            // 地契渲染逻辑 (Pos1 已定, Pos2 待定/已定)
            if (tag.contains("Pos1")) {
                BlockPos pos1 = BlockPos.of(tag.getLong("Pos1"));
                BlockPos pos2;
                
                if (tag.contains("Pos2")) {
                    // Pos2 已定，显示确认预览
                    pos2 = BlockPos.of(tag.getLong("Pos2"));
                } else {
                    // Pos2 未定，跟随光标
                    HitResult hitResult = mc.hitResult;
                    if (hitResult instanceof BlockHitResult blockHitResult) {
                        pos2 = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
                    } else {
                        return; // 未指向方块时不渲染
                    }
                }
                
                int minBuildHeight = mc.level.getMinBuildHeight();
                int maxBuildHeight = mc.level.getMaxBuildHeight();
                
                BlockPos minPos = new BlockPos(
                        Math.min(pos1.getX(), pos2.getX()),
                        minBuildHeight,
                        Math.min(pos1.getZ(), pos2.getZ())
                );
                BlockPos maxPos = new BlockPos(
                        Math.max(pos1.getX(), pos2.getX()),
                        maxBuildHeight,
                        Math.max(pos1.getZ(), pos2.getZ())
                );
                
                AABB previewBox = new AABB(
                        minPos.getX(), minPos.getY(), minPos.getZ(),
                        maxPos.getX() + 1.0, maxPos.getY() + 1.0, maxPos.getZ() + 1.0
                );
                
                int color;
                if (!LandDeedItem.isWithinBounds(pos1, pos2)) {
                    color = 0xFF0000; // 红色 (超出范围)
                } else {
                    // 检查余额，如果不足显示红色
                    int price = LandDeedItem.calculatePrice(team, pos1, pos2);
                    int coins = team != null ? team.getCoins() : 0;
                    color = (coins >= price) ? 0xFFD700 : 0xFF5555;
                }
                
                Outliner.getInstance().showAABB(PREVIEW_SLOT, previewBox)
                        .colored(color)
                        .lineWidth(1/16f)
                        .withFaceTextures(AllSpecialTextures.CUTOUT_CHECKERED, AllSpecialTextures.CUTOUT_CHECKERED);
            }
        } else if (tag.contains("Pos1")) {
            // 房间登记册渲染逻辑
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
                        .withFaceTextures(AllSpecialTextures.CUTOUT_CHECKERED, AllSpecialTextures.CUTOUT_CHECKERED);
            }
        }
    }
}
