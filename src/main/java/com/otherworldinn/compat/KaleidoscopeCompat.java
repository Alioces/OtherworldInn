package com.otherworldinn.compat;

import com.otherworldinn.OtherworldInn;
import net.neoforged.fml.ModList;

public class KaleidoscopeCompat {
    private static final String COOKERY_MOD_ID = "kaleidoscope_cookery";
    private static final String TAVERN_MOD_ID = "kaleidoscope_tavern";

    public static void init() {
        if (ModList.get().isLoaded(COOKERY_MOD_ID)) {
            initCookeryApi();
        }
        if (ModList.get().isLoaded(TAVERN_MOD_ID)) {
            initTavernApi();
        }
    }

    private static void initCookeryApi() {
        OtherworldInn.LOGGER.info("Kaleidoscope Cookery API compatibility enabled");
    }

    private static void initTavernApi() {
        OtherworldInn.LOGGER.info("Kaleidoscope Tavern API compatibility enabled");
    }
}
