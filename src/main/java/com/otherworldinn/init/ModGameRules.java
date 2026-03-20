package com.otherworldinn.init;

import net.minecraft.world.level.GameRules;

public final class ModGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> RULE_EXTERNAL_DIMENSION_SCHEDULED_RESET =
            GameRules.register(
                    "worldReset",
                    GameRules.Category.UPDATES,
                    GameRules.BooleanValue.create(true));

    private ModGameRules() {}

    public static void init() {}
}
