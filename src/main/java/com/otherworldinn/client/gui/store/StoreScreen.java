package com.otherworldinn.client.gui.store;

import com.mojang.blaze3d.systems.RenderSystem;
import com.otherworldinn.entity.store.StoreEntity;
import com.otherworldinn.world.inventory.StoreMenu;

import net.minecraft.client.Minecraft;
import com.otherworldinn.foundation.ModColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import com.otherworldinn.network.ModMessages;
import com.otherworldinn.network.packet.C2SStorePurchasePacket;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;

/**
 * 商店屏幕
 */
public class StoreScreen extends AbstractContainerScreen<StoreMenu> {

    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 5;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_SPACING = 2;

    private final ResourceLocation backgroundTexture;

    // 选中状态
    @Nullable
    private StoreEntity.StoreItem selectedItem = null;
    private int purchaseQuantity = 1;

    // 控件
    private EditBox quantityEditBox;
    private Button confirmButton;
    private Button purchaseButton;
    private final List<StoreEntity.StoreItem> cart = new ArrayList<>();
    
    // 购物车滚动相关
    private float scrollOffs = 0.0F;
    private boolean isScrolling = false;
    private static final int CART_ITEM_HEIGHT = 20;
    private static final int CART_DISPLAY_ROWS = 5; // 显示几行
    private static final int SCROLL_BAR_WIDTH = 4; // 滚动条宽度

    public StoreScreen(StoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256; // 宽屏界面
        this.imageHeight = 166;
        
        // 获取背景纹理
        if (menu.getStoreEntity() != null) {
            this.backgroundTexture = menu.getStoreEntity().getStoreBackground();
        } else {
            // 默认背景 (Fallback)
            this.backgroundTexture = ResourceLocation.fromNamespaceAndPath("otherworldinn", "textures/gui/store.png");
        }
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        // 数量输入框
        this.quantityEditBox = new EditBox(this.font, this.leftPos + 30, this.topPos + 110, 40, 16, Component.literal("1"));
        this.quantityEditBox.setValue("1");
        this.quantityEditBox.setFilter(s -> s.matches("\\d*"));
        this.quantityEditBox.setResponder(s -> {
            try {
                int val = Integer.parseInt(s);
                this.purchaseQuantity = Math.max(1, val);
            } catch (NumberFormatException e) {
                // ignore
            }
        });
        this.addRenderableWidget(this.quantityEditBox);

        // 减少按钮
        this.addRenderableWidget(Button.builder(Component.literal("-"), (btn) -> {
            int change = hasShiftDown() ? 16 : 1;
            if (this.purchaseQuantity > 1) {
                this.purchaseQuantity = Math.max(1, this.purchaseQuantity - change);
                this.quantityEditBox.setValue(String.valueOf(this.purchaseQuantity));
            }
        }).bounds(this.leftPos + 10, this.topPos + 110, 16, 16).build());

        // 增加按钮
        this.addRenderableWidget(Button.builder(Component.literal("+"), (btn) -> {
            int change = hasShiftDown() ? 16 : 1;
            this.purchaseQuantity += change;
            this.quantityEditBox.setValue(String.valueOf(this.purchaseQuantity));
        }).bounds(this.leftPos + 75, this.topPos + 110, 16, 16).build());

        // 确定按钮
        this.confirmButton = Button.builder(Component.translatable("gui.otherworldinn.store.confirm"), (btn) -> {
            if (this.selectedItem != null) {
                this.addToCart(this.selectedItem, this.purchaseQuantity);
            }
        }).bounds(this.leftPos + 10, this.topPos + 130, 80, 20).build();
        this.addRenderableWidget(this.confirmButton);

        // 购买按钮 (右侧)
        this.purchaseButton = Button.builder(Component.empty(), (btn) -> {
            if (this.menu.getStoreEntity() != null && !this.cart.isEmpty()) {
                // 构建购买列表
                List<C2SStorePurchasePacket.PurchaseItem> purchaseItems = new ArrayList<>();
                for (StoreEntity.StoreItem cartItem : this.cart) {
                    purchaseItems.add(new C2SStorePurchasePacket.PurchaseItem(cartItem.getItemStack(), cartItem.getCurrentStock()));
                }
                
                // 发送数据包
                ModMessages.sendToServer(new C2SStorePurchasePacket(this.menu.getStoreEntity().getId(), purchaseItems));
                
                // 清空购物车并关闭界面 (或者只清空)
                this.cart.clear();
                this.updateButtons();
                this.onClose(); // 购买成功后关闭界面，体验较好
            }
        }).bounds(this.leftPos + 150, this.topPos + 130, 90, 20).build();
        this.addRenderableWidget(this.purchaseButton);
        
        this.updateButtons();
    }

    private void addToCart(StoreEntity.StoreItem item, int quantity) {
        // 计算当前购物车中已有的该商品数量
        int existingQuantity = 0;
        int existingIndex = -1;
        
        for (int i = 0; i < this.cart.size(); i++) {
            StoreEntity.StoreItem cartItem = this.cart.get(i);
            if (ItemStack.isSameItemSameComponents(cartItem.getItemStack(), item.getItemStack())) {
                existingQuantity = cartItem.getCurrentStock();
                existingIndex = i;
                break;
            }
        }
        
        int newQuantity = existingQuantity + quantity;
        
        // 检查库存限制 (如果 maxStock 不是 -1)
        if (item.getMaxStock() != -1 && newQuantity > item.getCurrentStock()) {
             // 如果超过库存，则只添加到剩余库存量
             newQuantity = item.getCurrentStock();
             
             // 如果购物车里已经满了库存，不再添加
             if (existingQuantity >= item.getCurrentStock()) {
                 return; // 已达上限
             }
        }
        
        if (existingIndex != -1) {
            StoreEntity.StoreItem cartItem = this.cart.get(existingIndex);
            this.cart.set(existingIndex, new StoreEntity.StoreItem(cartItem.getItemStack(), cartItem.getPrice(), -1, newQuantity));
        } else {
            this.cart.add(new StoreEntity.StoreItem(item.getItemStack(), item.getPrice(), -1, newQuantity));
        }
        
        this.updateButtons();
    }

    private void updateButtons() {
        boolean hasSelection = this.selectedItem != null;
        this.confirmButton.active = hasSelection;
        this.quantityEditBox.setEditable(hasSelection);
        
        int totalPrice = 0;
        for (StoreEntity.StoreItem item : this.cart) {
            totalPrice += item.getPrice() * item.getCurrentStock(); // 这里 currentStock 借用来存购买数量
        }
        
        int playerBalance = 0;
        if (this.minecraft != null && this.minecraft.level != null) {
            TeamData teamData = TeamManager.getInstance().getClientTeamCache();
            if (teamData != null) {
                playerBalance = teamData.getCoins();
            }
        }
        
        boolean canAfford = playerBalance >= totalPrice;
        
        Component priceText = Component.translatable("gui.otherworldinn.store.purchase", totalPrice);
        if (!canAfford) {
            priceText = priceText.copy().withStyle(style -> style.withColor(ModColors.ERROR));
        }
        
        this.purchaseButton.setMessage(priceText);
        this.purchaseButton.active = !this.cart.isEmpty() && canAfford;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // 渲染购物车列表 (右侧)
        int leftPos = (this.width - this.imageWidth) / 2;
        int topPos = (this.height - this.imageHeight) / 2;
        int listX = leftPos + 150;
        int listY = topPos + 20;
        
        // 滚动条相关参数
        int scrollBarX = listX + 90;
        int scrollBarY = listY;
        int scrollBarHeight = CART_DISPLAY_ROWS * CART_ITEM_HEIGHT;
        boolean canScroll = this.cart.size() > CART_DISPLAY_ROWS;
        
        // 渲染滚动条背景
        guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + SCROLL_BAR_WIDTH, scrollBarY + scrollBarHeight, 0xFF202020);
        
        // 渲染滚动滑块
        if (canScroll) {
            int sliderHeight = (int) ((float) (scrollBarHeight * scrollBarHeight) / (float) (this.cart.size() * CART_ITEM_HEIGHT));
            sliderHeight = Math.max(32, sliderHeight);
            int sliderY = scrollBarY + (int) ((float) (scrollBarHeight - sliderHeight) * this.scrollOffs);
            guiGraphics.fill(scrollBarX, sliderY, scrollBarX + SCROLL_BAR_WIDTH, sliderY + sliderHeight, 0xFF808080);
            guiGraphics.fill(scrollBarX, sliderY, scrollBarX + SCROLL_BAR_WIDTH - 1, sliderY + sliderHeight - 1, 0xFFC0C0C0);
        } else {
             // 禁用状态滑块
             guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + SCROLL_BAR_WIDTH, scrollBarY + scrollBarHeight, 0xFF404040);
        }

        // 计算可见区域
        int startIndex = 0;
        if (canScroll) {
            startIndex = (int) (this.scrollOffs * (this.cart.size() - CART_DISPLAY_ROWS));
        }
        
        // 启用剪裁以限制列表显示区域
        guiGraphics.enableScissor(listX, listY, listX + 100, listY + scrollBarHeight);
        
        for (int i = startIndex; i < this.cart.size() && i < startIndex + CART_DISPLAY_ROWS + 1; i++) {
            StoreEntity.StoreItem item = this.cart.get(i);
            int y = listY + (i - startIndex) * CART_ITEM_HEIGHT;
            
            guiGraphics.renderItem(item.getItemStack(), listX, y);
            guiGraphics.renderItemDecorations(this.font, item.getItemStack(), listX, y);
            guiGraphics.drawString(this.font, Component.literal("×" + item.getCurrentStock()), listX + 20, y + 5, 0xFFFFFF);
            guiGraphics.drawString(this.font, Component.literal(item.getPrice() * item.getCurrentStock() + "§f\uE001§r"), listX + 50, y + 5, 0xFFFF00);
        }
        
        guiGraphics.disableScissor();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 不渲染 "Inventory" 和 "Hotbar" 标签
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        
        // 渲染玩家余额 (居中)
        int playerBalance = 0;
        if (this.minecraft != null && this.minecraft.level != null) {
            TeamData teamData = TeamManager.getInstance().getClientTeamCache();
            if (teamData != null) {
                playerBalance = teamData.getCoins();
            }
        }
        
        Component balanceText = Component.literal("§f\uE001§r " + playerBalance);
        int balanceWidth = this.font.width(balanceText);
        guiGraphics.drawString(this.font, balanceText, (this.imageWidth - balanceWidth) / 2, 6, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // 渲染背景
        guiGraphics.blit(this.backgroundTexture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        
        // 渲染商品网格
        int startX = this.leftPos + 10;
        int startY = this.topPos + 20;
        
        List<StoreEntity.StoreItem> items = this.menu.getStoreItems();
        for (int i = 0; i < items.size(); i++) {
            if (i >= GRID_COLS * GRID_ROWS) break;
            
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            int x = startX + col * (SLOT_SIZE + SLOT_SPACING);
            int y = startY + row * (SLOT_SIZE + SLOT_SPACING);
            
            // 绘制槽位背景
            guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF404040);
            
            // 绘制物品
            StoreEntity.StoreItem storeItem = items.get(i);
            boolean isOutOfStock = storeItem.getMaxStock() != -1 && storeItem.getCurrentStock() <= 0;
            
            if (isOutOfStock) {
                // 绘制灰色遮罩
                guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xA0000000);
            }
            
            guiGraphics.renderItem(storeItem.getItemStack(), x + 1, y + 1);
            guiGraphics.renderItemDecorations(this.font, storeItem.getItemStack(), x + 1, y + 1);
            
            // 选中高亮 (最后绘制以覆盖在物品上方，确保可见)
            if (items.get(i) == this.selectedItem) {
                // 为了确保可见性，我们使用 fill 绘制四条边框线，而不是 renderOutline
                int color = 0xFFF8F8FF;
                int borderSize = 1;
                guiGraphics.fill(x - borderSize, y - borderSize, x + SLOT_SIZE + borderSize, y, color); // 上
                guiGraphics.fill(x - borderSize, y + SLOT_SIZE, x + SLOT_SIZE + borderSize, y + SLOT_SIZE + borderSize, color); // 下
                guiGraphics.fill(x - borderSize, y, x, y + SLOT_SIZE, color); // 左
                guiGraphics.fill(x + SLOT_SIZE, y, x + SLOT_SIZE + borderSize, y + SLOT_SIZE, color); // 右
            }
        }
        
        // 渲染选中物品名称
        if (this.selectedItem != null) {
            Component name = this.selectedItem.getItemStack().getHoverName();
            guiGraphics.drawString(this.font, name, this.leftPos + 30, this.topPos + 100, 0xFFFFFF);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.cart.size() > CART_DISPLAY_ROWS) {
            // 每个滚轮单位滚动一项
            float scrollStep = 1.0F / (float)(this.cart.size() - CART_DISPLAY_ROWS);
            this.scrollOffs = (float)((double)this.scrollOffs - scrollY * (double)scrollStep);
            this.scrollOffs = net.minecraft.util.Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 先检查父类的点击处理 (例如输入框)
        if (this.quantityEditBox.mouseClicked(mouseX, mouseY, button)) {
             return true;
        }
        if (this.confirmButton.mouseClicked(mouseX, mouseY, button)) {
             return true;
        }
        if (this.purchaseButton.mouseClicked(mouseX, mouseY, button)) {
             return true;
        }
        // +/- 按钮
        for (net.minecraft.client.gui.components.events.GuiEventListener child : this.children()) {
             if (child instanceof Button && child.mouseClicked(mouseX, mouseY, button)) {
                 return true;
             }
        }
        
        // 检查点击商品
        int startX = this.leftPos + 10;
        int startY = this.topPos + 20;
        List<StoreEntity.StoreItem> items = this.menu.getStoreItems();
        
        for (int i = 0; i < items.size(); i++) {
            if (i >= GRID_COLS * GRID_ROWS) break;
            
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            int x = startX + col * (SLOT_SIZE + SLOT_SPACING);
            int y = startY + row * (SLOT_SIZE + SLOT_SPACING);
            
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                // 如果没有库存，不允许选择
                if (items.get(i).getMaxStock() != -1 && items.get(i).getCurrentStock() <= 0) {
                     return false;
                }

                if (this.selectedItem != items.get(i)) {
                    this.selectedItem = items.get(i);
                    // 重置购买数量为 1
                    this.purchaseQuantity = 1;
                    this.quantityEditBox.setValue("1");
                    this.updateButtons();
                }
                
                // Shift + 点击：快速添加 64 个 (或剩余库存)
                if (hasShiftDown()) {
                    int addAmount = 64;
                    if (this.selectedItem.getMaxStock() != -1) {
                        // 考虑剩余库存
                        addAmount = Math.min(addAmount, this.selectedItem.getCurrentStock());
                    }
                    this.addToCart(this.selectedItem, addAmount);
                }
                
                // 播放点击音效
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // 渲染商品 Tooltip
        int startX = this.leftPos + 10;
        int startY = this.topPos + 20;
        List<StoreEntity.StoreItem> items = this.menu.getStoreItems();
        
        for (int i = 0; i < items.size(); i++) {
            if (i >= GRID_COLS * GRID_ROWS) break;
            
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            int x = startX + col * (SLOT_SIZE + SLOT_SPACING);
            int y = startY + row * (SLOT_SIZE + SLOT_SPACING);
            
            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                StoreEntity.StoreItem item = items.get(i);
                List<Component> tooltip = getTooltipFromItem(minecraft, item.getItemStack());
                
                // 使用翻译键和自定义图标
                tooltip.add(Component.translatable("gui.otherworldinn.store.price", item.getPrice()).withStyle(net.minecraft.ChatFormatting.YELLOW));
                
                if (item.getMaxStock() != -1) {
                     tooltip.add(Component.translatable("gui.otherworldinn.store.stock", item.getCurrentStock(), item.getMaxStock()).withStyle(net.minecraft.ChatFormatting.GRAY));
                } else {
                     tooltip.add(Component.translatable("gui.otherworldinn.store.stock.infinite").withStyle(net.minecraft.ChatFormatting.GRAY));
                }
                guiGraphics.renderTooltip(this.font, tooltip, item.getItemStack().getTooltipImage(), mouseX, mouseY);
            }
        }
    }
}
