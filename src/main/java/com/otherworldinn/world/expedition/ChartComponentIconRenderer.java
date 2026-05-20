package com.otherworldinn.world.expedition;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.client.render.SlotIconRenderer;
import com.otherworldinn.item.ChartComponentItem;
import com.github.ysbbbbbb.kaleidoscopecookery.item.RecipeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
public final class ChartComponentIconRenderer {

    private static final float OVERLAY_SCALE = 0.7f;
    private static final int OVERLAY_Z = 200;

    private ChartComponentIconRenderer() {}

    @SubscribeEvent
    public static void onRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        GuiGraphics gfx = event.getGuiGraphics();
        AbstractContainerScreen<?> screen = event.getContainerScreen();

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.hasItem()) continue;
            renderOverlayIfApplicable(gfx, slot.getItem(), slot.x, slot.y);
        }
    }

    @SubscribeEvent
    public static void onRenderHotbar(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        GuiGraphics gfx = event.getGuiGraphics();
        Inventory inv = mc.player.getInventory();
        int screenW = gfx.guiWidth();
        int screenH = gfx.guiHeight();
        int startX = (screenW - 182) / 2 + 3;
        int y = screenH - 22 + 3;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            int x = startX + i * 20;
            renderOverlayIfApplicable(gfx, stack, x, y);
        }
    }

    public static void renderOverlayIfApplicable(GuiGraphics gfx, ItemStack stack,
            int slotX, int slotY) {
        ItemStack icon = resolveOverlayIcon(stack);
        if (icon == null) return;
        SlotIconRenderer.renderCentered(gfx, icon, slotX, slotY, OVERLAY_Z, OVERLAY_SCALE);
    }

    public static ItemStack resolveOverlayIcon(ItemStack stack) {
        if (stack.getItem() instanceof ChartComponentItem) {
            String compType = ChartComponentItem.getComponentType(stack);
            if ("blank".equals(compType)) return null;
            ChartComponentType type = ChartComponentType.byId(compType);
            if (type == null) return null;
            return new ItemStack(type.iconItem());
        }
        if (stack.getItem() instanceof RecipeItem) {
            RecipeItem.RecipeRecord record = RecipeItem.getRecipe(stack);
            if (record == null) return null;
            ItemStack output = record.output();
            if (output.isEmpty()) return null;
            return output.copy();
        }
        return null;
    }
}
