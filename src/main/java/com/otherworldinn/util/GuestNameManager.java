package com.otherworldinn.util;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * 旅客姓名管理器
 *
 * <p>负责生成随机的旅客姓名。 姓名由名（First Name）和姓（Last Name）组成，支持本地化。
 */
public class GuestNameManager {

    public static final int FIRST_NAME_COUNT = 20;
    public static final int LAST_NAME_COUNT = 20;

    /**
     * 获取一个随机姓名组件
     *
     * <p>格式：Component.translatable("guest.name.format", FirstName, LastName)
     *
     * @param random 随机源
     * @return 姓名组件
     */
    public static Component getRandomName(RandomSource random) {
        // 随机选择名和姓的索引 (1 到 COUNT)
        int firstIndex = random.nextInt(FIRST_NAME_COUNT) + 1;
        int lastIndex = random.nextInt(LAST_NAME_COUNT) + 1;

        String firstNameKey = "guest.name.first." + firstIndex;
        String lastNameKey = "guest.name.last." + lastIndex;

        return Component.translatable(
                "guest.name.format",
                Component.translatable(firstNameKey),
                Component.translatable(lastNameKey));
    }
}
