package com.otherworldinn.world.expedition;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.item.ChartComponentItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
public final class ChartComponentIconRenderer {

    private ChartComponentIconRenderer() {}

    @SubscribeEvent
    public static void onRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        GuiGraphics gfx = event.getGuiGraphics();
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        int guiLeft = screen.getGuiLeft();
        int guiTop = screen.getGuiTop();

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.hasItem()) continue;
            ItemStack stack = slot.getItem();
            if (!(stack.getItem() instanceof ChartComponentItem)) continue;

            String compType = ChartComponentItem.getComponentType(stack);
            if ("blank".equals(compType)) continue;

            ChartComponentType type = ChartComponentType.byId(compType);
            if (type == null) continue;

            int x = guiLeft + slot.x;
            int y = guiTop + slot.y;

            gfx.pose().pushPose();
            gfx.pose().translate(x + 4, y + 4, 200);
            gfx.pose().scale(0.7f, 0.7f, 1f);
            gfx.renderFakeItem(new ItemStack(type.iconItem()), 0, 0);
            gfx.pose().popPose();
        }
    }
}
