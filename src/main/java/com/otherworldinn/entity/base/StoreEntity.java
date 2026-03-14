package com.otherworldinn.entity.base;

import com.otherworldinn.world.inventory.StoreMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 商店实体抽象父类
 *
 * <p>特性： 1. 右键打开商店界面 (子类实现) 2. 原地不动，无 AI 3. 不受重力影响 4. 免疫绝大部分伤害 (除了 /kill 和创造模式玩家) 5. 无受伤变红效果 6.
 * 持续播放循环动画 (客户端逻辑) 7. 存储商品列表 (物品、数量、售价)
 */
public abstract class StoreEntity extends PathfinderMob {
    private static final int MAX_FAVOR_LEVEL = 10;
    private static final int COINS_PER_FAVOR_LEVEL = 500;
    private static final double MAX_LEVEL_DISCOUNT_RATE = 0.7D;

    public static int getMaxFavorLevelValue() {
        return MAX_FAVOR_LEVEL;
    }

    public static int getCoinsPerFavorLevelValue() {
        return COINS_PER_FAVOR_LEVEL;
    }

    public static int getDiscountedPriceForFavorLevel(int basePrice, int favorLevel) {
        if (favorLevel >= MAX_FAVOR_LEVEL) {
            return Math.max(1, (int) Math.floor(basePrice * MAX_LEVEL_DISCOUNT_RATE));
        }
        return basePrice;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D) // 不动
                .add(Attributes.KNOCKBACK_RESISTANCE, 25565.0D); // 抗击退
    }

    /** 商品列表 (合并了固定商品和随机商品) */
    protected final List<StoreItem> storeItems = new ArrayList<>();

    /**
     * 固定商品起始索引
     *
     * <p>在刷新库存时，保留索引在此之前的商品，移除之后的随机商品并重新生成。
     */
    protected int fixedItemsCount = 0;

    /** 上次进货的日期 (GameTime / 24000) */
    private long lastRestockDay = 0;

    private int totalSpentCoins = 0;
    private int favorLevel = 1;
    private final List<FavorStoreItemData> favorStoreItems = new ArrayList<>();

    /**
     * 待机/循环动画状态
     *
     * <p>子类模型需要使用此状态来播放动画。
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
                SoundEvent openSound = this.getOpenStoreSound();
                if (openSound != null) {
                    this.level()
                            .playSound(
                                    null,
                                    this.blockPosition(),
                                    openSound,
                                    this.getSoundSource(),
                                    1.0F,
                                    1.0F);
                }
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
     *
     * <p>默认实现尝试打开 MenuProvider。 子类可以覆盖此方法以自定义打开逻辑。
     *
     * @param player 交互的玩家
     */
    protected void openStoreScreen(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider menuProvider =
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new StoreMenu(id, inventory, this),
                            this.getDisplayName());
            serverPlayer.openMenu(
                    menuProvider,
                    (buf) -> {
                        buf.writeInt(this.getId()); // 传递实体 ID 以便客户端获取实体
                        buf.writeResourceLocation(
                                BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()));
                        buf.writeInt(this.favorLevel);
                        buf.writeInt(this.totalSpentCoins);

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
     *
     * <p>子类可以覆盖此方法以自定义背景。 默认为 "textures/gui/store.png"
     *
     * @return 背景纹理 ResourceLocation
     */
    public ResourceLocation getStoreBackground() {
        return ResourceLocation.fromNamespaceAndPath("otherworldinn", "textures/gui/store.png");
    }

    @Nullable
    protected SoundEvent getOpenStoreSound() {
        return SoundEvents.UI_BUTTON_CLICK.value();
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
     * 添加固定商品 (通过 ResourceLocation)
     *
     * @param itemId 物品 ID (例如 "minecraft:apple" 或 "create:zinc_ingot")
     * @param price 价格
     * @param maxStock 最大库存
     */
    public void addStoreItem(String itemId, int price, int maxStock) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl != null) {
            BuiltInRegistries.ITEM
                    .getOptional(rl)
                    .ifPresent(item -> this.addStoreItem(new ItemStack(item), price, maxStock));
        }
    }

    /**
     * 添加固定商品 (通过 ResourceLocation, 带修改器)
     *
     * @param itemId 物品 ID
     * @param price 价格
     * @param maxStock 最大库存
     * @param modifier 修改器
     */
    public void addStoreItem(String itemId, int price, int maxStock, Consumer<ItemStack> modifier) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl != null) {
            BuiltInRegistries.ITEM
                    .getOptional(rl)
                    .ifPresent(
                            item ->
                                    this.addStoreItem(
                                            new ItemStack(item), price, maxStock, modifier));
        }
    }

    /**
     * 添加固定商品
     *
     * <p>这些商品在刷新时不会被移除，只会补充库存。
     *
     * @param item 物品
     * @param price 价格
     * @param maxStock 最大库存 (-1 表示无限)
     */
    public void addStoreItem(ItemStack item, int price, int maxStock) {
        this.storeItems.add(new StoreItem(item, price, maxStock));
        this.fixedItemsCount = this.storeItems.size(); // 更新固定商品数量
    }

    public void addLimitedStoreItem(String itemId, int price, int maxStock) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl != null) {
            BuiltInRegistries.ITEM
                    .getOptional(rl)
                    .ifPresent(
                            item -> this.addLimitedStoreItem(new ItemStack(item), price, maxStock));
        }
    }

    public void addLimitedStoreItem(
            String itemId, int price, int maxStock, Consumer<ItemStack> modifier) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl != null) {
            BuiltInRegistries.ITEM
                    .getOptional(rl)
                    .ifPresent(
                            item ->
                                    this.addLimitedStoreItem(
                                            new ItemStack(item), price, maxStock, modifier));
        }
    }

    public void addLimitedStoreItem(ItemStack item, int price, int maxStock) {
        this.storeItems.add(new StoreItem(item, price, maxStock, maxStock, 1, false));
        this.fixedItemsCount = this.storeItems.size();
    }

    public void addLimitedStoreItem(
            ItemStack item, int price, int maxStock, Consumer<ItemStack> modifier) {
        ItemStack copy = item.copy();
        if (modifier != null) {
            modifier.accept(copy);
        }
        this.addLimitedStoreItem(copy, price, maxStock);
    }

    /**
     * 添加固定商品 (带自定义设置)
     *
     * @param item 物品
     * @param price 价格
     * @param maxStock 最大库存
     * @param modifier 对物品栈的自定义修改操作 (例如设置耐久、附魔等)
     */
    public void addStoreItem(
            ItemStack item, int price, int maxStock, Consumer<ItemStack> modifier) {
        ItemStack copy = item.copy();
        if (modifier != null) {
            modifier.accept(copy);
        }
        this.addStoreItem(copy, price, maxStock);
    }

    public int getFavorLevel() {
        return this.favorLevel;
    }

    public int getTotalSpentCoins() {
        return this.totalSpentCoins;
    }

    public boolean canPurchase(StoreItem item) {
        return item.getRequiredFavorLevel() <= this.favorLevel;
    }

    public int getPurchasePrice(StoreItem item) {
        return getDiscountedPriceForFavorLevel(item.getPrice(), this.favorLevel);
    }

    public void addSpentCoins(int spentCoins) {
        if (spentCoins <= 0) {
            return;
        }
        this.totalSpentCoins += spentCoins;
        int newLevel = this.calculateFavorLevel(this.totalSpentCoins);
        if (newLevel != this.favorLevel) {
            this.favorLevel = newLevel;
            this.unlockFavorStoreItems();
        }
    }

    public void addFavorStoreItem(int requiredFavorLevel, ItemStack item, int price, int maxStock) {
        this.addFavorStoreItem(requiredFavorLevel, item, price, maxStock, null);
    }

    public void addFavorStoreItem(
            int requiredFavorLevel,
            ItemStack item,
            int price,
            int maxStock,
            Consumer<ItemStack> modifier) {
        if (requiredFavorLevel < 2 || requiredFavorLevel > MAX_FAVOR_LEVEL) {
            return;
        }
        ItemStack copy = item.copy();
        if (modifier != null) {
            modifier.accept(copy);
        }
        this.favorStoreItems.add(new FavorStoreItemData(requiredFavorLevel, copy, price, maxStock));
        if (this.hasFixedItem(copy, price, maxStock, requiredFavorLevel)) {
            return;
        }
        int insertIndex = Math.min(this.fixedItemsCount, this.storeItems.size());
        this.storeItems.add(
                insertIndex, new StoreItem(copy, price, maxStock, maxStock, requiredFavorLevel));
        this.fixedItemsCount++;
    }

    public void addFavorStoreItem(int requiredFavorLevel, String itemId, int price, int maxStock) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl != null) {
            BuiltInRegistries.ITEM
                    .getOptional(rl)
                    .ifPresent(
                            item ->
                                    this.addFavorStoreItem(
                                            requiredFavorLevel,
                                            new ItemStack(item),
                                            price,
                                            maxStock));
        }
    }

    public void addFavorStoreItem(
            int requiredFavorLevel,
            String itemId,
            int price,
            int maxStock,
            Consumer<ItemStack> modifier) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl != null) {
            BuiltInRegistries.ITEM
                    .getOptional(rl)
                    .ifPresent(
                            item ->
                                    this.addFavorStoreItem(
                                            requiredFavorLevel,
                                            new ItemStack(item),
                                            price,
                                            maxStock,
                                            modifier));
        }
    }

    public List<StoreItem> getStoreItems() {
        return this.storeItems;
    }

    private int calculateFavorLevel(int spentCoins) {
        int level = 1 + (spentCoins / COINS_PER_FAVOR_LEVEL);
        if (level < 1) {
            return 1;
        }
        return Math.min(level, MAX_FAVOR_LEVEL);
    }

    private void unlockFavorStoreItems() {
        for (FavorStoreItemData favorItem : this.favorStoreItems) {
            if (this.hasFixedItem(
                    favorItem.itemStack(),
                    favorItem.price(),
                    favorItem.maxStock(),
                    favorItem.requiredFavorLevel())) {
                continue;
            }
            int insertIndex = Math.min(this.fixedItemsCount, this.storeItems.size());
            this.storeItems.add(
                    insertIndex,
                    new StoreItem(
                            favorItem.itemStack().copy(),
                            favorItem.price(),
                            favorItem.maxStock(),
                            favorItem.maxStock(),
                            favorItem.requiredFavorLevel()));
            this.fixedItemsCount++;
        }
    }

    private boolean hasFixedItem(
            ItemStack itemStack, int price, int maxStock, int requiredFavorLevel) {
        for (int i = 0; i < this.fixedItemsCount && i < this.storeItems.size(); i++) {
            StoreItem existing = this.storeItems.get(i);
            if (ItemStack.isSameItemSameComponents(existing.getItemStack(), itemStack)
                    && existing.getPrice() == price
                    && existing.getMaxStock() == maxStock
                    && existing.isRestockable()
                    && existing.getRequiredFavorLevel() == requiredFavorLevel) {
                return true;
            }
        }
        return false;
    }

    /** 补货所有商品并刷新随机商品 */
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
     *
     * <p>默认实现移除所有随机商品。子类覆盖此方法以生成新的随机商品。
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
     * @param pool 商品池
     * @param minTypes 最少抽取的商品种类数量
     * @param maxTypes 最多抽取的商品种类数量
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
                this.addRandomStoreItem(
                        new ItemStack(selected.item),
                        selected.minPrice,
                        selected.maxPrice,
                        selected.minStock,
                        selected.maxStock,
                        selected.modifier);
            }
        }
    }

    /**
     * 添加随机商品 (带范围随机)
     *
     * @param item 物品
     * @param minPrice 最小价格
     * @param maxPrice 最大价格
     * @param minStock 最小库存
     * @param maxStock 最大库存
     */
    protected void addRandomStoreItem(
            ItemStack item, int minPrice, int maxPrice, int minStock, int maxStock) {
        this.addRandomStoreItem(item, minPrice, maxPrice, minStock, maxStock, null);
    }

    /**
     * 添加随机商品 (带范围随机和修改器)
     *
     * @param item 物品
     * @param minPrice 最小价格
     * @param maxPrice 最大价格
     * @param minStock 最小库存
     * @param maxStock 最大库存
     * @param modifier 修改器
     */
    protected void addRandomStoreItem(
            ItemStack item,
            int minPrice,
            int maxPrice,
            int minStock,
            int maxStock,
            java.util.function.Consumer<ItemStack> modifier) {
        net.minecraft.util.RandomSource random = this.getRandom();
        int price = minPrice + random.nextInt(Math.max(1, maxPrice - minPrice + 1));
        int stock = minStock + random.nextInt(Math.max(1, maxStock - minStock + 1));

        ItemStack stack = item.copy();
        if (modifier != null) {
            modifier.accept(stack);
        }
        this.storeItems.add(new StoreItem(stack, price, stock));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putLong("LastRestockDay", this.lastRestockDay);
        compound.putInt("FixedItemsCount", this.fixedItemsCount);
        compound.putInt("FavorSpentCoins", this.totalSpentCoins);
        compound.putInt("FavorLevel", this.favorLevel);
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
        if (compound.contains("FavorSpentCoins")) {
            this.totalSpentCoins = compound.getInt("FavorSpentCoins");
        }
        if (compound.contains("FavorLevel")) {
            this.favorLevel = compound.getInt("FavorLevel");
        } else {
            this.favorLevel = this.calculateFavorLevel(this.totalSpentCoins);
        }
        this.favorLevel = Math.max(1, Math.min(MAX_FAVOR_LEVEL, this.favorLevel));
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
        this.fixedItemsCount = Math.min(this.fixedItemsCount, this.storeItems.size());
        this.unlockFavorStoreItems();
    }

    /** 随机商品池条目 */
    public record RandomItemData(
            net.minecraft.world.item.Item item,
            int minPrice,
            int maxPrice,
            int minStock,
            int maxStock,
            int weight,
            @Nullable java.util.function.Consumer<ItemStack> modifier) {
        public RandomItemData(
                net.minecraft.world.item.Item item,
                int minPrice,
                int maxPrice,
                int minStock,
                int maxStock,
                int weight) {
            this(item, minPrice, maxPrice, minStock, maxStock, weight, null);
        }
    }

    public record FavorStoreItemData(
            int requiredFavorLevel, ItemStack itemStack, int price, int maxStock) {}

    /** 商品条目内部类 */
    public static class StoreItem {
        private final ItemStack itemStack;
        private int price;
        private int maxStock;
        private int currentStock;
        private final int requiredFavorLevel;
        private final boolean restockable;

        public StoreItem(ItemStack itemStack, int price) {
            this(itemStack, price, -1);
        }

        public StoreItem(ItemStack itemStack, int price, int maxStock) {
            this(itemStack, price, maxStock, maxStock, 1, true);
        }

        public StoreItem(ItemStack itemStack, int price, int maxStock, int currentStock) {
            this(itemStack, price, maxStock, currentStock, 1, true);
        }

        public StoreItem(
                ItemStack itemStack,
                int price,
                int maxStock,
                int currentStock,
                int requiredFavorLevel) {
            this(itemStack, price, maxStock, currentStock, requiredFavorLevel, true);
        }

        public StoreItem(
                ItemStack itemStack,
                int price,
                int maxStock,
                int currentStock,
                int requiredFavorLevel,
                boolean restockable) {
            this.itemStack = itemStack;
            this.price = price;
            this.maxStock = maxStock;
            this.currentStock = currentStock;
            this.requiredFavorLevel = Math.max(1, requiredFavorLevel);
            this.restockable = restockable;
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

        public int getRequiredFavorLevel() {
            return this.requiredFavorLevel;
        }

        public boolean isRestockable() {
            return this.restockable;
        }

        /** 是否无限库存 */
        public boolean isInfinite() {
            return maxStock < 0;
        }

        /** 是否售罄 */
        public boolean isSoldOut() {
            return !isInfinite() && currentStock <= 0;
        }

        /**
         * 尝试购买（扣减库存）
         *
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

        /** 补货 */
        public void restock() {
            if (this.restockable && !isInfinite()) {
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
            tag.putInt("RequiredFavorLevel", requiredFavorLevel);
            tag.putBoolean("Restockable", restockable);
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
            int requiredFavorLevel =
                    tag.contains("RequiredFavorLevel") ? tag.getInt("RequiredFavorLevel") : 1;
            boolean restockable = !tag.contains("Restockable") || tag.getBoolean("Restockable");
            return new StoreItem(
                    stack, price, maxStock, currentStock, requiredFavorLevel, restockable);
        }
    }
}
