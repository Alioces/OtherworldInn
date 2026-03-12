package com.otherworldinn.world.inn;

import com.otherworldinn.OtherworldInn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 家具管理器
 * <p>
 * 管理所有家具的属性配置，包括舒适度、光照度和湿度。
 * 支持为特定方块或方块标签配置属性。
 * </p>
 */
public class FurnitureManager {

    private static final Map<Block, FurnitureStats> BLOCK_STATS = new HashMap<>();
    private static final Map<TagKey<Block>, FurnitureStats> TAG_STATS = new HashMap<>();

    static {
        initDefaultFurniture();
    }

    /**
     * 初始化默认家具配置
     */
    private static void initDefaultFurniture() {
        // 配置原版床：舒适度 +15
        FurnitureStats bedStats = new FurnitureStats(15, 0, 0);
        TagKey<Block> bedsTag = TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.withDefaultNamespace("beds"));
        registerTag(bedsTag, bedStats);
    }

    /**
     * 为指定方块注册家具属性
     *
     * @param block 目标方块
     * @param stats 家具属性
     */
    public static void registerBlock(Block block, FurnitureStats stats) {
        BLOCK_STATS.put(block, stats);
    }

    /**
     * 为指定方块标签注册家具属性
     *
     * @param tag   目标方块标签
     * @param stats 家具属性
     */
    public static void registerTag(TagKey<Block> tag, FurnitureStats stats) {
        TAG_STATS.put(tag, stats);
    }

    /**
     * 获取指定方块的家具属性
     * <p>
     * 优先匹配方块本身的配置，其次匹配标签配置。
     * </p>
     *
     * @param block 目标方块
     * @return 对应的家具属性，如果未配置则返回空
     */
    public static Optional<FurnitureStats> getStats(Block block) {
        if (BLOCK_STATS.containsKey(block)) {
            return Optional.of(BLOCK_STATS.get(block));
        }

        // 检查标签匹配
        // 注意：这里需要遍历所有已注册的标签，可能会有性能影响
        // 对于服务端频繁查询，建议后续增加缓存机制
        var state = block.defaultBlockState();
        for (var entry : TAG_STATS.entrySet()) {
            if (state.is(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }

    /**
     * 家具属性记录类
     * <p>
     * 包含舒适度、光照度和湿度三个维度的数值。
     * </p>
     *
     * @param comfort  舒适度 (-20 ~ 20)
     * @param light    光照度 (-20 ~ 20)
     * @param humidity 湿度 (-20 ~ 20)
     */
    public record FurnitureStats(int comfort, int light, int humidity) {
    }

    /**
     * 客户端事件处理器
     */
    @EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
    public static class ClientHandler {
        /**
         * 处理物品提示框事件，显示家具属性
         *
         * @param event 物品提示框事件
         */
        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                getStats(block).ifPresent(stats -> {
                    if (stats.comfort != 0) {
                        event.getToolTip().add(Component.translatable("tooltip.otherworldinn.furniture.comfort", String.format("%+d", stats.comfort)).withStyle(style -> style.withColor(ModColors.COMFORT)));
                    }
                    if (stats.light != 0) {
                        event.getToolTip().add(Component.translatable("tooltip.otherworldinn.furniture.light", String.format("%+d", stats.light)).withStyle(style -> style.withColor(ModColors.LIGHT)));
                    }
                    if (stats.humidity != 0) {
                        event.getToolTip().add(Component.translatable("tooltip.otherworldinn.furniture.humidity", String.format("%+d", stats.humidity)).withStyle(style -> style.withColor(ModColors.HUMIDITY)));
                    }
                });
            }
        }
    }
}
