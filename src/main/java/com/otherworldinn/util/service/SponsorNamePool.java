package com.otherworldinn.util.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

public final class SponsorNamePool {
    public static final List<String> NAMES =
            List.of(
                    "MOQing",
                    "YeLuoYueShang",
                    "Ailisi415",
                    "PCLuige",
                    "SPDish",
                    "KnightKite",
                    "YOYIMIYASAMA",
                    "Yiyi",
                    "Mangomineralspr",
                    "AngryB",
                    "Xiwanzi",
                    "Hkszk",
                    "QvQi",
                    "GimmickyHare220",
                    "YoungAubthur",
                    "Kevinoer",
                    "AoRan",
                    "Lazypeople",
                    "RIHE122",
                    "NekotuanQAQ",
                    "HuLi_Tn",
                    "LinLei_Baruch",
                    "DanxiaoHanser",
                    "TheFlareStar",
                    "Biantwin",
                    "Striveturtle",
                    "XHeYa_3u3",
                    "Wuyu_OWO",
                    "chihuo_QWQ",
                    "B_eibao",
                    "Mr_eyes_5",
                    "JustKayina",
                    "IsoiaYUME",
                    "Cadivy",
                    "Yumicuibb",
                    "KeyxelDesu",
                    "Caoning",
                    "NomeSun",
                    "xiao_zhan",
                    "Shuo_Mo",
                    "yszx_",
                    "zzniania",
                    "Ms_Springfield",
                    "YAKUMODESU",
                    "cabll",
                    "Cillian_master");

    private SponsorNamePool() {}

    public static String getRandomName(RandomSource random) {
        return NAMES.get(random.nextInt(NAMES.size()));
    }

    public static String getRandomName(RandomSource random, @Nullable ServerLevel level) {
        if (level == null) {
            return getRandomName(random);
        }

        Set<String> onlineNames = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            String name = player.getGameProfile().getName();
            if (name != null && !name.isBlank()) {
                onlineNames.add(name.toLowerCase(Locale.ROOT));
            }
        }

        if (onlineNames.isEmpty()) {
            return getRandomName(random);
        }

        List<Integer> matchedIndexes = new ArrayList<>();
        List<Integer> unmatchedIndexes = new ArrayList<>();
        for (int i = 0; i < NAMES.size(); i++) {
            String sponsorName = NAMES.get(i);
            if (onlineNames.contains(sponsorName.toLowerCase(Locale.ROOT))) {
                matchedIndexes.add(i);
            } else {
                unmatchedIndexes.add(i);
            }
        }

        if (matchedIndexes.isEmpty()) {
            return getRandomName(random);
        }
        if (unmatchedIndexes.isEmpty()) {
            return NAMES.get(matchedIndexes.get(random.nextInt(matchedIndexes.size())));
        }

        if (random.nextFloat() < 0.5F) {
            return NAMES.get(matchedIndexes.get(random.nextInt(matchedIndexes.size())));
        }
        return NAMES.get(unmatchedIndexes.get(random.nextInt(unmatchedIndexes.size())));
    }
}
