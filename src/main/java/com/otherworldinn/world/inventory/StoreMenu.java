package com.otherworldinn.world.inventory;

import com.otherworldinn.entity.store.StoreEntity;
import com.otherworldinn.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;

/**
 * 商店菜单
 */
public class StoreMenu extends AbstractContainerMenu {

    private final StoreEntity storeEntity;
    private final List<StoreEntity.StoreItem> storeItems;
    private final int favorLevel;
    private final int totalSpentCoins;

    public StoreMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getEntity(playerInventory, extraData), readFavorLevel(extraData), readTotalSpentCoins(extraData), readStoreItems(playerInventory, extraData));
    }

    private static StoreEntity getEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        Entity entity = playerInventory.player.level().getEntity(extraData.readInt());
        if (entity instanceof StoreEntity storeEntity) {
            return storeEntity;
        }
        return null;
    }

    private static List<StoreEntity.StoreItem> readStoreItems(Inventory playerInventory, FriendlyByteBuf extraData) {
        List<StoreEntity.StoreItem> items = new ArrayList<>();
        int size = extraData.readInt();
        HolderLookup.Provider registryAccess = playerInventory.player.registryAccess();
        for (int i = 0; i < size; i++) {
            items.add(StoreEntity.StoreItem.load(registryAccess, extraData.readNbt()));
        }
        return items;
    }

    private static int readFavorLevel(FriendlyByteBuf extraData) {
        return extraData.readInt();
    }

    private static int readTotalSpentCoins(FriendlyByteBuf extraData) {
        return extraData.readInt();
    }

    public StoreMenu(int containerId, Inventory playerInventory, StoreEntity storeEntity) {
        this(containerId, playerInventory, storeEntity, storeEntity != null ? storeEntity.getFavorLevel() : 1, storeEntity != null ? storeEntity.getTotalSpentCoins() : 0, storeEntity != null ? storeEntity.getStoreItems() : new ArrayList<>());
    }

    protected StoreMenu(int containerId, Inventory playerInventory, StoreEntity storeEntity, int favorLevel, int totalSpentCoins, List<StoreEntity.StoreItem> storeItems) {
        super(ModMenuTypes.STORE_MENU.get(), containerId);
        this.storeEntity = storeEntity;
        this.favorLevel = favorLevel;
        this.totalSpentCoins = totalSpentCoins;
        this.storeItems = new ArrayList<>(storeItems);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return storeEntity != null && storeEntity.isAlive() && storeEntity.distanceTo(player) < 8.0f;
    }

    public StoreEntity getStoreEntity() {
        return storeEntity;
    }

    public List<StoreEntity.StoreItem> getStoreItems() {
        return storeItems;
    }

    public int getFavorLevel() {
        return this.favorLevel;
    }

    public int getTotalSpentCoins() {
        return this.totalSpentCoins;
    }
}
