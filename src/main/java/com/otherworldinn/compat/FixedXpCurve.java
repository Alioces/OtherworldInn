package com.otherworldinn.compat;

public final class FixedXpCurve {
    public static final int XP_PER_LEVEL = 62;

    private FixedXpCurve() {}

    public static int getXpForLevel(int level) {
        return Math.max(0, level) * XP_PER_LEVEL;
    }

    public static int getTotalXp(int level, float progress) {
        float clampedProgress = Math.max(0.0F, Math.min(1.0F, progress));
        return getXpForLevel(level) + Math.round(clampedProgress * XP_PER_LEVEL);
    }

    public static int getLevelForTotalXp(int totalXp) {
        return Math.max(0, totalXp) / XP_PER_LEVEL;
    }

    public static float getProgressForLevel(int totalXp, int level) {
        int base = getXpForLevel(level);
        float progress = (totalXp - base) / (float) XP_PER_LEVEL;
        return Math.max(0.0F, Math.min(1.0F, progress));
    }
}
