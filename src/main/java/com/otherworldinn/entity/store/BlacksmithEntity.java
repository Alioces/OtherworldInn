package com.otherworldinn.entity.store;

import com.otherworldinn.OtherworldInn;
import com.simibubi.create.AllItems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 铁匠实体
 * <p>
 * 出售粗矿和矿锭 (不出售宝石)。
 * </p>
 */
public class BlacksmithEntity extends StoreEntity {

    public BlacksmithEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // 初始化商品列表
        if (!level.isClientSide) {
             this.initDefaultStoreItems();
        }
    }

    private void initDefaultStoreItems() {
        // 粗矿
        this.addStoreItem(new ItemStack(Items.RAW_IRON), 5, 64);
        this.addStoreItem(new ItemStack(AllItems.RAW_ZINC.get()), 4, 64);
        this.addStoreItem(new ItemStack(Items.RAW_COPPER), 3, 64);
        this.addStoreItem(new ItemStack(Items.RAW_GOLD), 8, 32);

        // 矿锭
        this.addStoreItem(new ItemStack(Items.IRON_INGOT), 10, 32);
        this.addStoreItem(new ItemStack(AllItems.ZINC_INGOT.get()), 8, 32);
        this.addStoreItem(new ItemStack(Items.COPPER_INGOT), 6, 32);
        this.addStoreItem(new ItemStack(Items.GOLD_INGOT), 15, 16);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D) // 不动
                .add(Attributes.KNOCKBACK_RESISTANCE, 25565.0D); // 抗击退
    }

    @Override
    public ResourceLocation getStoreBackground() {
        return ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "textures/gui/store/blacksmith.png");
    }
}
