package com.otherworldinn.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.otherworldinn.OtherworldInn;
import com.otherworldinn.client.CameraHandler;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
public class InnRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!CameraHandler.isMapMode()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        TeamData team = TeamManager.getInstance().getPlayerTeam(player);
        if (team == null) return;

        BlockPos center = team.getInnZoneCenter();
        int radius = team.getInnZoneRadius();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // 移动到世界坐标原点
        Vec3 cameraPos = event.getCamera().getPosition();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // 渲染旅社范围 (水平矩形)
        boolean isEditMode = team.getInnData().isEditMode();
        renderInnZone(poseStack, center, radius, isEditMode);

        poseStack.popPose();
    }

    private static void renderInnZone(PoseStack poseStack, BlockPos center, int radius, boolean isEditMode) {
        Tesselator tesselator = Tesselator.getInstance();
        
        // 渲染设置
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest(); // 穿透渲染
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();

        // 1. 渲染半透明填充
        // Y 轴高度稍微抬高一点，防止 Z-fighting (虽然在正交视角下可能不明显，但为了以防万一)
        float y = 70.01f; // 假设地面在 70
        float minX = center.getX() - radius;
        float maxX = center.getX() + radius + 1; // +1 覆盖完整方块
        float minZ = center.getZ() - radius;
        float maxZ = center.getZ() + radius + 1;
        
        // 颜色设置
        float red, green, blue;
        if (isEditMode) {
            // 编辑模式：蓝色半透明
            red = 0.0f;
            green = 0.0f;
            blue = 1.0f;
        } else {
            // 普通模式：绿色半透明
            red = 0.0f;
            green = 1.0f;
            blue = 0.0f;
        }
        float alpha = 0.2f;

        Matrix4f matrix = poseStack.last().pose();
        
        // 绘制填充
        try {
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            
            buffer.addVertex(matrix, minX, y, minZ).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, minX, y, maxZ).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, maxX, y, maxZ).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, maxX, y, minZ).setColor(red, green, blue, alpha);
            
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } catch (Exception e) {
            // 忽略可能的异常 (API 变动)
        }

        // 绘制边框 (线条)
        RenderSystem.lineWidth(2.0f);
        alpha = 0.8f;
        
        try {
            BufferBuilder lineBuffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            
            lineBuffer.addVertex(matrix, minX, y, minZ).setColor(red, green, blue, alpha);
            lineBuffer.addVertex(matrix, minX, y, maxZ).setColor(red, green, blue, alpha);
            lineBuffer.addVertex(matrix, maxX, y, maxZ).setColor(red, green, blue, alpha);
            lineBuffer.addVertex(matrix, maxX, y, minZ).setColor(red, green, blue, alpha);
            lineBuffer.addVertex(matrix, minX, y, minZ).setColor(red, green, blue, alpha); // 闭合
            
            BufferUploader.drawWithShader(lineBuffer.buildOrThrow());
        } catch (Exception e) {
            // 忽略可能的异常
        }
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }
}
