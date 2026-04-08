package com.otherworldinn.util;

import com.otherworldinn.OtherworldInn;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancementUtils {
    public static final ResourceLocation REPAIR_BOILER_ROOM =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "repair_boiler_room");
    public static final ResourceLocation REPAIR_GREENHOUSE =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "repair_greenhouse");
    public static final ResourceLocation INN_RATING_1 =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "inn_rating_1");
    public static final ResourceLocation INN_RATING_2 =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "inn_rating_2");
    public static final ResourceLocation INN_RATING_3 =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "inn_rating_3");
    public static final ResourceLocation INN_RATING_4 =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "inn_rating_4");
    public static final ResourceLocation INN_RATING_5 =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "inn_rating_5");
    public static final ResourceLocation SERVE_ONE_VIP =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "serve_one_vip");

    private AdvancementUtils() {}

    public static void award(ServerPlayer player, ResourceLocation advancementId) {
        if (player == null || advancementId == null) {
            return;
        }
        AdvancementHolder advancement = player.server.getAdvancements().get(advancementId);
        if (advancement == null) {
            return;
        }
        for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    public static void awardInnRatingProgress(ServerPlayer player, int rating) {
        if (rating >= 1) {
            award(player, INN_RATING_1);
        }
        if (rating >= 2) {
            award(player, INN_RATING_2);
        }
        if (rating >= 3) {
            award(player, INN_RATING_3);
        }
        if (rating >= 4) {
            award(player, INN_RATING_4);
        }
        if (rating >= 5) {
            award(player, INN_RATING_5);
        }
    }
}
