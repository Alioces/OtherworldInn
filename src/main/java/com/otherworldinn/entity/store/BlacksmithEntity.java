package com.otherworldinn.entity.store;

import com.otherworldinn.OtherworldInn;
import com.simibubi.create.AllItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * 铁匠实体
 * <p>
 * 出售粗矿和矿锭
 * 也会随机刷新一些带有损耗或附魔的铁制工具。
 * </p>
 */
public class BlacksmithEntity extends StoreEntity {

    // 随机工具池
    private static final List<RandomItemData> TOOL_POOL = new ArrayList<>();

    public BlacksmithEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // 初始化商品列表
        if (!level.isClientSide) {
             this.initDefaultStoreItems();
             
             // 初始化工具池 (如果为空)
             if (TOOL_POOL.isEmpty()) {
                 initToolPool();
             }
        }
    }

    private void initToolPool() {
        // 定义一个通用的随机耐久和附魔修改器
        java.util.function.Consumer<ItemStack> randomToolModifier = (stack) -> {
            RandomSource random = this.getRandom();
            // 随机耐久损耗 (10% - 50%)
            int maxDamage = stack.getMaxDamage();
            int damage = (int) (maxDamage * (0.1f + random.nextFloat() * 0.4f));
            stack.setDamageValue(damage);
            
            // 随机附魔 (低级)
            if (random.nextBoolean()) {
                this.registryAccess().lookup(Registries.ENCHANTMENT)
                    .flatMap(reg -> reg.get(Enchantments.EFFICIENCY))
                    .ifPresent(enchantment -> stack.enchant(enchantment, 1 + random.nextInt(2)));
            }
            if (random.nextFloat() < 0.3f) {
                this.registryAccess().lookup(Registries.ENCHANTMENT)
                    .flatMap(reg -> reg.get(Enchantments.UNBREAKING))
                    .ifPresent(enchantment -> stack.enchant(enchantment, 1));
            }
        };

        // 添加铁制工具到池子
        TOOL_POOL.add(new RandomItemData(Items.IRON_PICKAXE, 25, 35, 1, 1, 10, randomToolModifier));
        TOOL_POOL.add(new RandomItemData(Items.IRON_AXE, 20, 30, 1, 1, 10, randomToolModifier));
        TOOL_POOL.add(new RandomItemData(Items.IRON_SHOVEL, 10, 20, 1, 1, 10, randomToolModifier));
        TOOL_POOL.add(new RandomItemData(Items.IRON_SWORD, 15, 25, 1, 1, 10, randomToolModifier));
        TOOL_POOL.add(new RandomItemData(Items.IRON_HOE, 10, 20, 1, 1, 5, randomToolModifier));
        
        // 偶尔出现金工具
        TOOL_POOL.add(new RandomItemData(Items.GOLDEN_PICKAXE, 20, 30, 1, 1, 5, randomToolModifier));
        TOOL_POOL.add(new RandomItemData(Items.GOLDEN_SWORD, 20, 30, 1, 1, 5, randomToolModifier));
    }

    @Override
    public void tick() {
        super.tick();
        // 初始生成随机商品
        if (!this.level().isClientSide && this.storeItems.size() == this.fixedItemsCount && !TOOL_POOL.isEmpty()) {
             this.refreshRandomItems();
        }
    }
    
    @Override
    protected void refreshRandomItems() {
        super.refreshRandomItems();
        if (TOOL_POOL.isEmpty()) {
            initToolPool();
        }
        // 随机抽取 2-4 件工具
        this.generateRandomItems(TOOL_POOL, 2, 4);
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
