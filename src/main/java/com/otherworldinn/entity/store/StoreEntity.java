package com.otherworldinn.entity.store;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import com.otherworldinn.world.inventory.StoreMenu;
import net.minecraft.resources.ResourceLocation;

/**
 * 商店实体抽象父类
 * <p>
 * 特性：
 * 1. 右键打开商店界面 (子类实现)
 * 2. 原地不动，无 AI
 * 3. 不受重力影响
 * 4. 免疫绝大部分伤害 (除了 /kill 和创造模式玩家)
 * 5. 无受伤变红效果
 * 6. 持续播放循环动画 (客户端逻辑)
 * 7. 存储商品列表 (物品、数量、售价)
 * </p>
 */
public abstract class StoreEntity extends PathfinderMob {

    /**
     * 商品列表 (合并了固定商品和随机商品)
     */
    protected final List<StoreItem> storeItems = new ArrayList<>();
    
    /**
     * 固定商品起始索引
     * <p>
     * 在刷新库存时，保留索引在此之前的商品，移除之后的随机商品并重新生成。
     * </p>
     */
    protected int fixedItemsCount = 0;

    /**
     * 上次进货的日期 (GameTime / 24000)
     */
    private long lastRestockDay = 0;

    /**
     * 待机/循环动画状态
     * <p>
     * 子类模型需要使用此状态来播放动画。
     * </p>
     */
    public final AnimationState idleAnimationState = new AnimationState();

    protected StoreEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true); // 无 AI (虽然 setNoAi 已经处理了大部分逻辑，但 PathfinderMob 还是会有寻路相关的初始化)
        this.setNoGravity(true); // 无重力
        this.setPersistenceRequired(); // 防止自然消失
    }

    @Override
    public void tick() {
        // 客户端动画逻辑
        if (this.level().isClientSide) {
            // 确保持续播放待机动画
            this.idleAnimationState.startIfStopped(this.tickCount);
        } else {
            // 服务端逻辑：每天早上重置库存
            // 计算当前天数
            long currentDay = this.level().getGameTime() / 24000L;
            // 检查是否是新的一天
            if (currentDay > this.lastRestockDay) {
                this.restockAll();
                this.lastRestockDay = currentDay;
            }
        }
        super.tick();
    }

    // --- 交互逻辑 ---

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        // 只有主手交互生效，防止触发两次
        if (hand == InteractionHand.MAIN_HAND) {
            if (!this.level().isClientSide) {
                // 打开商店界面 (服务端逻辑)
                this.openStoreScreen(player);
            }
            // 客户端返回 SUCCESS 播放交互动作 (挥手)
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * 打开商店界面
     * <p>
     * 默认实现尝试打开 MenuProvider。
     * 子类可以覆盖此方法以自定义打开逻辑。
     * </p>
     *
     * @param player 交互的玩家
     */
    protected void openStoreScreen(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider menuProvider = new SimpleMenuProvider(
                (id, inventory, p) -> new StoreMenu(id, inventory, this),
                this.getDisplayName()
            );
            serverPlayer.openMenu(menuProvider, (buf) -> {
                buf.writeInt(this.getId()); // 传递实体 ID 以便客户端获取实体
                
                // 序列化商品列表
                buf.writeInt(this.storeItems.size());
                for (StoreItem item : this.storeItems) {
                    buf.writeNbt(item.save(this.registryAccess()));
                }
            });
        }
    }

    /**
     * 获取商店背景纹理
     * <p>
     * 子类可以覆盖此方法以自定义背景。
     * 默认为 "textures/gui/store.png"
     * </p>
     * 
     * @return 背景纹理 ResourceLocation
     */
    public ResourceLocation getStoreBackground() {
        return ResourceLocation.fromNamespaceAndPath("otherworldinn", "textures/gui/store.png");
    }

    // --- 免疫与物理逻辑 ---

    @Override
    public boolean isPushable() {
        return false; // 不可被推动
    }

    @Override
    public void push(Entity entity) {
        // 覆盖为空，防止被推动
    }

    @Override
    protected void doPush(Entity entity) {
        // 覆盖为空，防止被推动
    }
    
    @Override
    public boolean isPickable() {
        // 可被选取 (攻击/交互)
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 仅允许创造模式玩家或 /kill (BYPASSES_INVULNERABILITY) 造成伤害
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        
        if (source.getEntity() instanceof Player player && player.isCreative()) {
             return super.hurt(source, amount);
        }
        
        return false;
    }

    @Override
    protected void pushEntities() {
        // 不推动其他实体
    }

    // 覆盖此方法以确保无重力效果生效，并防止因受击导致的位移
    @Override
    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        if (this.isNoGravity()) {
            this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        super.travel(travelVector);
    }
    
    // 拦截受伤变红效果 (EntityEvent ID 2)
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 2) { // 2 是受伤动画 (Damage)
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    protected @Nullable net.minecraft.sounds.SoundEvent getDeathSound() {
        return null; // 无死亡音效
    }

    // --- 商品管理 ---

    /**
     * 添加固定商品
     * <p>
     * 这些商品在刷新时不会被移除，只会补充库存。
     * </p>
     *
     * @param item 物品
     * @param price 价格
     * @param maxStock 最大库存 (-1 表示无限)
     */
    public void addStoreItem(ItemStack item, int price, int maxStock) {
        this.storeItems.add(new StoreItem(item, price, maxStock));
        this.fixedItemsCount = this.storeItems.size(); // 更新固定商品数量
    }
    
    public List<StoreItem> getStoreItems() {
        return this.storeItems;
    }

    /**
     * 补货所有商品并刷新随机商品
     */
    public void restockAll() {
        // 1. 补充固定商品库存
        for (int i = 0; i < this.fixedItemsCount && i < this.storeItems.size(); i++) {
            this.storeItems.get(i).restock();
        }
        
        // 2. 刷新随机商品
        this.refreshRandomItems();
    }
    
    /**
     * 刷新随机商品
     * <p>
     * 默认实现移除所有随机商品。子类覆盖此方法以生成新的随机商品。
     * </p>
     */
    protected void refreshRandomItems() {
        // 移除所有非固定商品
        if (this.storeItems.size() > this.fixedItemsCount) {
            this.storeItems.subList(this.fixedItemsCount, this.storeItems.size()).clear();
        }
    }

    /**
     * 生成随机商品
     *
     * @param pool      商品池
     * @param minTypes  最少抽取的商品种类数量
     * @param maxTypes  最多抽取的商品种类数量
     */
    protected void generateRandomItems(List<RandomItemData> pool, int minTypes, int maxTypes) {
        if (pool == null || pool.isEmpty()) return;
        
        net.minecraft.util.RandomSource random = this.getRandom();
        
        // 随机选择 minTypes 到 maxTypes 种商品
        int count = minTypes + random.nextInt(Math.max(1, maxTypes - minTypes + 1));
        List<RandomItemData> poolCopy = new ArrayList<>(pool);
        
        for (int i = 0; i < count; i++) {
            if (poolCopy.isEmpty()) break;
            
            // 按权重随机选择
            int totalWeight = poolCopy.stream().mapToInt(e -> e.weight).sum();
            int roll = random.nextInt(totalWeight);
            int current = 0;
            RandomItemData selected = null;
            
            for (RandomItemData entry : poolCopy) {
                current += entry.weight;
                if (roll < current) {
                    selected = entry;
                    break;
                }
            }
            
            if (selected != null) {
                poolCopy.remove(selected); // 避免重复
                this.addRandomStoreItem(new ItemStack(selected.item), selected.minPrice, selected.maxPrice, selected.minStock, selected.maxStock);
            }
        }
    }

    /**
     * 添加随机商品 (带范围随机)
     *
     * @param item      物品
     * @param minPrice  最小价格
     * @param maxPrice  最大价格
     * @param minStock  最小库存
     * @param maxStock  最大库存
     */
    protected void addRandomStoreItem(ItemStack item, int minPrice, int maxPrice, int minStock, int maxStock) {
        RandomSource random = this.getRandom();
        int price = minPrice + random.nextInt(Math.max(1, maxPrice - minPrice + 1));
        int stock = minStock + random.nextInt(Math.max(1, maxStock - minStock + 1));
        this.storeItems.add(new StoreItem(item, price, stock));
    }


    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putLong("LastRestockDay", this.lastRestockDay);
        compound.putInt("FixedItemsCount", this.fixedItemsCount);
        ListTag itemsTag = new ListTag();
        HolderLookup.Provider registryAccess = this.registryAccess();
        for (StoreItem storeItem : this.storeItems) {
            itemsTag.add(storeItem.save(registryAccess));
        }
        compound.put("StoreItems", itemsTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("LastRestockDay")) {
            this.lastRestockDay = compound.getLong("LastRestockDay");
        }
        if (compound.contains("FixedItemsCount")) {
            this.fixedItemsCount = compound.getInt("FixedItemsCount");
        }
        if (compound.contains("StoreItems", Tag.TAG_LIST)) {
            ListTag itemsTag = compound.getList("StoreItems", Tag.TAG_COMPOUND);
            HolderLookup.Provider registryAccess = this.registryAccess();
            this.storeItems.clear();
            for (Tag tag : itemsTag) {
                if (tag instanceof CompoundTag itemTag) {
                    this.storeItems.add(StoreItem.load(registryAccess, itemTag));
                }
            }
        }
    }

    /**
     * 随机商品池条目
     */
    public record RandomItemData(net.minecraft.world.item.Item item, int minPrice, int maxPrice, int minStock, int maxStock, int weight) {}

    /**
     * 商品条目内部类
     */
    public static class StoreItem {
        private final ItemStack itemStack;
        private int price;
        private int maxStock;
        private int currentStock;

        public StoreItem(ItemStack itemStack, int price) {
            this(itemStack, price, -1);
        }

        public StoreItem(ItemStack itemStack, int price, int maxStock) {
            this.itemStack = itemStack;
            this.price = price;
            this.maxStock = maxStock;
            this.currentStock = maxStock; // 默认满库存
        }

        public StoreItem(ItemStack itemStack, int price, int maxStock, int currentStock) {
            this.itemStack = itemStack;
            this.price = price;
            this.maxStock = maxStock;
            this.currentStock = currentStock;
        }

        public ItemStack getItemStack() {
            return itemStack;
        }

        public int getPrice() {
            return price;
        }

        public void setPrice(int price) {
            this.price = price;
        }

        public int getMaxStock() {
            return maxStock;
        }

        public int getCurrentStock() {
            return currentStock;
        }

        public void setCurrentStock(int currentStock) {
            this.currentStock = currentStock;
        }

        /**
         * 是否无限库存
         */
        public boolean isInfinite() {
            return maxStock < 0;
        }

        /**
         * 是否售罄
         */
        public boolean isSoldOut() {
            return !isInfinite() && currentStock <= 0;
        }

        /**
         * 尝试购买（扣减库存）
         * @return true 如果购买成功（有库存），false 如果售罄
         */
        public boolean tryPurchase() {
            if (isInfinite()) return true;
            if (currentStock > 0) {
                currentStock--;
                return true;
            }
            return false;
        }

        /**
         * 补货
         */
        public void restock() {
            if (!isInfinite()) {
                this.currentStock = this.maxStock;
            }
        }

        public CompoundTag save(HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            if (!itemStack.isEmpty()) {
                tag.put("Item", itemStack.save(provider, new CompoundTag()));
            }
            tag.putInt("Price", price);
            tag.putInt("MaxStock", maxStock);
            tag.putInt("CurrentStock", currentStock);
            return tag;
        }

        public static StoreItem load(HolderLookup.Provider provider, CompoundTag tag) {
            ItemStack stack = ItemStack.EMPTY;
            if (tag.contains("Item")) {
                stack = ItemStack.parse(provider, tag.getCompound("Item")).orElse(ItemStack.EMPTY);
            }
            int price = tag.getInt("Price");
            int maxStock = tag.contains("MaxStock") ? tag.getInt("MaxStock") : -1;
            int currentStock = tag.contains("CurrentStock") ? tag.getInt("CurrentStock") : maxStock;
            return new StoreItem(stack, price, maxStock, currentStock);
        }
    }
}
