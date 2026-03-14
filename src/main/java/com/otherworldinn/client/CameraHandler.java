package com.otherworldinn.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.otherworldinn.OtherworldInn;
import com.otherworldinn.client.gui.MapViewScreen;
import com.otherworldinn.client.map.MapPageManager;
import com.otherworldinn.foundation.ClientConfig;
import com.otherworldinn.init.ModKeyBindings;
import com.otherworldinn.world.dimension.TownDimensions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
/** 摄像机处理器 */
public class CameraHandler {

    private static boolean isMapMode = false;
    private static Entity dummyCameraEntity;
    private static Entity originalCameraEntity;

    private static boolean isTransitioning = false;
    private static float transitionProgress = 0.0f;
    private static float prevTransitionProgress = 0.0f;
    private static final float TRANSITION_DURATION = 5.0f;
    private static boolean transitionToMap = false;

    private static Vec3 startPos;
    private static Vec3 targetPos;
    private static float startYaw, startPitch;
    private static float targetYaw, targetPitch;

    private static int currentGridX = 0;
    private static int currentGridZ = 0;
    private static int prevGridX = 0;
    private static int prevGridZ = 0;

    /**
     * 处理按键输入事件
     *
     * @param event 按键事件
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.dimension() != TownDimensions.TOWN_LEVEL) {
            return;
        }

        if (ModKeyBindings.TOGGLE_MAP_MODE.consumeClick()) {
            if (isMapMode) {
                disableMapMode();
            } else {
                enableMapMode();
            }
        }
    }

    /** 打开地图 */
    public static void enableMapMode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        isTransitioning = true;
        transitionProgress = 0.0f;
        prevTransitionProgress = 0.0f;
        transitionToMap = true;
        currentGridX = 0;
        currentGridZ = 0;
        prevGridX = 0;
        prevGridZ = 0;

        originalCameraEntity = mc.getCameraEntity();

        if (dummyCameraEntity == null || !dummyCameraEntity.isAlive()) {
            dummyCameraEntity = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            dummyCameraEntity.setInvisible(true);
            dummyCameraEntity.setNoGravity(true);
            mc.level.addEntity(dummyCameraEntity);
        }

        targetPos = getCurrentPageTargetPos();
        targetYaw = ClientConfig.INSTANCE.cameraYaw.get().floatValue();
        targetPitch = ClientConfig.INSTANCE.cameraPitch.get().floatValue();

        double x = targetPos.x;
        double y = targetPos.y;
        double z = targetPos.z;

        startPos = new Vec3(x, y - 5.0, z);
        startYaw = targetYaw;
        startPitch = targetPitch;

        updateDummyEntity(startPos, startYaw, startPitch);

        mc.setCameraEntity(dummyCameraEntity);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.player.setInvisible(false);

        mc.setScreen(new MapViewScreen());

        isMapMode = true;
    }

    /**
     * 移动到指定页面
     *
     * @param gridX 页面网格 X
     * @param gridZ 页面网格 Z
     */
    public static void moveToPage(int gridX, int gridZ) {
        if (!isMapMode) return;

        prevGridX = currentGridX;
        prevGridZ = currentGridZ;
        currentGridX = gridX;
        currentGridZ = gridZ;

        isTransitioning = true;
        transitionProgress = 0.0f;
        prevTransitionProgress = 0.0f;
        transitionToMap = true; // Use ease-out for smooth arrival

        if (dummyCameraEntity != null) {
            startPos = dummyCameraEntity.position();
        } else {
            startPos = getCurrentPageTargetPos(); // Fallback
        }

        targetPos = getCurrentPageTargetPos();
        // Yaw/Pitch remain same
        startYaw = targetYaw;
        startPitch = targetPitch;
    }

    /** 关闭地图 */
    public static void disableMapMode() {
        isTransitioning = true;
        transitionProgress = 0.0f;
        prevTransitionProgress = 0.0f;
        transitionToMap = false;

        Minecraft mc = Minecraft.getInstance();

        startPos = getCurrentPageTargetPos();
        startYaw = ClientConfig.INSTANCE.cameraYaw.get().floatValue();
        startPitch = ClientConfig.INSTANCE.cameraPitch.get().floatValue();

        targetPos = new Vec3(startPos.x, startPos.y - 5.0, startPos.z);
        targetYaw = startYaw;
        targetPitch = startPitch;

        if (mc.screen instanceof MapViewScreen) {
            mc.setScreen(null);
        }
    }

    /** 获取当前页面中心目标位置 */
    private static Vec3 getCurrentPageTargetPos() {
        double baseX = ClientConfig.INSTANCE.cameraX.get();
        double baseY = ClientConfig.INSTANCE.cameraY.get();
        double baseZ = ClientConfig.INSTANCE.cameraZ.get();

        return new Vec3(
                baseX + currentGridX * MapPageManager.PAGE_SPACING,
                baseY,
                baseZ + currentGridZ * MapPageManager.PAGE_SPACING);
    }

    public static int getCurrentGridX() {
        return currentGridX;
    }

    public static int getCurrentGridZ() {
        return currentGridZ;
    }

    public static int getPrevGridX() {
        return prevGridX;
    }

    public static int getPrevGridZ() {
        return prevGridZ;
    }

    public static float getTransitionProgress() {
        return transitionProgress;
    }

    /**
     * 获取平滑插值的过渡进度
     *
     * @param partialTick 渲染部分刻
     */
    public static float getSmoothTransitionProgress(float partialTick) {
        if (!isTransitioning) return isMapMode ? 1.0f : 0.0f;
        return Mth.lerp(partialTick, prevTransitionProgress, transitionProgress);
    }

    public static boolean isTransitioning() {
        return isTransitioning;
    }

    /** 检查是否处于地图模式 */
    public static boolean isMapMode() {
        return isMapMode;
    }

    /**
     * 更新虚拟摄像机实体的位置和旋转
     *
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

    /** 阻止第一人称手部渲染 */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isMapMode) {
            event.setCanceled(true);
        }
    }

    /**
     * 应用正交投影矩阵
     *
     * <p>在 {@link RenderLevelStageEvent.Stage#AFTER_SKY} 阶段修改投影矩阵，
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && isMapMode) {
            double size = ClientConfig.INSTANCE.orthoSize.get();
            double aspectRatio =
                    (double) Minecraft.getInstance().getWindow().getWidth()
                            / (double) Minecraft.getInstance().getWindow().getHeight();

            double near = -256.0;
            double far = 256.0;

            Matrix4f ortho = new Matrix4f();
            ortho.setOrtho(
                    (float) (-size * aspectRatio),
                    (float) (size * aspectRatio),
                    (float) (-size),
                    (float) (size),
                    (float) near,
                    (float) far);

            RenderSystem.setProjectionMatrix(ortho, VertexSorting.DISTANCE_TO_ORIGIN);

            event.getProjectionMatrix().set(ortho);
        }
    }

    /** 隐藏准星 */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (isMapMode) {
            if (event.getName().getPath().equals("crosshair")) {
                event.setCanceled(true);
            }
        }
    }

    /** 客户端每刻更新 */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (isTransitioning) {
            prevTransitionProgress = transitionProgress;
            transitionProgress += 1.0f / TRANSITION_DURATION;
            if (transitionProgress >= 1.0f) {
                transitionProgress = 1.0f;
                isTransitioning = false;

                if (!transitionToMap) {
                    finishDisableMapMode();
                    return;
                }
            }

            float t;
            if (transitionToMap) {
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
        } else if (isMapMode) {
            Minecraft mc = Minecraft.getInstance();
            if (dummyCameraEntity != null) {
                Vec3 target = getCurrentPageTargetPos();

                float yaw = ClientConfig.INSTANCE.cameraYaw.get().floatValue();
                float pitch = ClientConfig.INSTANCE.cameraPitch.get().floatValue();

                updateDummyEntity(target, yaw, pitch);

                if (mc.getCameraEntity() != dummyCameraEntity) {
                    mc.setCameraEntity(dummyCameraEntity);
                }
            }
        }
    }

    /**
     * 获取虚拟摄像机实体的当前位置
     *
     * @return 位置，如果虚拟实体不存在则返回 null
     */
    public static Vec3 getDummyCameraPos() {
        return dummyCameraEntity != null ? dummyCameraEntity.position() : null;
    }

    /** 获取目标偏航角 */
    public static float getTargetYaw() {
        return targetYaw;
    }

    /** 获取目标俯仰角 */
    public static float getTargetPitch() {
        return targetPitch;
    }

    /** 完成退出地图视角 */
    private static void finishDisableMapMode() {
        Minecraft mc = Minecraft.getInstance();
        isMapMode = false;

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
