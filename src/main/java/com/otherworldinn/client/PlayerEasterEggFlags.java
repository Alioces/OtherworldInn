package com.otherworldinn.client;

public final class PlayerEasterEggFlags {
    private static boolean maimaiAtlasEnabled;

    private PlayerEasterEggFlags() {}

    public static boolean isMaimaiAtlasEnabled() {
        return maimaiAtlasEnabled;
    }

    public static void setMaimaiAtlasEnabled(boolean enabled) {
        maimaiAtlasEnabled = enabled;
    }

    public static boolean toggleMaimaiAtlas() {
        maimaiAtlasEnabled = !maimaiAtlasEnabled;
        return maimaiAtlasEnabled;
    }
}
