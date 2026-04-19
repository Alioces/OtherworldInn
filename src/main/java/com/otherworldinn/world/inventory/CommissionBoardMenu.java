package com.otherworldinn.world.inventory;

import com.otherworldinn.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class CommissionBoardMenu extends AbstractContainerMenu {

    public CommissionBoardMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.COMMISSION_BOARD_MENU.get(), containerId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
