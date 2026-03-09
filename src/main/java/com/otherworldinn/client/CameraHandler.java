package com.otherworldinn.client;

import com.mojang.blaze3d.systems.RenderSystem;
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
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import com.otherworldinn.client.gui.MapViewScreen;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.minecraft.client.gui.Gui;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
public class CameraHandler {

    private static boolean isStrategyMode = false;
    private static Entity dummyCameraEntity;
    private static Entity originalCameraEntity;
    
    // Animation fields
    private static boolean isTransitioning = false;
    private static float transitionProgress = 0.0f;
    private static final float TRANSITION_DURATION = 5.0f;
    private static boolean transitionToStrategy = false;
    
    private static Vec3 startPos;
    private static Vec3 targetPos;
    private static float startYaw, startPitch;
    private static float targetYaw, targetPitch;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (ModKeyBindings.TOGGLE_STRATEGY_MODE.consumeClick()) {
            if (isStrategyMode) {
                // 如果已经在地图模式，则关闭它（通常通过 GUI 关闭，但这里作为备份）
                disableStrategyMode();
            } else {
                enableStrategyMode();
            }
        }
    }

    public static void enableStrategyMode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        // Start transition
        isTransitioning = true;
        transitionProgress = 0.0f;
        transitionToStrategy = true;
        
        originalCameraEntity = mc.getCameraEntity();
        
        // Create dummy entity if needed
        if (dummyCameraEntity == null || !dummyCameraEntity.isAlive()) {
            dummyCameraEntity = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            dummyCameraEntity.setInvisible(true);
            dummyCameraEntity.setNoGravity(true);
            mc.level.addEntity(dummyCameraEntity);
        }

        
        // Setup target position
        double x = ClientConfig.INSTANCE.cameraX.get();
        double y = ClientConfig.INSTANCE.cameraY.get();
        double z = ClientConfig.INSTANCE.cameraZ.get();
        targetPos = new Vec3(x, y, z);
        targetYaw = ClientConfig.INSTANCE.cameraYaw.get().floatValue();
        targetPitch = ClientConfig.INSTANCE.cameraPitch.get().floatValue();
        
        // 修改：初始位置不是玩家位置，而是目标位置减去一定高度（例如 -5）
        // 这样效果是从下方升起
        startPos = new Vec3(x, y - 5.0, z);
        startYaw = targetYaw;
        startPitch = targetPitch;
        
        // Set initial dummy state to start position
        updateDummyEntity(startPos, startYaw, startPitch);
        
        mc.setCameraEntity(dummyCameraEntity);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.player.setInvisible(false);
        
        // Don't set screen yet, wait for transition? Or set immediately?
        // Set immediately to block input, but maybe we want to see the transition
        // MapViewScreen handles input blocking.
        // Let's set it immediately.
        mc.setScreen(new MapViewScreen());
        
        isStrategyMode = true;
    }

    public static void disableStrategyMode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Start transition back
        isTransitioning = true;
        transitionProgress = 0.0f;
        transitionToStrategy = false;
        
        // Current dummy pos is start (which is the high position)
        double x = ClientConfig.INSTANCE.cameraX.get();
        double y = ClientConfig.INSTANCE.cameraY.get();
        double z = ClientConfig.INSTANCE.cameraZ.get();
        startPos = new Vec3(x, y, z);
        startYaw = ClientConfig.INSTANCE.cameraYaw.get().floatValue();
        startPitch = ClientConfig.INSTANCE.cameraPitch.get().floatValue();
        
        // Target is lower position (not player position)
        targetPos = new Vec3(x, y - 5.0, z);
        targetYaw = startYaw;
        targetPitch = startPitch;
        
        // Close screen immediately
        if (mc.screen instanceof MapViewScreen) {
            mc.setScreen(null);
        }
    }
    
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

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isStrategyMode) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (isStrategyMode && event.getEntity() == Minecraft.getInstance().player) {
            // 确保玩家总是被渲染
            // 某些 Mod 或原版逻辑可能因为 cameraEntity != player 而跳过一些渲染阶段
            // 这里我们不做取消操作，默认应该渲染
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
         if (isStrategyMode) {
             // 强制设置透明度为 1.0，防止意外透明
             RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }
    }


    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && isStrategyMode) {
            double size = ClientConfig.INSTANCE.orthoSize.get();
            double aspectRatio = (double) Minecraft.getInstance().getWindow().getWidth() / (double) Minecraft.getInstance().getWindow().getHeight();
            
            double near = 0.05;
            double far = Minecraft.getInstance().gameRenderer.getRenderDistance();
            
            // Apply zoom animation during transition if desired, or just static
            // Maybe interpolate size too?
            // For now, static ortho size.
            
            Matrix4f ortho = new Matrix4f();
            ortho.setOrtho(
                    (float)(-size * aspectRatio), 
                    (float)(size * aspectRatio), 
                    (float)(-size), 
                    (float)(size), 
                    (float)near, 
                    (float)far
            );
            
            event.getProjectionMatrix().set(ortho);
        }
    }
    
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (isStrategyMode) {
             // Block crosshair
             // In 1.21, layers are identified by ResourceLocation
             // Vanilla layers are in VanillaGuiLayers
             if (event.getName().getPath().equals("crosshair")) {
                 event.setCanceled(true);
             }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (isTransitioning) {
            transitionProgress += 1.0f / TRANSITION_DURATION;
            if (transitionProgress >= 1.0f) {
                transitionProgress = 1.0f;
                isTransitioning = false;
                
                if (!transitionToStrategy) {
                    finishDisableStrategyMode();
                    return; // Stop processing
                }
            }
            
            float t;
            if (transitionToStrategy) {
                // Ease out (decelerating)
                // 1 - (1 - x)^3
                float f = 1.0f - transitionProgress;
                t = 1.0f - f * f * f;
            } else {
                // Ease in (accelerating)
                // x^3
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
    
    @SubscribeEvent
    public static void onKeyInputPre(InputEvent.Key event) {
        if (isStrategyMode && Minecraft.getInstance().screen == null) {
            // Allow toggle key
            if (event.getKey() == ModKeyBindings.TOGGLE_STRATEGY_MODE.getKey().getValue()) {
                return;
            }
        }
    }
}
