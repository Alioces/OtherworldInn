package com.otherworldinn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class SlotIconRenderer {

    private SlotIconRenderer() {}

    public static void renderCentered(GuiGraphics gfx, ItemStack icon,
            int slotX, int slotY, int z, float scale) {
        PoseStack pose = gfx.pose();
        pose.pushPose();
        float half = 8.0f * (1.0f - scale);
        pose.translate(slotX + half, slotY + half, z);
        pose.scale(scale, scale, 1.0f);
        gfx.renderFakeItem(icon, 0, 0);
        pose.popPose();
    }
}
