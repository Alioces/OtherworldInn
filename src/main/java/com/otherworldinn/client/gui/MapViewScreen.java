package com.otherworldinn.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.otherworldinn.client.control.CameraHandler;
import com.otherworldinn.client.map.service.MapPageManager;
import com.otherworldinn.foundation.ClientConfig;
import com.otherworldinn.foundation.ModColors;
import com.otherworldinn.init.ModKeyBindings;
import com.otherworldinn.network.ModMessages;
import com.otherworldinn.network.packet.C2STeleportPacket;
import com.otherworldinn.world.map.MapPoint;
import com.otherworldinn.world.map.TownDataProvider;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.service.TeamManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * 地图视图屏幕
 *
 * <p>提供全屏地图浏览功能，支持平滑缩放、页面切换和地图点传送。
 */
public class MapViewScreen extends Screen {

    /** 纹理基础尺寸 (16x16) */
    private static final int TEXTURE_SIZE = 16;

    /** 动画持续时间 (毫秒) */
    private static final long ANIMATION_DURATION = 300;

    // 控件列表
    private final List<MapPointButton> pointButtons = new ArrayList<>();
    private final List<MapNavigationButton> navButtons = new ArrayList<>();
    private final List<MapNavigationButton> prevNavButtons = new ArrayList<>(); // 用于页面切换时的淡出动画

    // 屏幕打开/关闭动画状态
    private final long openTime;
    private long closeStartTime = -1;
    private boolean isClosing = false;

    // 页面切换动画状态
    private long switchStartTime = -1;
    private boolean isSwitchingPage = false;

    public MapViewScreen() {
        super(Component.translatable("screen.otherworldinn.map_view"));
        this.openTime = System.currentTimeMillis();
    }

    /** 开始关闭屏幕的动画流程 */
    public void startClosing() {
        if (!isClosing) {
            isClosing = true;
            closeStartTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void init() {
        super.init();
        pointButtons.clear();
        navButtons.clear();
        prevNavButtons.clear();

        TeamData teamData = TeamManager.getInstance().getClientPlayerTeam();
        MapPageManager manager = MapPageManager.getInstance();

        int currentX = CameraHandler.getCurrentGridX();
        int currentZ = CameraHandler.getCurrentGridZ();

        Set<ResourceLocation> pagePoints = manager.getPointsForPage(currentX, currentZ);

        // 初始化当前页面的地图点按钮
        for (MapPoint point : TownDataProvider.getPoints()) {
            if (pagePoints.contains(point.id()) && teamData.isMapPointUnlocked(point.id())) {
                MapPointButton button = new MapPointButton(point);
                pointButtons.add(button);
                this.addRenderableWidget(button);
            }
        }

        // 初始化分页导航按钮
        initNavigationButtons();

        // 初始化按钮位置
        updateButtonPositionsForFrame(0.0f, 1.0f);
    }

    /**
     * 切换页面时刷新界面控件
     *
     * <p>重新加载当前页和上一页的地图点，并设置导航按钮的淡入淡出状态。
     */
    public void refreshForPageSwitch() {
        this.switchStartTime = System.currentTimeMillis();
        this.isSwitchingPage = true;

        this.clearWidgets();
        pointButtons.clear();

        // 将当前的导航按钮移动到 prev 列表以执行淡出动画
        prevNavButtons.clear();
        prevNavButtons.addAll(navButtons);
        navButtons.clear();

        // 将旧按钮重新添加回界面，但禁用交互
        for (MapNavigationButton btn : prevNavButtons) {
            this.addRenderableWidget(btn);
            btn.active = false;
        }

        TeamData teamData = TeamManager.getInstance().getClientPlayerTeam();
        MapPageManager manager = MapPageManager.getInstance();

        int currentX = CameraHandler.getCurrentGridX();
        int currentZ = CameraHandler.getCurrentGridZ();
        int prevX = CameraHandler.getPrevGridX();
        int prevZ = CameraHandler.getPrevGridZ();

        Set<ResourceLocation> currentPoints = manager.getPointsForPage(currentX, currentZ);
        Set<ResourceLocation> prevPoints = manager.getPointsForPage(prevX, prevZ);

        // 合并当前页和上一页的所有点，以便在切换动画中同时显示
        Set<ResourceLocation> allPoints = new HashSet<>();
        allPoints.addAll(currentPoints);
        allPoints.addAll(prevPoints);

        for (MapPoint point : TownDataProvider.getPoints()) {
            if (allPoints.contains(point.id()) && teamData.isMapPointUnlocked(point.id())) {
                MapPointButton button = new MapPointButton(point);
                pointButtons.add(button);
                this.addRenderableWidget(button);
            }
        }

        // 初始化新页面的导航按钮
        initNavigationButtons();

        // 立即更新一次位置
        updateButtonPositionsForFrame(0.0f, 1.0f);
    }

    /** 初始化四个方向的导航按钮 */
    private void initNavigationButtons() {
        int currentX = CameraHandler.getCurrentGridX();
        int currentZ = CameraHandler.getCurrentGridZ();
        MapPageManager manager = MapPageManager.getInstance();

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int targetX = currentX + dir.getStepX();
            int targetZ = currentZ + dir.getStepZ();

            if (manager.hasPage(targetX, targetZ)) {
                MapNavigationButton button = new MapNavigationButton(dir, targetX, targetZ);
                navButtons.add(button);
                this.addRenderableWidget(button);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float visibility;

        // 计算屏幕开关动画的可见度 (0.0 - 1.0)
        if (isClosing) {
            float t = (now - closeStartTime) / (float) ANIMATION_DURATION;
            visibility = 1.0f - Mth.clamp(t, 0.0f, 1.0f);

            if (t >= 1.0f) {
                super.onClose();
                return;
            }
        } else {
            float t = (now - openTime) / (float) ANIMATION_DURATION;
            visibility = Mth.clamp(t, 0.0f, 1.0f);
        }

        // 更新每一帧的按钮位置和透明度
        updateButtonPositionsForFrame(partialTick, visibility);

        // 计算全局淡入淡出位移
        float yOffset = calculateFadeOffset(visibility);

        RenderSystem.enableBlend();
        // 设置全局 Shader 颜色，子控件的 alpha 会与之相乘
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, visibility);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, yOffset, 0);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.pose().popPose();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    /**
     * 计算淡入淡出动画的垂直位移
     *
     * @param visibility 可见度 (0.0 - 1.0)
     * @return Y轴像素偏移量
     */
    private float calculateFadeOffset(float visibility) {
        // Cubic ease out
        float eased = 1.0f - (1.0f - visibility) * (1.0f - visibility) * (1.0f - visibility);

        // 从底部滑入 (偏移量从负值变到0)
        int iconSize = getIconSize();
        float startOffset = -iconSize / 2.0f;

        return startOffset * (1.0f - eased);
    }

    /** 获取图标动态尺寸 */
    private int getIconSize() {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        return Math.max(8, (int) (8 * guiScale));
    }

    /**
     * 每帧更新按钮位置
     *
     * @param partialTick 渲染部分刻
     * @param globalVisibility 全局可见度 (用于控制整体透明度)
     */
    private void updateButtonPositionsForFrame(float partialTick, float globalVisibility) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        double baseX = ClientConfig.INSTANCE.cameraX.get();
        double baseY = ClientConfig.INSTANCE.cameraY.get();
        double baseZ = ClientConfig.INSTANCE.cameraZ.get();

        // 计算页面切换动画进度 (基于时间)
        float smoothProgress = 0.0f;
        if (isSwitchingPage) {
            long now = System.currentTimeMillis();
            float t = (now - switchStartTime) / (float) ANIMATION_DURATION;
            smoothProgress = Mth.clamp(t, 0.0f, 1.0f);

            if (smoothProgress >= 1.0f) {
                isSwitchingPage = false;
            }
        }

        // 计算插值因子 (Cubic Ease Out)
        float transitionT = 0.0f;
        if (isSwitchingPage) {
            float f = 1.0f - smoothProgress;
            transitionT = 1.0f - f * f * f;
        }

        // 计算当前相机的插值位置
        Vec3 camPos;
        if (isSwitchingPage) {
            double startX = baseX + CameraHandler.getPrevGridX() * MapPageManager.PAGE_SPACING;
            double startZ = baseZ + CameraHandler.getPrevGridZ() * MapPageManager.PAGE_SPACING;

            double targetX = baseX + CameraHandler.getCurrentGridX() * MapPageManager.PAGE_SPACING;
            double targetZ = baseZ + CameraHandler.getCurrentGridZ() * MapPageManager.PAGE_SPACING;

            double currX = Mth.lerp(transitionT, startX, targetX);
            double currZ = Mth.lerp(transitionT, startZ, targetZ);

            camPos = new Vec3(currX, baseY, currZ);
        } else {
            camPos =
                    new Vec3(
                            baseX + CameraHandler.getCurrentGridX() * MapPageManager.PAGE_SPACING,
                            baseY,
                            baseZ + CameraHandler.getCurrentGridZ() * MapPageManager.PAGE_SPACING);
        }

        double currentCamX = camPos.x;
        double currentCamY = camPos.y;
        double currentCamZ = camPos.z;

        double orthoSize = ClientConfig.INSTANCE.orthoSize.get();
        double pixelsPerBlock = height / (orthoSize * 2.0);
        int iconSize = getIconSize();

        // 准备集合以判断点属于哪个页面
        MapPageManager manager = MapPageManager.getInstance();
        int curGridX = CameraHandler.getCurrentGridX();
        int curGridZ = CameraHandler.getCurrentGridZ();
        int prevGridX = CameraHandler.getPrevGridX();
        int prevGridZ = CameraHandler.getPrevGridZ();

        Set<ResourceLocation> currentPoints = manager.getPointsForPage(curGridX, curGridZ);
        Set<ResourceLocation> prevPoints = manager.getPointsForPage(prevGridX, prevGridZ);

        // 1. 更新地图点按钮
        for (MapPointButton button : pointButtons) {
            MapPoint point = button.point;

            float alpha = 1.0f;
            float yOffset = 0.0f;

            double calcCamX = currentCamX;
            double calcCamY = currentCamY;
            double calcCamZ = currentCamZ;

            if (isSwitchingPage) {
                boolean inCurrent = currentPoints.contains(point.id());
                boolean inPrev = prevPoints.contains(point.id());

                if (inCurrent && inPrev) {
                    // 共用点：直接插值屏幕坐标，确保平滑
                    alpha = 1.0f;

                    // 起点：相对于上一页相机的屏幕坐标
                    double startCamX = baseX + prevGridX * MapPageManager.PAGE_SPACING;
                    double startCamZ = baseZ + prevGridZ * MapPageManager.PAGE_SPACING;
                    Vec2 startScreenPos =
                            calculateScreenPosition(
                                    point.worldPosition().x,
                                    point.worldPosition().y,
                                    point.worldPosition().z,
                                    point.screenOffset().x,
                                    point.screenOffset().y,
                                    startCamX,
                                    baseY,
                                    startCamZ,
                                    pixelsPerBlock);

                    // 终点：相对于当前页相机的屏幕坐标
                    double endCamX = baseX + curGridX * MapPageManager.PAGE_SPACING;
                    double endCamZ = baseZ + curGridZ * MapPageManager.PAGE_SPACING;
                    Vec2 endScreenPos =
                            calculateScreenPosition(
                                    point.worldPosition().x,
                                    point.worldPosition().y,
                                    point.worldPosition().z,
                                    point.screenOffset().x,
                                    point.screenOffset().y,
                                    endCamX,
                                    baseY,
                                    endCamZ,
                                    pixelsPerBlock);

                    // 插值
                    double screenX = Mth.lerp(transitionT, startScreenPos.x, endScreenPos.x);
                    double screenY = Mth.lerp(transitionT, startScreenPos.y, endScreenPos.y);

                    button.setWidth(iconSize);
                    button.setHeight(iconSize);
                    button.setPosition((int) screenX - iconSize / 2, (int) screenY - iconSize / 2);

                    // 设置透明度并跳过后续常规更新
                    button.setAlpha(alpha * globalVisibility);
                    button.active = (alpha * globalVisibility) > 0.05f;
                    button.visible = (alpha * globalVisibility) > 0.01f;
                    continue;

                } else if (inPrev) {
                    // 退出点：淡出并向上滑出
                    // 保持相对于上一页相机的固定位置
                    calcCamX = baseX + prevGridX * MapPageManager.PAGE_SPACING;
                    calcCamZ = baseZ + prevGridZ * MapPageManager.PAGE_SPACING;

                    float visibility = 1.0f - smoothProgress;
                    alpha = visibility;
                    yOffset = calculateFadeOffset(visibility);

                } else if (inCurrent) {
                    // 进入点：淡入并从上滑入
                    // 保持相对于当前页相机的固定位置
                    calcCamX = baseX + curGridX * MapPageManager.PAGE_SPACING;
                    calcCamZ = baseZ + curGridZ * MapPageManager.PAGE_SPACING;

                    float visibility = smoothProgress;
                    alpha = visibility;
                    yOffset = calculateFadeOffset(visibility);

                } else {
                    alpha = 0.0f;
                }
            }

            button.setAlpha(alpha * globalVisibility);
            button.active = (alpha * globalVisibility) > 0.05f;
            button.visible = (alpha * globalVisibility) > 0.01f;

            updateButtonPosition(
                    button,
                    point.worldPosition().x,
                    point.worldPosition().y,
                    point.worldPosition().z,
                    point.screenOffset().x,
                    point.screenOffset().y + yOffset,
                    calcCamX,
                    calcCamY,
                    calcCamZ,
                    pixelsPerBlock,
                    iconSize);
        }

        // 2. 更新导航按钮 (新页面) - 淡入
        double navOffset = 25.0;
        double pageY = ClientConfig.INSTANCE.cameraY.get();

        for (MapNavigationButton button : navButtons) {
            if (isSwitchingPage) {
                button.setAlpha(smoothProgress * globalVisibility);
            } else {
                button.setAlpha(1.0f * globalVisibility);
            }

            double btnWorldX = currentCamX + button.direction.getStepX() * navOffset;
            double btnWorldZ = currentCamZ + button.direction.getStepZ() * navOffset;

            updateButtonPosition(
                    button,
                    btnWorldX,
                    pageY,
                    btnWorldZ,
                    0.0,
                    0.0,
                    currentCamX,
                    currentCamY,
                    currentCamZ,
                    pixelsPerBlock,
                    iconSize);
        }

        // 3. 更新旧导航按钮 (旧页面) - 淡出
        for (MapNavigationButton button : prevNavButtons) {
            if (isSwitchingPage) {
                button.setAlpha((1.0f - smoothProgress) * globalVisibility);

                // 保持相对于旧页面的固定位置
                double prevPageCenterX = baseX + prevGridX * MapPageManager.PAGE_SPACING;
                double prevPageCenterZ = baseZ + prevGridZ * MapPageManager.PAGE_SPACING;

                double prevBtnWorldX = prevPageCenterX + button.direction.getStepX() * navOffset;
                double prevBtnWorldZ = prevPageCenterZ + button.direction.getStepZ() * navOffset;

                updateButtonPosition(
                        button,
                        prevBtnWorldX,
                        pageY,
                        prevBtnWorldZ,
                        0.0,
                        0.0,
                        prevPageCenterX,
                        baseY,
                        prevPageCenterZ,
                        pixelsPerBlock,
                        iconSize);
            } else {
                button.visible = false;
                button.active = false;
            }
        }
    }

    /** 计算并设置按钮的屏幕位置 */
    private void updateButtonPosition(
            Button button,
            double worldX,
            double worldY,
            double worldZ,
            double screenOffsetX,
            double screenOffsetY,
            double camX,
            double camY,
            double camZ,
            double pixelsPerBlock,
            int iconSize) {
        Vec2 screenPos =
                calculateScreenPosition(
                        worldX,
                        worldY,
                        worldZ,
                        (float) screenOffsetX,
                        (float) screenOffsetY,
                        camX,
                        camY,
                        camZ,
                        pixelsPerBlock);

        button.setWidth(iconSize);
        button.setHeight(iconSize);
        button.setPosition((int) screenPos.x - iconSize / 2, (int) screenPos.y - iconSize / 2);
    }

    /** 根据相机位置计算点的屏幕坐标 */
    private Vec2 calculateScreenPosition(
            double worldX,
            double worldY,
            double worldZ,
            float screenOffsetX,
            float screenOffsetY,
            double camX,
            double camY,
            double camZ,
            double pixelsPerBlock) {

        // 1. 计算相对于相机的世界坐标偏移
        double dx = worldX - camX;
        double dy = worldY - camY;
        double dz = worldZ - camZ;

        // 2. 旋转坐标系以匹配相机视角
        float yawRad = (float) Math.toRadians(ClientConfig.INSTANCE.cameraYaw.get());
        float pitchRad = (float) Math.toRadians(ClientConfig.INSTANCE.cameraPitch.get());

        double rotatedX = dx * Math.cos(-yawRad) - dz * Math.sin(-yawRad);
        double rotatedZ = dx * Math.sin(-yawRad) + dz * Math.cos(-yawRad);

        double x1 = -rotatedX;
        double z1 = rotatedZ;
        double y1 = dy;

        // 3. 投影到屏幕
        int centerX = width / 2;
        int centerY = height / 2;
        double screenX = centerX + x1 * pixelsPerBlock;
        double screenY =
                centerY - (y1 * Math.cos(pitchRad) + z1 * Math.sin(pitchRad)) * pixelsPerBlock;

        // 4. 应用屏幕偏移
        screenX += screenOffsetX;
        screenY += screenOffsetY;

        return new Vec2((float) screenX, (float) screenY);
    }

    @Override
    public void renderBackground(
            GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 留空以保持背景透明
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyBindings.TOGGLE_MAP_MODE.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        CameraHandler.disableMapMode();
        // 实际关闭操作由 render 中的动画逻辑触发
    }

    /** 地图点按钮 */
    private class MapPointButton extends Button {
        private final MapPoint point;

        protected MapPointButton(MapPoint point) {
            super(0, 0, 0, 0, point.displayName(), (btn) -> {}, Button.DEFAULT_NARRATION);
            this.point = point;
            this.setTooltip(null);
        }

        @Override
        public void onPress() {
            if (canTeleport()) {
                Minecraft.getInstance()
                        .getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                ModMessages.sendToServer(new C2STeleportPacket(point.id()));
                MapViewScreen.this.onClose();
            }
        }

        private boolean canTeleport() {
            TeamData teamData = TeamManager.getInstance().getClientPlayerTeam();
            return teamData.isTeleportUnlocked();
        }

        @Override
        public void renderWidget(
                GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();

            ResourceLocation texture = point.iconTexture();

            int vOffset = 0;
            if (isHovered) {
                if (Minecraft.getInstance().mouseHandler.isLeftPressed()) {
                    vOffset = TEXTURE_SIZE * 2;
                } else {
                    vOffset = TEXTURE_SIZE;
                }
            }

            int totalTextureHeight = TEXTURE_SIZE * 3;

            // 应用按钮透明度
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);

            guiGraphics.blit(
                    texture,
                    getX(),
                    getY(),
                    getWidth(),
                    getHeight(),
                    0,
                    vOffset,
                    TEXTURE_SIZE,
                    TEXTURE_SIZE,
                    TEXTURE_SIZE,
                    totalTextureHeight);

            // 悬停时显示名称
            if (isHovered) {
                Component text = point.displayName();
                int textWidth = Minecraft.getInstance().font.width(text);
                int textX = getX() + (width - textWidth) / 2;
                int textY = getY() - 10;
                guiGraphics.drawString(
                        Minecraft.getInstance().font, text, textX, textY, ModColors.WHITE, true);
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableDepthTest();
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (canTeleport()) {
                super.onClick(mouseX, mouseY);
            }
        }
    }

    /** 导航箭头按钮 */
    private class MapNavigationButton extends Button {
        final Direction direction;

        // 导航贴图
        private static final ResourceLocation ARROW_NORTH =
                ResourceLocation.fromNamespaceAndPath(
                        "otherworldinn", "textures/gui/map/arrow_north.png");
        private static final ResourceLocation ARROW_SOUTH =
                ResourceLocation.fromNamespaceAndPath(
                        "otherworldinn", "textures/gui/map/arrow_south.png");
        private static final ResourceLocation ARROW_EAST =
                ResourceLocation.fromNamespaceAndPath(
                        "otherworldinn", "textures/gui/map/arrow_east.png");
        private static final ResourceLocation ARROW_WEST =
                ResourceLocation.fromNamespaceAndPath(
                        "otherworldinn", "textures/gui/map/arrow_west.png");

        public MapNavigationButton(Direction dir, int targetX, int targetZ) {
            super(
                    0,
                    0,
                    0,
                    0,
                    Component.empty(),
                    (btn) -> {
                        CameraHandler.moveToPage(targetX, targetZ);
                        if (Minecraft.getInstance().screen instanceof MapViewScreen mapScreen) {
                            mapScreen.refreshForPageSwitch();
                        }
                    },
                    Button.DEFAULT_NARRATION);
            this.direction = dir;
        }

        @Override
        public void renderWidget(
                GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();

            ResourceLocation texture =
                    switch (direction) {
                        case NORTH -> ARROW_NORTH;
                        case SOUTH -> ARROW_SOUTH;
                        case EAST -> ARROW_EAST;
                        case WEST -> ARROW_WEST;
                        default -> ARROW_NORTH;
                    };

            int vOffset = 0;
            if (isHovered) {
                if (Minecraft.getInstance().mouseHandler.isLeftPressed()) {
                    vOffset = 16 * 2;
                } else {
                    vOffset = 16;
                }
            }

            int totalTextureHeight = 16 * 3;

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha);

            guiGraphics.blit(
                    texture,
                    getX(),
                    getY(),
                    getWidth(),
                    getHeight(),
                    0,
                    vOffset,
                    16,
                    16,
                    16,
                    totalTextureHeight);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableDepthTest();
        }
    }
}
