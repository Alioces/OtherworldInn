package com.otherworldinn.world.economy.service;

import com.github.ysbbbbbb.kaleidoscopetavern.item.BottleBlockItem;
import com.github.ysbbbbbb.kaleidoscopetavern.item.DrinkBlockItem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 物品售价管理器
 *
 * <p>用于管理玩家向旅社出售物品的价格。 使用静态 Map 存储
 */
public class ItemSellPriceManager {

    private static final Map<ResourceLocation, Integer> PRICES = new HashMap<>();

    static {
        // 初始化默认价格
        // 原版水果
        addPrice("minecraft:apple", 3);
        addPrice("minecraft:sweet_berries", 2);
        addPrice("minecraft:glow_berries", 3);
        addPrice("minecraft:melon_slice", 2);

        // 原版熟肉类
        addPrice("minecraft:cooked_beef", 6);
        addPrice("minecraft:cooked_porkchop", 6);
        addPrice("minecraft:cooked_chicken", 5);
        addPrice("minecraft:cooked_mutton", 5);
        addPrice("minecraft:cooked_rabbit", 6);
        addPrice("minecraft:cooked_cod", 5);
        addPrice("minecraft:cooked_salmon", 5);

        // 原版简易菜肴
        addPrice("minecraft:bread", 3);
        addPrice("minecraft:baked_potato", 3);
        addPrice("minecraft:pumpkin_pie", 5);
        addPrice("minecraft:cookie", 2);
        addPrice("minecraft:cake", 8);
        addPrice("minecraft:mushroom_stew", 5);
        addPrice("minecraft:beetroot_soup", 5);
        addPrice("minecraft:rabbit_stew", 7);

        addPrice("kaleidoscope_cookery:suspicious_stir_fry", 20); // 谜之炒菜
        addPrice("kaleidoscope_cookery:slime_ball_meal", 20); // 黏液饭
        addPrice("kaleidoscope_cookery:fondant_pie", 20); // 翻糖派
        addPrice("kaleidoscope_cookery:dongpo_pork", 20); // 东坡肉
        addPrice("kaleidoscope_cookery:fondant_spider_eye", 20); // 翻糖蛛眼
        addPrice("kaleidoscope_cookery:chorus_fried_egg", 20); // 荷包紫颂烧
        addPrice("kaleidoscope_cookery:braised_fish", 20); // 红烧鱼
        addPrice("kaleidoscope_cookery:golden_salad", 20); // 黄金沙拉
        addPrice("kaleidoscope_cookery:spicy_chicken", 20); // 辣子鸡
        addPrice("kaleidoscope_cookery:yakitori", 20); // 烧鸟串
        addPrice("kaleidoscope_cookery:crystal_lamb_chop", 20); // 水晶羊排
        addPrice("kaleidoscope_cookery:nether_style_sashimi", 20); // 下界风味刺身
        addPrice("kaleidoscope_cookery:pan_seared_knight_steak", 20); // 香煎骑士牛排
        addPrice("kaleidoscope_cookery:stargazy_pie", 20); // 仰望星空派
        addPrice("kaleidoscope_cookery:sweet_and_sour_ender_pearls", 20); // 珍珠咕噜肉
        addPrice("kaleidoscope_cookery:blaze_lamb_chop", 20); // 烈焰羊排
        addPrice("kaleidoscope_cookery:frost_lamb_chop", 20); // 凛冬羊排
        addPrice("kaleidoscope_cookery:braised_pork_ribs", 20); // 红烧排骨
        addPrice("kaleidoscope_cookery:buddha_jumps_over_the_wall", 20); // 佛跳墙
        addPrice("kaleidoscope_cookery:brown_mushroom_pot_soup", 20); // 棕蘑菇瓦罐汤
        addPrice("kaleidoscope_cookery:candied_potato", 20); // 拔丝土豆
        addPrice("kaleidoscope_cookery:cold_cut_ham_slices", 20); // 冷切火腿片
        addPrice("kaleidoscope_cookery:cold_roasted_meat", 20); // 冷肉炙
        addPrice("kaleidoscope_cookery:crimson_fungus_pot_soup", 20); // 绯红菌瓦罐汤
        addPrice("kaleidoscope_cookery:dough_drop_soup", 20); // 疙瘩汤
        addPrice("kaleidoscope_cookery:four_joy_meatball_soup", 20); // 四喜丸子汤
        addPrice("kaleidoscope_cookery:fried_caterpillar", 20); // 油炸猪儿虫
        addPrice("kaleidoscope_cookery:fried_spring_roll", 20); // 炸春卷
        addPrice("kaleidoscope_cookery:fruit_platter", 20); // 水果拼盘
        addPrice("kaleidoscope_cookery:numbing_spicy_chicken", 20); // 椒麻鸡
        addPrice("kaleidoscope_cookery:oil_splashed_fish", 20); // 油泼鱼
        addPrice("kaleidoscope_cookery:red_mushroom_pot_soup", 20); // 红蘑菇瓦罐汤
        addPrice("kaleidoscope_cookery:spicy_blood_stew", 20); // 毛血旺
        addPrice("kaleidoscope_cookery:spicy_rabbit_head", 20); // 麻辣兔头
        addPrice("kaleidoscope_cookery:stuffed_tiger_skin_pepper", 20); // 虎皮青椒酿肉
        addPrice("kaleidoscope_cookery:warped_fungus_pot_soup", 20); // 诡异菌瓦罐汤
        addPrice("kaleidoscope_cookery:end_style_sashimi", 20); // 末地风味刺身
        addPrice("kaleidoscope_cookery:desert_style_sashimi", 20); // 沙漠风味刺身
        addPrice("kaleidoscope_cookery:tundra_style_sashimi", 20); // 苔原风味刺身
        addPrice("kaleidoscope_cookery:cold_style_sashimi", 20); // 寒带风味刺身
        addPrice("kaleidoscope_cookery:shengjian_mantou", 20); // 水煎包
        addPrice("kaleidoscope_cookery:fried_egg", 20); // 煎蛋
        addPrice("kaleidoscope_cookery:scramble_egg_with_tomatoes", 20); // 番茄炒蛋
        addPrice("kaleidoscope_cookery:scramble_egg_with_tomatoes_rice_bowl", 20); // 番茄炒蛋盖饭
        addPrice("kaleidoscope_cookery:stir_fried_beef_offal", 20); // 爆炒牛杂
        addPrice("kaleidoscope_cookery:stir_fried_beef_offal_rice_bowl", 20); // 爆炒牛杂盖饭
        addPrice("kaleidoscope_cookery:braised_beef", 20); // 红烧牛肉
        addPrice("kaleidoscope_cookery:braised_beef_rice_bowl", 20); // 红烧牛肉盖饭
        addPrice("kaleidoscope_cookery:stir_fried_pork_with_peppers", 20); // 青椒炒肉
        addPrice("kaleidoscope_cookery:stir_fried_pork_with_peppers_rice_bowl", 20); // 青椒炒肉盖饭
        addPrice("kaleidoscope_cookery:sweet_and_sour_pork", 20); // 糖醋里脊
        addPrice("kaleidoscope_cookery:sweet_and_sour_pork_rice_bowl", 20); // 糖醋里脊盖饭
        addPrice("kaleidoscope_cookery:country_style_mixed_vegetables", 20); // 田园杂蔬
        addPrice("kaleidoscope_cookery:fish_flavored_shredded_pork", 20); // 鱼香肉丝
        addPrice("kaleidoscope_cookery:fish_flavored_shredded_pork_rice_bowl", 20); // 鱼香肉丝盖饭
        addPrice("kaleidoscope_cookery:braised_fish_rice_bowl", 20); // 红烧鱼盖饭
        addPrice("kaleidoscope_cookery:spicy_chicken_rice_bowl", 20); // 辣子鸡盖饭
        addPrice("kaleidoscope_cookery:suspicious_stir_fry_rice_bowl", 20); // 谜之炒菜盖饭
        addPrice("kaleidoscope_cookery:egg_fried_rice", 20); // 蛋炒饭
        addPrice("kaleidoscope_cookery:delicious_egg_fried_rice", 20); // 美味蛋炒饭
        addPrice("kaleidoscope_cookery:pork_bone_soup", 20); // 大骨汤
        addPrice("kaleidoscope_cookery:seafood_miso_soup", 20); // 海鲜味噌汤
        addPrice("kaleidoscope_cookery:fearsome_thick_soup", 20); // 恐惧浓汤
        addPrice("kaleidoscope_cookery:lamb_and_radish_soup", 20); // 萝卜羊肉汤
        addPrice("kaleidoscope_cookery:braised_beef_with_potatoes", 20); // 土豆炖牛肉
        addPrice("kaleidoscope_cookery:wild_mushroom_rabbit_soup", 20); // 野菌兔肉汤
        addPrice("kaleidoscope_cookery:tomato_beef_brisket_soup", 20); // 番茄牛腩汤
        addPrice("kaleidoscope_cookery:pufferfish_soup", 20); // 河豚汤
        addPrice("kaleidoscope_cookery:borscht", 20); // 罗宋汤
        addPrice("kaleidoscope_cookery:beef_meatball_soup", 20); // 牛丸汤
        addPrice("kaleidoscope_cookery:chicken_and_mushroom_stew", 20); // 小鸡炖蘑菇
        addPrice("kaleidoscope_cookery:donkey_soup", 20); // 驴肉汤
        addPrice("kaleidoscope_cookery:cooked_lamb_chops", 20); // 熟羊排
        addPrice("kaleidoscope_cookery:cooked_cow_offal", 20); // 熟牛杂
        addPrice("kaleidoscope_cookery:cooked_pork_belly", 20); // 熟五花肉
        addPrice("kaleidoscope_cookery:cooked_cut_small_meats", 20); // 熟切制小肉
        addPrice("kaleidoscope_cookery:cooked_meatball", 20); // 熟丸子
        addPrice("kaleidoscope_cookery:cooked_donkey_meat", 20); // 熟驴肉
        addPrice("kaleidoscope_cookery:donkey_burger", 20); // 驴肉火烧
        addPrice("kaleidoscope_cookery:baozi", 20); // 包子
        addPrice("kaleidoscope_cookery:dumpling", 20); // 饺子
        addPrice("kaleidoscope_cookery:samsa", 20); // 烤包子
        addPrice("kaleidoscope_cookery:mantou", 20); // 馒头
        addPrice("kaleidoscope_cookery:meat_pie", 20); // 馅饼
        addPrice("kaleidoscope_cookery:beef_noodle", 20); // 牛肉面
        addPrice("kaleidoscope_cookery:hui_noodle", 20); // 羊肉烩面
        addPrice("kaleidoscope_cookery:udon_noodle", 20); // 乌冬面

        addPrice("kaleidoscope_tavern:wine", 10); // 葡萄酒
        addPrice("kaleidoscope_tavern:molotov", 10); // 莫洛托夫鸡尾酒
        addPrice("kaleidoscope_tavern:champagne", 10); // 香槟
        addPrice("kaleidoscope_tavern:vodka", 10); // 伏特加
        addPrice("kaleidoscope_tavern:brandy", 10); // 白兰地
        addPrice("kaleidoscope_tavern:carignan", 10); // 佳丽酿
        addPrice("kaleidoscope_tavern:sakura_wine", 10); // 樱花葡萄酒
        addPrice("kaleidoscope_tavern:plum_wine", 10); // 梅酒
        addPrice("kaleidoscope_tavern:whiskey", 10); // 威士忌
        addPrice("kaleidoscope_tavern:ice_wine", 10); // 冰葡萄酒
    }

    /**
     * 添加物品售价
     *
     * @param itemId 物品 ID (例如 "minecraft:apple")
     * @param price 售价
     */
    public static void addPrice(String itemId, int price) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl != null) {
            PRICES.put(rl, price);
        }
    }

    /**
     * 获取物品的单价
     *
     * @param itemStack 物品栈
     * @return 单价，如果未定义则返回 0
     */
    public static int getPrice(ItemStack itemStack) {
        if (itemStack.isEmpty()) return 0;
        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        int basePrice = PRICES.getOrDefault(rl, 0);
        if (basePrice <= 0) {
            return 0;
        }
        if (!(itemStack.getItem() instanceof DrinkBlockItem)) {
            return basePrice;
        }
        int brewLevel = Math.max(1, Math.min(7, BottleBlockItem.getBrewLevel(itemStack)));
        int price = basePrice;
        for (int level = 2; level <= brewLevel; level++) {
            price = (int) Math.floor(price * 1.4d);
        }
        return price;
    }
}
