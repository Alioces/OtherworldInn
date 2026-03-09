package com.otherworldinn.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.ClientConfig;
import com.otherworldinn.init.ModKeyBindings;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import com.otherworldinn.client.gui.MapViewScreen;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
/**
 * 摄像机处理器
 */
public class CameraHandler {

    private static boolean isStrategyMode = false;
    private static Entity dummyCameraEntity;
    private static Entity originalCameraEntity;
    
    private static boolean isTransitioning = false;
    private static float transitionProgress = 0.0f;
    private static final float TRANSITION_DURATION = 5.0f;
    private static boolean transitionToStrategy = false;
    
    private static Vec3 startPos;
    private static Vec3 targetPos;
    private static float startYaw, startPitch;
    private static float targetYaw, targetPitch;

    /**
     * 处理按键输入事件
     * @param event 按键事件
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (ModKeyBindings.TOGGLE_STRATEGY_MODE.consumeClick()) {
            if (isStrategyMode) {
                disableStrategyMode();
            } else {
                enableStrategyMode();
            }
        }
    }

    /**
     * 打开地图
     */
    public static void enableStrategyMode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        isTransitioning = true;
        transitionProgress = 0.0f;
        transitionToStrategy = true;
        
        originalCameraEntity = mc.getCameraEntity();
        
        if (dummyCameraEntity == null || !dummyCameraEntity.isAlive()) {
            dummyCameraEntity = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            dummyCameraEntity.setInvisible(true);
            dummyCameraEntity.setNoGravity(true);
            mc.level.addEntity(dummyCameraEntity);
        }
        double x = ClientConfig.INSTANCE.cameraX.get();
        double y = ClientConfig.INSTANCE.cameraY.get();
        double z = ClientConfig.INSTANCE.cameraZ.get();
        targetPos = new Vec3(x, y, z);
        targetYaw = ClientConfig.INSTANCE.cameraYaw.get().floatValue();
        targetPitch = ClientConfig.INSTANCE.cameraPitch.get().floatValue();
        
        startPos = new Vec3(x, y - 5.0, z);
        startYaw = targetYaw;
        startPitch = targetPitch;
        
        updateDummyEntity(startPos, startYaw, startPitch);
        
        mc.setCameraEntity(dummyCameraEntity);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.player.setInvisible(false);
        
        mc.setScreen(new MapViewScreen());
        
        isStrategyMode = true;
    }

    /**
     * 关闭地图
     */
    public static void disableStrategyMode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        isTransitioning = true;
        transitionProgress = 0.0f;
        transitionToStrategy = false;
        
        double x = ClientConfig.INSTANCE.cameraX.get();
        double y = ClientConfig.INSTANCE.cameraY.get();
        double z = ClientConfig.INSTANCE.cameraZ.get();
        startPos = new Vec3(x, y, z);
        startYaw = ClientConfig.INSTANCE.cameraYaw.get().floatValue();
        startPitch = ClientConfig.INSTANCE.cameraPitch.get().floatValue();
        
        targetPos = new Vec3(x, y - 5.0, z);
        targetYaw = startYaw;
        targetPitch = startPitch;
        
        if (mc.screen instanceof MapViewScreen mapScreen) {
            // 不立即关闭屏幕，而是通知屏幕开始关闭动画
            mapScreen.startClosing();
        } else {
            // 如果不是地图屏幕（异常情况），则直接关闭
            if (mc.screen instanceof MapViewScreen) {
                mc.setScreen(null);
            }
        }
    }
    
    /**
     * 更新虚拟摄像机实体的位置和旋转
     * @param pos 位置
     * @param yaw 偏航角
     * @param pitch 俯仰角
     */
    private static void updateDummyEntity(Vec3 pos, float yaw, float pitch) {
        if (dummyCameraEntity == null) return;
        dummyCameraEntity.setPos(pos.x, pos.y, pos.z);
        dummyCameraEntity.setYRot(yaw);
        dummyCameraEntity.setXRot(pitch);
        dummyCameraEntity.yRotO = yaw;
        dummyCameraEntity.xRotO = pitch;
        if (dummyCameraEntity instanceof LivingEntity living) {
            living.yHeadRot = yaw;
            living.yHeadRotO = yaw;
            living.yBodyRot = yaw;
            living.yBodyRotO = yaw;
        }
    }

    /**
     * 阻止第一人称手部渲染
     */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isStrategyMode) {
            event.setCanceled(true);
        }
    }



    /**
     * 应用正交投影矩阵
     * <p>
     * 在 {@link RenderLevelStageEvent.Stage#AFTER_SKY} 阶段修改投影矩阵，
     * </p>
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && isStrategyMode) {
            double size = ClientConfig.INSTANCE.orthoSize.get();
            double aspectRatio = (double) Minecraft.getInstance().getWindow().getWidth() / (double) Minecraft.getInstance().getWindow().getHeight();
            
            double near = -256.0;
            double far = 256.0;
            
            Matrix4f ortho = new Matrix4f();
            ortho.setOrtho(
                    (float)(-size * aspectRatio), 
                    (float)(size * aspectRatio), 
                    (float)(-size), 
                    (float)(size), 
                    (float)near, 
                    (float)far
            );
            
            RenderSystem.setProjectionMatrix(ortho, VertexSorting.DISTANCE_TO_ORIGIN);
            
            event.getProjectionMatrix().set(ortho);
        }
    }
    
    /**
     * 隐藏准星
     */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (isStrategyMode) {
             if (event.getName().getPath().equals("crosshair")) {
                 event.setCanceled(true);
             }
        }
    }

    /**
     * 客户端每刻更新
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (isTransitioning) {
            transitionProgress += 1.0f / TRANSITION_DURATION;
            if (transitionProgress >= 1.0f) {
                transitionProgress = 1.0f;
                isTransitioning = false;
                
                if (!transitionToStrategy) {
                    finishDisableStrategyMode();
                    return;
                }
            }
            
            float t;
            if (transitionToStrategy) {
                float f = 1.0f - transitionProgress;
                t = 1.0f - f * f * f;
            } else {
                t = transitionProgress * transitionProgress * transitionProgress;
            }
            
            double x = Mth.lerp(t, startPos.x, targetPos.x);
            double y = Mth.lerp(t, startPos.y, targetPos.y);
            double z = Mth.lerp(t, startPos.z, targetPos.z);
            
            float yaw = Mth.rotLerp(t, startYaw, targetYaw);
            float pitch = Mth.rotLerp(t, startPitch, targetPitch);
            
            updateDummyEntity(new Vec3(x, y, z), yaw, pitch);
        } else if (isStrategyMode) {
            Minecraft mc = Minecraft.getInstance();
            if (dummyCameraEntity != null) {
                double x = ClientConfig.INSTANCE.cameraX.get();
                double y = ClientConfig.INSTANCE.cameraY.get();
                double z = ClientConfig.INSTANCE.cameraZ.get();
                float yaw = ClientConfig.INSTANCE.cameraYaw.get().floatValue();
                float pitch = ClientConfig.INSTANCE.cameraPitch.get().floatValue();
                
                updateDummyEntity(new Vec3(x, y, z), yaw, pitch);
                
                if (mc.getCameraEntity() != dummyCameraEntity) {
                    mc.setCameraEntity(dummyCameraEntity);
                }
            }
        }
    }
    
    /**
     * 获取虚拟摄像机实体的当前位置
     * @return 位置，如果虚拟实体不存在则返回 null
     */
    public static Vec3 getDummyCameraPos() {
        return dummyCameraEntity != null ? dummyCameraEntity.position() : null;
    }
    
    /**
     * 获取目标偏航角
     */
    public static float getTargetYaw() {
        return targetYaw;
    }
    
    /**
     * 获取目标俯仰角
     */
    public static float getTargetPitch() {
        return targetPitch;
    }

    /**
     * 完成退出地图视角
     */
    private static void finishDisableStrategyMode() {
        Minecraft mc = Minecraft.getInstance();
        isStrategyMode = false;
        
        if (originalCameraEntity != null) {
            mc.setCameraEntity(originalCameraEntity);
        } else {
            mc.setCameraEntity(mc.player);
        }
        
        if (dummyCameraEntity != null) {
            dummyCameraEntity.remove(Entity.RemovalReason.DISCARDED);
            dummyCameraEntity = null;
        }
    }
    
}
