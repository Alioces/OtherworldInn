package com.otherworldinn.entity.store;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.base.StoreEntity;
import com.simibubi.create.AllItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class FarmerEntity extends StoreEntity {
    public FarmerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WHEAT));
        equipDefaultStrawHat();
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        if (!level.isClientSide) {
            this.initDefaultStoreItems();
        }
    }

    private void equipDefaultStrawHat() {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.STRAW_HAT.get()));
    }

    private void initDefaultStoreItems() {
        this.addStoreItem(new ItemStack(Items.WHEAT_SEEDS), 1, 64);
        this.addStoreItem(new ItemStack(Items.BEETROOT_SEEDS), 1, 64);
        this.addStoreItem(new ItemStack(Items.PUMPKIN_SEEDS), 2, 64);
        this.addStoreItem(new ItemStack(Items.MELON_SEEDS), 2, 64);
        this.addStoreItem(new ItemStack(Items.WHEAT), 2, 64);
        this.addStoreItem(new ItemStack(Items.CARROT), 2, 64);
        this.addStoreItem(new ItemStack(Items.POTATO), 2, 64);
        this.addStoreItem(new ItemStack(Items.BEETROOT), 2, 64);
        this.addStoreItem(new ItemStack(Items.PUMPKIN), 4, 32);
        this.addStoreItem(new ItemStack(Items.MELON_SEEDS), 1, 64);
        this.addStoreItem(new ItemStack(Items.SUGAR_CANE), 2, 64);
        this.addStoreItem(new ItemStack(ModItems.TOMATO_SEED.get()), 2, 64);
        this.addStoreItem(new ItemStack(ModItems.CHILI_SEED.get()), 2, 64);
        this.addStoreItem(new ItemStack(ModItems.LETTUCE_SEED.get()), 2, 64);
        this.addStoreItem(new ItemStack(ModItems.WILD_RICE_SEED.get()), 2, 64);
        this.addFavorStoreItem(2, new ItemStack(Items.BONE_MEAL), 3, 64);
        this.addFavorStoreItem(4, new ItemStack(AllItems.TREE_FERTILIZER.get()), 6, 64);
        this.addFavorStoreItem(8, new ItemStack(Items.NETHERITE_HOE), 64, 1);
    }

    @Override
    public ResourceLocation getStoreBackground() {
        return ResourceLocation.fromNamespaceAndPath(
                OtherworldInn.MODID, "textures/gui/store/farmer.png");
    }

    @Override
    protected SoundEvent getOpenStoreSound() {
        return SoundEvents.COMPOSTER_FILL_SUCCESS;
    }
}
