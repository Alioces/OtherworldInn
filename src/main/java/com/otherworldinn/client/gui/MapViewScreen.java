package com.otherworldinn.client.gui;

import com.otherworldinn.client.CameraHandler;
import com.otherworldinn.init.ModKeyBindings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 地图视图屏幕
 */
public class MapViewScreen extends Screen {

    public MapViewScreen() {
        super(Component.translatable("screen.otherworldinn.map_view"));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 暂时不渲染任何背景，保持透明
        // 可以在这里绘制自定义 HUD、边框或信息
        
        // 调用父类以渲染子控件（如果有）
        super.render(guiGraphics, mouseX, mouseY, partialTick);
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
        // 关闭屏幕时同时也退出地图
        CameraHandler.disableStrategyMode();
        super.onClose();
    }
}
