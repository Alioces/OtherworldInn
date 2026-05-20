package com.otherworldinn.client;

import net.minecraft.client.Minecraft;

public final class ExpeditionTimerClientManager {

    private static long expeditionDeadlineTick = -1;

    private ExpeditionTimerClientManager() {}

    public static void syncRemaining(long remainingTicks) {
        if (remainingTicks <= 0) {
            expeditionDeadlineTick = -1;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            expeditionDeadlineTick = mc.level.getGameTime() + remainingTicks;
        } else {
            expeditionDeadlineTick = -1;
        }
    }

    public static void clearDeadline() {
        expeditionDeadlineTick = -1;
    }

    public static long getRemainingTicks(long currentTick) {
        if (expeditionDeadlineTick <= 0) return -1;
        return Math.max(0, expeditionDeadlineTick - currentTick);
    }
}
