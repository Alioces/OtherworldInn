package com.otherworldinn.entity.store;

import com.otherworldinn.OtherworldInn;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 随机商人实体
 * <p>
 * 每天或生成时刷新随机商品。
 * </p>
 */
public class RandomMerchantEntity extends StoreEntity {

    // 随机商品池配置
    private static final List<RandomItemData> POOL = new ArrayList<>();

    static {
        // 初始化池子 (示例数据)
        // 食物
        POOL.add(new RandomItemData(Items.APPLE, 2, 5, 10, 32, 10));
        POOL.add(new RandomItemData(Items.BREAD, 3, 6, 10, 32, 10));
        POOL.add(new RandomItemData(Items.COOKED_BEEF, 5, 10, 5, 16, 8));
        POOL.add(new RandomItemData(Items.COOKED_PORKCHOP, 5, 10, 5, 16, 8));
        POOL.add(new RandomItemData(Items.GOLDEN_APPLE, 50, 100, 1, 5, 2));
        
        // 杂项
        POOL.add(new RandomItemData(Items.COAL, 2, 5, 16, 64, 15));
        POOL.add(new RandomItemData(Items.IRON_INGOT, 8, 15, 5, 32, 10));
        POOL.add(new RandomItemData(Items.GOLD_INGOT, 10, 20, 5, 16, 8));
        POOL.add(new RandomItemData(Items.DIAMOND, 50, 100, 1, 8, 5));
        POOL.add(new RandomItemData(Items.EMERALD, 30, 60, 1, 16, 5));
        
        // 稀有
        POOL.add(new RandomItemData(Items.ENDER_PEARL, 20, 40, 1, 8, 5));
        POOL.add(new RandomItemData(Items.EXPERIENCE_BOTTLE, 10, 20, 1, 16, 8));
        POOL.add(new RandomItemData(Items.NAME_TAG, 50, 100, 1, 1, 3));
    }

    public RandomMerchantEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        // 如果客户端或刚生成且列表为空，尝试刷新 (仅服务端)
        if (!this.level().isClientSide && this.storeItems.isEmpty()) {
            this.refreshRandomItems();
        }
    }

    @Override
    protected void refreshRandomItems() {
        super.refreshRandomItems(); // 清除旧的随机商品
        this.generateRandomItems(POOL, 5, 8);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("entity.otherworldinn.random_merchant");
    }

    @Override
    public ResourceLocation getStoreBackground() {
        // 复用铁匠铺背景，或者使用默认
        return ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "textures/gui/store/blacksmith.png");
    }

    /**
     * 商品池条目
     */
}
