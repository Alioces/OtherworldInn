package com.otherworldinn.client.commission;

import com.otherworldinn.client.gui.screen.CommissionBoardScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public final class CommissionClientManager {
    private static CompoundTag latestBoardData;

    private CommissionClientManager() {}

    public static void handleBoardData(CompoundTag data, boolean openScreen) {
        latestBoardData = data == null ? new CompoundTag() : data.copy();
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CommissionBoardScreen screen) {
            screen.updateBoardData(latestBoardData);
        } else if (openScreen) {
            mc.setScreen(new CommissionBoardScreen(latestBoardData));
        }
    }

    public static CompoundTag getLatestBoardData() {
        return latestBoardData == null ? new CompoundTag() : latestBoardData.copy();
    }
}
