package com.otherworldinn.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.otherworldinn.client.CameraHandler;
import com.otherworldinn.foundation.ClientConfig;
import com.otherworldinn.init.ModKeyBindings;
import com.otherworldinn.network.ModMessages;
import com.otherworldinn.network.packet.C2STeleportPacket;
import com.otherworldinn.world.map.MapPoint;
import com.otherworldinn.world.map.TownDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;

/**
 * 地图视图屏幕
 */
public class MapViewScreen extends Screen {

    // 原始纹理大小为 16x16
    private static final int TEXTURE_SIZE = 16;

    // 按钮列表，用于处理交互
    private final List<MapPointButton> pointButtons = new ArrayList<>();
    
    // 动画相关
    private long openTime;
    private long closeStartTime = -1;
    private boolean isClosing = false;
    private static final long ANIMATION_DURATION = 300; // 0.3秒

    public MapViewScreen() {
        super(Component.translatable("screen.otherworldinn.map_view"));
        this.openTime = System.currentTimeMillis();
    }
    
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
        
        TeamData teamData = TeamManager.getInstance().getClientPlayerTeam();
        
        // 初始化地图点按钮
        for (MapPoint point : TownDataProvider.getPoints()) {
            // 仅添加已解锁的地图点
            if (teamData.isMapPointUnlocked(point.id())) {
                MapPointButton button = new MapPointButton(point);
                pointButtons.add(button);
                this.addRenderableWidget(button);
            }
        }
        
        // 初始化时计算一次位置，避免每帧计算导致抖动
        initButtonPositions();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float progress;
        
        if (isClosing) {
            // 退出动画：1.0 -> 0.0
            float t = (now - closeStartTime) / (float) ANIMATION_DURATION;
            progress = 1.0f - Mth.clamp(t, 0.0f, 1.0f);
            
            // 动画结束后真正关闭屏幕
            if (t >= 1.0f) {
                super.onClose();
                return;
            }
        } else {
            // 进入动画：0.0 -> 1.0
            float t = (now - openTime) / (float) ANIMATION_DURATION;
            progress = Mth.clamp(t, 0.0f, 1.0f);
        }
        
        float eased = 1.0f - (1.0f - progress) * (1.0f - progress) * (1.0f - progress);

        // 偏移量调整为半个图标大小
        int iconSize = getIconSize();
        int startOffset = -iconSize / 2; 
        int yOffset = (int) (startOffset * (1.0f - eased));
        
        // 设置透明度
        // 进入时：0 -> 1
        // 退出时：1 -> 0
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, progress);
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, yOffset, 0);
        
        // 暂时不渲染任何背景，保持透明
        
        // 调用父类以渲染子控件（按钮）
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        guiGraphics.pose().popPose();
        
        // 恢复颜色
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
    
    /**
     * 计算图标的动态大小
     * 基于界面尺寸 (GUI Scale) 为 4 时，图标大小为 32
     */
    private int getIconSize() {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        // 确保最小尺寸，防止不可见
        return Math.max(8, (int) (8 * guiScale));
    }

    /**
     * 将世界坐标转换为屏幕坐标并更新按钮位置
     * <p>直接使用配置参数计算，保持图标静止</p>
     */
    private void initButtonPositions() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        // 直接读取配置中的目标位置，确保图标位置绝对静止
        double camX = ClientConfig.INSTANCE.cameraX.get();
        double camY = ClientConfig.INSTANCE.cameraY.get();
        double camZ = ClientConfig.INSTANCE.cameraZ.get();
        
        double orthoSize = ClientConfig.INSTANCE.orthoSize.get();
        
        // 计算缩放比例：将世界单位转换为屏幕像素
        // 屏幕高度对应 2 * orthoSize 的世界高度
        //double fovMultiplier = 1.5;
        double pixelsPerBlock = height / (orthoSize * 2.0);
        
        int iconSize = getIconSize();

        for (MapPointButton button : pointButtons) {
            MapPoint point = button.point;

            // 1. 计算世界坐标相对于摄像机的偏移
            double dx = point.worldPosition().x - camX;
            double dy = point.worldPosition().y - camY;
            double dz = point.worldPosition().z - camZ;
            
            // 2. 旋转这些偏移以匹配摄像机视角
            float yawRad = (float) Math.toRadians(ClientConfig.INSTANCE.cameraYaw.get());
            float pitchRad = (float) Math.toRadians(ClientConfig.INSTANCE.cameraPitch.get());
            
            // 坐标系旋转：

            double rotatedX = dx * Math.cos(-yawRad) - dz * Math.sin(-yawRad);
            double rotatedZ = dx * Math.sin(-yawRad) + dz * Math.cos(-yawRad);
            
            double x1 = -rotatedX; 
            double z1 = rotatedZ;
            double y1 = dy;
            
            // 屏幕中心
            int centerX = width / 2;
            int centerY = height / 2;
            double screenX = centerX + x1 * pixelsPerBlock;

            double screenY = centerY - (y1 * Math.cos(pitchRad) + z1 * Math.sin(pitchRad)) * pixelsPerBlock;
            
            // 添加手动偏移
            screenX += point.screenOffset().x;
            screenY += point.screenOffset().y;
            
            // 更新按钮大小和位置
            button.setWidth(iconSize);
            button.setHeight(iconSize);
            button.setPosition((int) screenX - iconSize / 2, (int) screenY - iconSize / 2);
        }
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 留空以保持背景透明，不渲染默认的暗色背景
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 再次按下 M 键关闭
        if (ModKeyBindings.TOGGLE_STRATEGY_MODE.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        CameraHandler.disableStrategyMode();
        //等待动画结束
    }
    
    /**
     * 地图点按钮内部类
     */
    private class MapPointButton extends Button {
        private final MapPoint point;

        protected MapPointButton(MapPoint point) {
            super(0, 0, 0, 0, point.displayName(), (btn) -> {
                // 点击回调 - 实际逻辑移至 onPress
            }, Button.DEFAULT_NARRATION);
            this.point = point;
            
            // 移除 Tooltip，改为直接绘制文本
            this.setTooltip(null);
        }
        
        @Override
        public void onPress() {
            // 特殊处理：城镇大门
            if (point.id().equals(ResourceLocation.fromNamespaceAndPath(com.otherworldinn.OtherworldInn.MODID, "town_gate"))) {
                // TODO: 城镇大门特殊交互逻辑
                return;
            }

            if (canTeleport()) {
                // 播放点击音效
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                // 发送传送数据包
                ModMessages.sendToServer(new C2STeleportPacket(point.id()));
                
                // 关闭地图视图
                MapViewScreen.this.onClose();
            } else {
                // 传送未解锁时的点击反馈
            }
        }
        
        private boolean canTeleport() {
            TeamData teamData = TeamManager.getInstance().getClientPlayerTeam();
            return teamData.isTeleportUnlocked();
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            
            // 根据状态选择纹理区域
            ResourceLocation texture = point.iconTexture();
            
            // 计算纹理 V 偏移
            // 假设纹理为垂直排列：正常、悬停、按下
            // 每个状态的高度为 TEXTURE_SIZE (16)
            int vOffset = 0;
            if (isHovered) {
                // 检查是否按下鼠标左键
                if (Minecraft.getInstance().mouseHandler.isLeftPressed()) {
                    vOffset = TEXTURE_SIZE * 2; // 按下 (Index 2)
                } else {
                    vOffset = TEXTURE_SIZE; // 悬停 (Index 1)
                }
            }
            // 正常状态 (Index 0) vOffset = 0
            
            // 绘制图标
            // 纹理总高度假设为 3 * TEXTURE_SIZE = 48
            int totalTextureHeight = TEXTURE_SIZE * 3;
            
            guiGraphics.blit(texture, getX(), getY(), getWidth(), getHeight(), 0, vOffset, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, totalTextureHeight);
            
            // 悬停时在图标上方绘制文字
            if (isHovered) {
                Component text = point.displayName();
                
                int textWidth = Minecraft.getInstance().font.width(text);
                int textX = getX() + (width - textWidth) / 2; // 居中
                int textY = getY() - 10; // 图标上方 10 像素
                
                guiGraphics.drawString(Minecraft.getInstance().font, text, textX, textY, 0xFFFFFF, true);
            }
            
            RenderSystem.enableDepthTest(); // 恢复深度测试
        }
        
        @Override
        public void onClick(double mouseX, double mouseY) {
            if (canTeleport()) {
                super.onClick(mouseX, mouseY);
            } else {
                // 未解锁时的点击反馈（可选：播放锁定音效）
            }
        }
    }
}
